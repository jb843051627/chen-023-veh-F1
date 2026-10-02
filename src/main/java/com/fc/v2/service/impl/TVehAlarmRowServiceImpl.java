package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TVehAlarmBookMapper;
import com.fc.v2.mapper.auto.TVehAlarmRowMapper;
import com.fc.v2.mapper.auto.TVehTruckMapper;
import com.fc.v2.model.auto.TVehAlarmBook;
import com.fc.v2.model.auto.TVehAlarmRow;
import com.fc.v2.model.auto.TVehTruck;
import com.fc.v2.model.custom.AlarmBookPayload;
import com.fc.v2.model.custom.AlarmBookReceipt;
import com.fc.v2.service.ITVehAlarmRowService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 报警处置核收行 Service：一道口子、整册进门。
 *
 * @author fuce
 * @date 2026-09-28
 */
@Service
public class TVehAlarmRowServiceImpl implements ITVehAlarmRowService {

    /** 处里给每册行数定的顶：进门头一步先数行，压过顶的整册原样退回。 */
    private static final int MAX_ROWS = 500;

    /** 行落地情形 */
    private static final int ROW_OPEN = 0;
    private static final int ROW_ACCEPT = 1;
    private static final int ROW_REJECT = 2;

    /** 进门落点：落进库里 / 原样送回——进门那一步定盘，后日勾核不动它 */
    private static final int DOOR_IN = 1;
    private static final int DOOR_BACK = 2;

    /** 册情形 */
    private static final int BOOK_DOING = 0;
    private static final int BOOK_NUCLEAR = 1;
    private static final int BOOK_REJECTED = 2;
    private static final int BOOK_EMPTY = 3;
    private static final int BOOK_VOID = 4;

    private static final String REASON_CODE = "车辆营运代号对不上名录：认车只认营运代号，拿车牌对、代号念不全的按没对上办";
    private static final String REASON_NUMBERS = "采集报警条数与仍未处置条数须一并递齐，缺任一个不许代补";
    private static final String REASON_NEGATIVE = "条数不得为负";
    private static final String REASON_OVER = "本行未处置条数高过采集条数，该行原路送回";
    private static final String REASON_OVERFLOW = "进门行数压过每册行顶（" + MAX_ROWS + "行），整册原样退回、库里一行不留";

    @Resource
    private TVehAlarmRowMapper vehAlarmRowMapper;

    @Resource
    private TVehAlarmBookMapper vehAlarmBookMapper;

    @Resource
    private TVehTruckMapper vehTruckMapper;

    @Override
    public TVehAlarmRow selectTVehAlarmRowById(Long id) {
        return this.vehAlarmRowMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlarmBookReceipt receiveBook(AlarmBookPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("进门件不许空");
        }
        String bookNo = payload.getBookNo() == null ? null : payload.getBookNo().trim();
        if (bookNo == null || bookNo.isEmpty()) {
            throw new IllegalArgumentException("册码不许空：册子只认这一道口子，没册码的不进门");
        }
        List<AlarmBookPayload.RowLine> rows = payload.getRows() == null
                ? new ArrayList<AlarmBookPayload.RowLine>() : payload.getRows();
        int total = rows.size();

        String sourceKind = payload.getSourceKind() == null ? "" : payload.getSourceKind().trim().toUpperCase();
        if (!"AGENCY".equals(sourceKind) && !"CITY".equals(sourceKind)) {
            throw new IllegalArgumentException("来路只认两条：AGENCY机构自录、CITY市级转递，两条来路走同一道门");
        }

        // 进门头一步先数行：压过行顶的整册原样退回，库里一行不留（册头留退回记录，不占有效册码）。
        if (total > MAX_ROWS) {
            TVehAlarmBook rejected = newHead(bookNo, payload);
            rejected.setStatus(BOOK_REJECTED);
            rejected.setNuclearFlag(0);
            rejected.setTotalRows(total);
            rejected.setStoredRows(0);
            rejected.setReturnedRows(0);
            rejected.setActiveKey(null);
            rejected.setRemark(REASON_OVERFLOW);
            this.vehAlarmBookMapper.insert(rejected);

            AlarmBookReceipt receipt = new AlarmBookReceipt(bookNo);
            receipt.setBookStatus(BOOK_REJECTED);
            receipt.setTotalRows(total);
            receipt.setStoredRows(0);
            receipt.setReturnedRows(0);
            receipt.setNuclear(false);
            receipt.setReason(REASON_OVERFLOW);
            return receipt;
        }

        // 同册码第二遍：先与库里已有行照个面。有效册码在册的，一行不加、一行不减。
        TVehAlarmBook active = findActiveBook(bookNo);
        if (active != null) {
            AlarmBookReceipt receipt = buildReceipt(active);
            receipt.setDuplicate(true);
            return receipt;
        }

        int reportYear = resolveReportYear(payload);

        // 进门落册头（有效册码占位；唯一索引再兜一道并发二遍）。
        TVehAlarmBook book = newHead(bookNo, payload);
        book.setReportYear(reportYear);
        book.setTotalRows(total);
        book.setVoidFlag(0);
        book.setActiveKey(bookNo);
        try {
            this.vehAlarmBookMapper.insert(book);
        } catch (DuplicateKeyException e) {
            // 并发下另一趟先落了同册码：仍只照面，不覆它。
            TVehAlarmBook winner = findActiveBook(bookNo);
            if (winner == null) {
                throw new IllegalStateException("同册码正由另一趟进门，请按二遍照面再取回执：" + bookNo);
            }
            AlarmBookReceipt receipt = buildReceipt(winner);
            receipt.setDuplicate(true);
            return receipt;
        }

        // 一行也没收着的册：回执写“空册”，不发核讫。
        if (total == 0) {
            book.setStatus(BOOK_EMPTY);
            book.setNuclearFlag(0);
            book.setStoredRows(0);
            book.setReturnedRows(0);
            book.setRowSnapshot("[]");
            updateBook(book);

            AlarmBookReceipt receipt = new AlarmBookReceipt(bookNo);
            receipt.setBookStatus(BOOK_EMPTY);
            return receipt;
        }

        // 行次不留给人填：按落库先后（即整册月度文件行次序）系统排定，排定不再挪。
        // 逐行核：退回的只退它那一行，缘故写在行上，同册别行不被牵动、整册不打回起点。
        int seq = 0;
        for (AlarmBookPayload.RowLine line : rows) {
            seq++;
            TVehAlarmRow row = new TVehAlarmRow();
            row.setBookId(book.getId());
            row.setBatchNo(bookNo);
            row.setRowNo(seq);
            row.setReportYear(reportYear);
            row.setArchived(0);
            row.setDelFlag(0);
            row.setUnitKind("条");

            String reason = inspect(line);
            if (reason == null) {
                row.setItemCode(line.getItemCode().trim());
                row.setHoursNum(line.getHoursNum());
                row.setQty(line.getQty());
                row.setStatus(ROW_OPEN);
                row.setDoorFlag(DOOR_IN);
            } else {
                // 送回的行也留一道底：行号、递来的代号/两数与缘故都在行上，事后翻得着。
                row.setItemCode(line == null || line.getItemCode() == null ? null : line.getItemCode().trim());
                row.setHoursNum(line == null ? null : line.getHoursNum());
                row.setQty(line == null ? null : line.getQty());
                row.setStatus(ROW_REJECT);
                row.setDoorFlag(DOOR_BACK);
                row.setRejectReason(reason);
            }
            this.vehAlarmRowMapper.insert(row);
        }

        // 一册办完：三数回库现点现取，不拿手里这份凑数。
        int stored = countRows(book.getId(), false);
        int returned = countRows(book.getId(), true);
        List<TVehAlarmRow> landed = listRows(book.getId(), null);
        book.setStoredRows(stored);
        book.setReturnedRows(returned);
        book.setRowSnapshot(toSnapshot(landed));
        book.setStatus(BOOK_NUCLEAR);
        book.setNuclearFlag(1);
        updateBook(book);

        return buildReceipt(book);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean voidBook(String bookNo, String reason) {
        TVehAlarmBook active = findActiveBook(bookNo);
        if (active == null) {
            return false;
        }
        // 撤册只腾空有效册码：旧册头与旧行一行不删，头一遍落的结果谁也覆不掉。
        // 注意 active_key 要显式 set 成 NULL——updateById 默认不更新空字段。
        this.vehAlarmBookMapper.update(null, new UpdateWrapper<TVehAlarmBook>()
                .set("active_key", null)
                .set("void_flag", 1)
                .set("status", BOOK_VOID)
                .set("update_time", new Date())
                .set(reason != null, "remark", reason)
                .eq("id", active.getId()));
        return true;
    }

    @Override
    public AlarmBookReceipt getReceipt(String bookNo) {
        TVehAlarmBook active = findActiveBook(bookNo);
        if (active == null) {
            return null;
        }
        return buildReceipt(active);
    }

    @Override
    public AlarmBookReceipt.Audit audit(String bookNo) {
        TVehAlarmBook active = findActiveBook(bookNo);
        AlarmBookReceipt.Audit audit = new AlarmBookReceipt.Audit();
        audit.setBookNo(bookNo);
        if (active == null) {
            audit.setBalanced(false);
            return audit;
        }

        // 现点现取
        int currentStored = countRows(active.getId(), false);
        int currentReturned = countRows(active.getId(), true);
        audit.setReceiptTotal(active.getTotalRows() == null ? 0 : active.getTotalRows());
        audit.setReceiptStored(active.getStoredRows() == null ? 0 : active.getStoredRows());
        audit.setReceiptReturned(active.getReturnedRows() == null ? 0 : active.getReturnedRows());
        audit.setCurrentStored(currentStored);
        audit.setCurrentReturned(currentReturned);

        // 逐行与办结那一回的快照照面（进门落定的 door_flag 永不挪，后日勾核只动 status），
        // 行没了或进门落点变了的当场报出行号。
        List<String> diffs = new ArrayList<>();
        JSONArray snapshot = parseSnapshot(active.getRowSnapshot());
        if (snapshot != null) {
            for (int i = 0; i < snapshot.size(); i++) {
                JSONObject item = snapshot.getJSONObject(i);
                Long rowId = item.getLong("id");
                Integer snapDoor = item.getInteger("doorFlag");
                TVehAlarmRow now = rowId == null ? null : this.vehAlarmRowMapper.selectById(rowId);
                if (now == null || !java.util.Objects.equals(snapDoor, now.getDoorFlag())
                        || !java.util.Objects.equals(item.getInteger("rowNo"), now.getRowNo())) {
                    diffs.add("第" + item.getIntValue("rowNo") + "行");
                }
            }
        }
        audit.setDiffRows(diffs);

        boolean countsAgree = currentStored == audit.getReceiptStored()
                && currentReturned == audit.getReceiptReturned()
                && audit.getReceiptTotal() == currentStored + currentReturned;
        audit.setBalanced(countsAgree && diffs.isEmpty());
        return audit;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int decideRow(Long id, int status, String reason) {
        if (status != ROW_ACCEPT && status != ROW_REJECT) {
            throw new IllegalArgumentException("行结果只许三选一：未勾、收下、退回");
        }
        // 只许从现行名单上的未勾行落结果，已落下的、送回的、挪往期的都不许在这动。
        TVehAlarmRow patch = new TVehAlarmRow();
        patch.setStatus(status);
        if (status == ROW_REJECT) {
            patch.setRejectReason(reason);
        }
        patch.setUpdateTime(new Date());
        return this.vehAlarmRowMapper.update(patch, new UpdateWrapper<TVehAlarmRow>()
                .eq("id", id)
                .eq("status", ROW_OPEN)
                .eq("door_flag", DOOR_IN)
                .eq("archived", 0)
                .eq("del_flag", 0));
    }

    @Override
    public List<TVehAlarmRow> listRows(Long bookId, Integer archived) {
        QueryWrapper<TVehAlarmRow> wrapper = new QueryWrapper<TVehAlarmRow>()
                .eq("book_id", bookId)
                .eq("del_flag", 0)
                .orderByAsc("row_no");
        if (archived != null) {
            wrapper.eq("archived", archived.intValue());
        }
        return this.vehAlarmRowMapper.selectList(wrapper);
    }

    @Override
    public List<TVehAlarmRow> listCurrentRows(String bookNo) {
        return listByBatchNo(bookNo, 0);
    }

    @Override
    public List<TVehAlarmRow> listArchivedRows(String bookNo) {
        return listByBatchNo(bookNo, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int archiveStaleRows(int currentReportYear) {
        // 连着两个报送年度结果还空着（未勾）的现行行挪往期；往期照旧翻得着，新册走不着这条路。
        TVehAlarmRow patch = new TVehAlarmRow();
        patch.setArchived(1);
        patch.setUpdateTime(new Date());
        return this.vehAlarmRowMapper.update(patch, new UpdateWrapper<TVehAlarmRow>()
                .eq("archived", 0)
                .eq("status", ROW_OPEN)
                .eq("del_flag", 0)
                .le("report_year", currentReportYear - 2));
    }

    // ------------------------------------------------------------------

    private List<TVehAlarmRow> listByBatchNo(String bookNo, int archived) {
        return this.vehAlarmRowMapper.selectList(new QueryWrapper<TVehAlarmRow>()
                .eq("batch_no", bookNo)
                .eq("archived", archived)
                .eq("del_flag", 0)
                .orderByAsc("book_id", "row_no"));
    }

    private TVehAlarmBook findActiveBook(String bookNo) {
        if (bookNo == null || bookNo.isEmpty()) {
            return null;
        }
        return this.vehAlarmBookMapper.selectOne(new QueryWrapper<TVehAlarmBook>()
                .eq("active_key", bookNo)
                .eq("del_flag", 0)
                .last("limit 1"));
    }

    private int countRows(Long bookId, boolean rejected) {
        // 进门落定后 door_flag 再不挪，三数才经得起后日现点；后日勾核只动 status。
        QueryWrapper<TVehAlarmRow> wrapper = new QueryWrapper<TVehAlarmRow>()
                .eq("book_id", bookId)
                .eq("del_flag", 0)
                .eq("door_flag", rejected ? DOOR_BACK : DOOR_IN);
        Integer cnt = this.vehAlarmRowMapper.selectCount(wrapper);
        return cnt == null ? 0 : cnt.intValue();
    }

    /**
     * 逐行照规矩验：代号对名录 → 两个数一并递齐 → 不得为负 → 未处置不得高过采集。
     * 任何一条不过，只退这一行，缘故写在行上。
     */
    private String inspect(AlarmBookPayload.RowLine line) {
        if (line == null) {
            return REASON_NUMBERS;
        }
        String code = line.getItemCode() == null ? null : line.getItemCode().trim();
        if (code == null || code.isEmpty()) {
            return REASON_CODE;
        }
        TVehTruck truck = this.vehTruckMapper.selectOne(new QueryWrapper<TVehTruck>()
                .eq("item_code", code)
                .eq("status", 0)
                .eq("del_flag", 0)
                .last("limit 1"));
        if (truck == null) {
            return REASON_CODE;
        }
        BigDecimal hours = line.getHoursNum();
        BigDecimal qty = line.getQty();
        if (hours == null || qty == null) {
            return REASON_NUMBERS;
        }
        if (hours.signum() < 0 || qty.signum() < 0) {
            return REASON_NEGATIVE;
        }
        if (qty.compareTo(hours) > 0) {
            return REASON_OVER;
        }
        return null;
    }

    private AlarmBookReceipt buildReceipt(TVehAlarmBook book) {
        // 三个数各自回库现点现取：原有取进门点的册头底数，落库/送回当场再点一回。
        AlarmBookReceipt receipt = new AlarmBookReceipt(book.getBookNo());
        receipt.setBookStatus(book.getStatus());
        receipt.setTotalRows(book.getTotalRows() == null ? 0 : book.getTotalRows().intValue());
        if (book.getStatus() != null && book.getStatus() == BOOK_REJECTED) {
            receipt.setStoredRows(0);
            receipt.setReturnedRows(0);
            receipt.setNuclear(false);
            receipt.setReason(book.getRemark());
            return receipt;
        }
        receipt.setStoredRows(countRows(book.getId(), false));
        receipt.setReturnedRows(countRows(book.getId(), true));
        receipt.setNuclear(book.getNuclearFlag() != null && book.getNuclearFlag() == 1);
        return receipt;
    }

    private TVehAlarmBook newHead(String bookNo, AlarmBookPayload payload) {
        TVehAlarmBook book = new TVehAlarmBook();
        book.setBookNo(bookNo);
        String source = payload.getSourceKind() == null || payload.getSourceKind().trim().isEmpty()
                ? "AGENCY" : payload.getSourceKind().trim().toUpperCase();
        book.setSourceKind(source);
        book.setSourceName(payload.getSourceName());
        book.setReportMonth(payload.getReportMonth());
        book.setDelFlag(0);
        book.setVoidFlag(0);
        book.setNuclearFlag(0);
        book.setStoredRows(0);
        book.setReturnedRows(0);
        book.setTotalRows(0);
        return book;
    }

    private int resolveReportYear(AlarmBookPayload payload) {
        String month = payload.getReportMonth();
        if (month != null && month.length() >= 4) {
            try {
                return Integer.parseInt(month.substring(0, 4));
            } catch (NumberFormatException ignore) {
                // 月度念得不全，年度由系统当下补落（不影响行上的两个数）。
            }
        }
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    private void updateBook(TVehAlarmBook book) {
        book.setUpdateTime(new Date());
        this.vehAlarmBookMapper.updateById(book);
    }

    private String toSnapshot(List<TVehAlarmRow> rows) {
        JSONArray array = new JSONArray();
        for (TVehAlarmRow r : rows) {
            JSONObject item = new JSONObject();
            item.put("id", r.getId());
            item.put("rowNo", r.getRowNo());
            item.put("doorFlag", r.getDoorFlag());
            array.add(item);
        }
        return array.toJSONString();
    }

    private JSONArray parseSnapshot(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseArray(json);
        } catch (Exception e) {
            return null;
        }
    }
}
