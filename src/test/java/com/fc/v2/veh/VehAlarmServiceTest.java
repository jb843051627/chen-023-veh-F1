package com.fc.v2.veh;

import com.fc.v2.model.auto.TVehAlarmBook;
import com.fc.v2.model.auto.TVehAlarmRow;
import com.fc.v2.model.custom.AlarmBookPayload;
import com.fc.v2.model.custom.AlarmBookReceipt;
import com.fc.v2.service.ITVehAlarmRowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 一道口子核收门规全量校验（H2 + 真实 MyBatis-Plus + 真实事务）。
 */
@SpringJUnitConfig(classes = {VehAlarmTestConfig.class, VehAlarmServiceTest.AppConfig.class})
@Transactional
class VehAlarmServiceTest {

    static final String C1 = "YY3201000001";
    static final String C2 = "YY3201000002";
    static final String C3 = "YY3201000003";
    /** 名录里已注销(status=1)的代号 */
    static final String C_CANCELLED = "YY3201000004";

    @Autowired
    private ITVehAlarmRowService service;

    @Autowired
    private JdbcTemplate jdbc;

    @org.springframework.context.annotation.Configuration
    static class AppConfig {
        @org.springframework.context.annotation.Bean
        JdbcTemplate jdbcTemplate(javax.sql.DataSource ds) {
            return new JdbcTemplate(ds);
        }

        /** 只起核收这一个 service，不拉起 shiro/redis 那一摊 */
        @org.springframework.context.annotation.Bean
        com.fc.v2.service.impl.TVehAlarmRowServiceImpl vehAlarmRowService(
                com.fc.v2.mapper.auto.TVehAlarmRowMapper rowMapper,
                com.fc.v2.mapper.auto.TVehAlarmBookMapper bookMapper,
                com.fc.v2.mapper.auto.TVehTruckMapper truckMapper) {
            com.fc.v2.service.impl.TVehAlarmRowServiceImpl impl =
                    new com.fc.v2.service.impl.TVehAlarmRowServiceImpl();
            org.springframework.test.util.ReflectionTestUtils.setField(impl, "vehAlarmRowMapper", rowMapper);
            org.springframework.test.util.ReflectionTestUtils.setField(impl, "vehAlarmBookMapper", bookMapper);
            org.springframework.test.util.ReflectionTestUtils.setField(impl, "vehTruckMapper", truckMapper);
            return impl;
        }
    }

    private AlarmBookPayload book(String no, String source, String month, AlarmBookPayload.RowLine... lines) {
        AlarmBookPayload p = new AlarmBookPayload();
        p.setBookNo(no);
        p.setSourceKind(source);
        p.setReportMonth(month);
        List<AlarmBookPayload.RowLine> rows = new ArrayList<>();
        for (AlarmBookPayload.RowLine l : lines) {
            rows.add(l);
        }
        p.setRows(rows);
        return p;
    }

    private AlarmBookPayload.RowLine line(String code, String hours, String qty) {
        AlarmBookPayload.RowLine l = new AlarmBookPayload.RowLine();
        l.setItemCode(code);
        l.setHoursNum(hours == null ? null : new BigDecimal(hours));
        l.setQty(qty == null ? null : new BigDecimal(qty));
        return l;
    }

    private int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    // 1. 整册合法进门：三数接平、发核讫、行落库为未勾
    @Test
    @DisplayName("合法整册：全部收下落库，回执三数接平且核讫")
    void validBook_storedBalancedAndNuclear() {
        AlarmBookReceipt r = service.receiveBook(book("B-001", "AGENCY", "2026-09",
                line(C1, "10", "2"), line(C2, "8", "0"), line(C3, "5", "5")));

        assertEquals(1, r.getBookStatus().intValue());
        assertEquals(3, r.getTotalRows());
        assertEquals(3, r.getStoredRows());
        assertEquals(0, r.getReturnedRows());
        assertTrue(r.isBalanced());
        assertTrue(r.isNuclear());
        assertFalse(r.isDuplicate());

        List<TVehAlarmRow> rows = service.listCurrentRows("B-001");
        assertEquals(3, rows.size());
        for (TVehAlarmRow row : rows) {
            assertEquals(0, row.getStatus().intValue(), "进门合法行先挂未勾");
            assertEquals(1, row.getDoorFlag().intValue());
            assertEquals(0, row.getArchived().intValue());
            assertEquals("条", row.getUnitKind());
        }
    }

    // 2. 行次系统按落库先后排，进门件根本没有行次可填
    @Test
    @DisplayName("行次由系统按进门次序排定，与纸面册对得上号")
    void rowNo_assignedByLandingOrder() {
        service.receiveBook(book("B-002", "CITY", "2026-09",
                line(C3, "1", "1"), line(C1, "2", "1"), line(C2, "3", "0")));

        List<TVehAlarmRow> rows = service.listCurrentRows("B-002");
        assertEquals(1, rows.get(0).getRowNo().intValue());
        assertEquals(C3, rows.get(0).getItemCode());
        assertEquals(2, rows.get(1).getRowNo().intValue());
        assertEquals(C1, rows.get(1).getItemCode());
        assertEquals(3, rows.get(2).getRowNo().intValue());
    }

    // 3. 缺一个数：只退该行，别行不牵动
    @Test
    @DisplayName("两个数缺一：该行原路退回写缘故，同册别行照常落库")
    void missingOneNumber_singleRowReturnedOthersUnaffected() {
        AlarmBookReceipt r = service.receiveBook(book("B-003", "AGENCY", "2026-09",
                line(C1, "10", "2"),
                line(C2, "8", null),
                line(C3, "5", "1")));

        assertEquals(3, r.getTotalRows());
        assertEquals(2, r.getStoredRows());
        assertEquals(1, r.getReturnedRows());
        assertTrue(r.isBalanced());

        List<TVehAlarmRow> all = service.listRows(headIdOf("B-003"), null);
        TVehAlarmRow bad = all.get(1);
        assertEquals(2, bad.getStatus().intValue());
        assertEquals(2, bad.getDoorFlag().intValue());
        assertNotNull(bad.getRejectReason());
        assertTrue(bad.getRejectReason().contains("一并递齐"));
        // 退回次日再翻，原行次不挪
        assertEquals(2, bad.getRowNo().intValue());
        assertEquals(0, all.get(0).getStatus().intValue());
        assertEquals(0, all.get(2).getStatus().intValue());
    }

    // 4. 认车只认代号：名录没有、已注销的，按没对上办
    @Test
    @DisplayName("代号对不上/已注销：退回该行，拿车牌也救不了")
    void codeMismatchOrCancelled_rejected() {
        AlarmBookReceipt r = service.receiveBook(book("B-004", "AGENCY", "2026-09",
                line("宁A99999", "3", "1"),
                line(C_CANCELLED, "3", "1"),
                line(null, "3", "1"),
                line("  ", "3", "1"),
                line(C1, "3", "1")));

        assertEquals(1, r.getStoredRows());
        assertEquals(4, r.getReturnedRows());
        assertTrue(r.isBalanced());
        List<TVehAlarmRow> all = service.listRows(headIdOf("B-004"), null);
        for (int i = 0; i < 4; i++) {
            assertEquals(2, all.get(i).getDoorFlag().intValue());
            assertTrue(all.get(i).getRejectReason().contains("营运代号"));
        }
    }

    // 5. 未处置高过采集：只退这一行，不把整册打回起点
    @Test
    @DisplayName("未处置>采集：该行退回且缘故在行上，册仍核讫，别行不受牵动")
    void qtyOverHours_rowOnlyReturned_bookStillNuclear() {
        AlarmBookReceipt r = service.receiveBook(book("B-005", "AGENCY", "2026-09",
                line(C1, "3", "10"),
                line(C2, "4", "4")));

        assertTrue(r.isNuclear(), "有行进门且办完即核讫，不因一行退回而扣册");
        assertEquals(1, r.getStoredRows());
        assertEquals(1, r.getReturnedRows());
        TVehAlarmRow over = service.listRows(headIdOf("B-005"), null).get(0);
        assertEquals(2, over.getStatus().intValue());
        assertTrue(over.getRejectReason().contains("高过采集条数"));
    }

    // 6. 行顶：进门先数行，压过顶整册原样退回，库里一行不留
    @Test
    @DisplayName("行数压过行顶：整册原样退回、一行不留，缘故在册上")
    void overCap_wholeBookRejected_nothingStored() {
        AlarmBookPayload.RowLine[] many = new AlarmBookPayload.RowLine[501];
        for (int i = 0; i < many.length; i++) {
            many[i] = line(C1, "1", "0");
        }
        AlarmBookReceipt r = service.receiveBook(book("B-006", "AGENCY", "2026-09", many));

        assertEquals(2, r.getBookStatus().intValue());
        assertEquals(501, r.getTotalRows());
        assertEquals(0, r.getStoredRows());
        assertFalse(r.isNuclear());
        assertNotNull(r.getReason());
        assertEquals(0, count("SELECT COUNT(*) FROM t_veh_alarm_row"));
        assertEquals(0, count("SELECT COUNT(*) FROM t_veh_alarm_book WHERE active_key='B-006'"));

        // 整册退回后册码不占口子：改合规矩可重新进门，不算二遍
        AlarmBookReceipt again = service.receiveBook(book("B-006", "AGENCY", "2026-09",
                line(C1, "1", "0")));
        assertFalse(again.isDuplicate());
        assertEquals(1, again.getStoredRows());
    }

    // 7. 空册：回执“空册”，不发核讫
    @Test
    @DisplayName("一行没收着：写空册，核讫那一说用不上")
    void emptyBook_receiptEmptyNoNuclear() {
        AlarmBookReceipt r = service.receiveBook(book("B-007", "CITY", "2026-09"));
        assertEquals(3, r.getBookStatus().intValue());
        assertEquals(0, r.getTotalRows());
        assertEquals(0, r.getStoredRows());
        assertEquals(0, r.getReturnedRows());
        assertTrue(r.isBalanced());
        assertFalse(r.isNuclear());
        assertEquals(1, count("SELECT COUNT(*) FROM t_veh_alarm_book WHERE book_no='B-007' AND nuclear_flag=0"));
    }

    // 8. 同册码第二遍：只照面，一行不加不减，头一遍结果覆不掉
    @Test
    @DisplayName("同册码二遍递交：与库存照面，不增不减不改")
    void duplicateSubmission_noChange() {
        service.receiveBook(book("B-008", "AGENCY", "2026-09",
                line(C1, "5", "1"), line(C2, "5", "9")));
        assertEquals(2, count("SELECT COUNT(*) FROM t_veh_alarm_row"));

        // 二遍带着完全不同的内容来也不认
        AlarmBookReceipt second = service.receiveBook(book("B-008", "CITY", "2026-09",
                line(C3, "1", "0"), line(C3, "1", "0"), line(C3, "1", "0")));

        assertTrue(second.isDuplicate());
        assertEquals(1, second.getStoredRows());
        assertEquals(1, second.getReturnedRows());
        assertEquals(2, count("SELECT COUNT(*) FROM t_veh_alarm_row"));
        assertEquals(1, count("SELECT COUNT(*) FROM t_veh_alarm_book WHERE book_no='B-008'"));
        // 二遍来路也被同一道门挡住，不会另立册头
        assertEquals("AGENCY",
                jdbc.queryForObject("SELECT source_kind FROM t_veh_alarm_book WHERE active_key='B-008'", String.class));
    }

    // 9. 要改先撤册：撤完可重递，旧行不删、头一遍结果不覆
    @Test
    @DisplayName("撤册后重递：新册头新行落库，旧册旧行原样留着")
    void voidThenResubmit_oldKeptNewLands() {
        service.receiveBook(book("B-009", "AGENCY", "2026-09", line(C1, "5", "5")));
        Long oldHead = headIdOf("B-009");
        assertTrue(service.voidBook("B-009", "机构自报错数"));
        assertFalse(service.voidBook("B-009", "再撤一回"));

        AlarmBookReceipt r = service.receiveBook(book("B-009", "AGENCY", "2026-09",
                line(C1, "5", "1"), line(C2, "3", "0")));

        assertFalse(r.isDuplicate());
        assertEquals(2, r.getStoredRows());
        assertEquals(2, count("SELECT COUNT(*) FROM t_veh_alarm_book WHERE book_no='B-009'"));
        assertEquals(1, count("SELECT COUNT(*) FROM t_veh_alarm_row WHERE book_id=?", oldHead));
        assertEquals(1, count("SELECT COUNT(*) FROM t_veh_alarm_book WHERE active_key='B-009' AND void_flag=0"));
        Long newHead = headIdOf("B-009");
        assertTrue(!newHead.equals(oldHead));
    }

    // 10. 同车两册各占一行，互不牵扯
    @Test
    @DisplayName("同一辆车两册各占一行：收下一行不带着另一行")
    void sameVehicleTwoBooks_rowsIndependent() {
        service.receiveBook(book("B-010A", "AGENCY", "2026-09", line(C1, "5", "1")));
        service.receiveBook(book("B-010B", "CITY", "2026-09", line(C1, "9", "9")));

        TVehAlarmRow a = service.listCurrentRows("B-010A").get(0);
        TVehAlarmRow b = service.listCurrentRows("B-010B").get(0);
        assertEquals(1, service.decideRow(a.getId(), 1, null));

        assertEquals(1, service.selectTVehAlarmRowById(a.getId()).getStatus().intValue(), "A 册行已收下");
        assertEquals(0, service.selectTVehAlarmRowById(b.getId()).getStatus().intValue(), "B 册行仍未勾");
    }

    // 11. 行结果三选一：已落不许覆，非法取值不许
    @Test
    @DisplayName("勾核只许未勾→收下/退回，落下后覆不动")
    void decideRow_rules() {
        service.receiveBook(book("B-011", "AGENCY", "2026-09", line(C1, "5", "1")));
        Long id = service.listCurrentRows("B-011").get(0).getId();

        assertEquals(1, service.decideRow(id, 1, null));
        assertEquals(0, service.decideRow(id, 2, "改退"), "已落下结果不许覆");
        assertEquals(1, service.selectTVehAlarmRowById(id).getStatus().intValue());
        assertThrows(IllegalArgumentException.class, () -> service.decideRow(id, 9, null));
    }

    // 12. 回执三数回库现点现取：勾核动 status，进门落点不动，三数仍与办结同回一致
    @Test
    @DisplayName("回执现点现取：勾核后三数仍接平、与核讫那回同一口径")
    void receipt_recountsFromDb() {
        service.receiveBook(book("B-012", "AGENCY", "2026-09",
                line(C1, "5", "1"), line(C2, "5", "9"), line(C3, "5", "0")));
        List<TVehAlarmRow> rows = service.listRows(headIdOf("B-012"), null);
        service.decideRow(rows.get(0).getId(), 1, null);

        AlarmBookReceipt later = service.getReceipt("B-012");
        assertEquals(3, later.getTotalRows());
        assertEquals(2, later.getStoredRows());
        assertEquals(1, later.getReturnedRows());
        assertTrue(later.isBalanced());
    }

    // 13. 事后对账：行没了当场报差行
    @Test
    @DisplayName("事后挑行回头看：与办结快照差开的行当场报出")
    void audit_reportsDiffRows() {
        service.receiveBook(book("B-013", "AGENCY", "2026-09",
                line(C1, "5", "1"), line(C2, "5", "1"), line(C3, "5", "9")));

        AlarmBookReceipt.Audit ok = service.audit("B-013");
        assertTrue(ok.isBalanced());
        assertTrue(ok.getDiffRows().isEmpty());

        // 模拟有人事后动了库里的行
        List<TVehAlarmRow> rows = service.listRows(headIdOf("B-013"), null);
        jdbc.update("DELETE FROM t_veh_alarm_row WHERE id=?", rows.get(1).getId());

        AlarmBookReceipt.Audit bad = service.audit("B-013");
        assertFalse(bad.isBalanced());
        assertTrue(bad.getDiffRows().contains("第2行"));
    }

    // 14. 连着两个报送年度还没着落：挪往期，往期一条不少
    @Test
    @DisplayName("跨两个报送年度仍未勾：挪往期，现行不摆、往期照旧翻得着")
    void archiveStaleRows() {
        service.receiveBook(book("B-OLD", "AGENCY", "2024-03",
                line(C1, "5", "1"), line(C2, "5", "9")));
        service.receiveBook(book("B-NEW", "AGENCY", "2026-09", line(C3, "5", "0")));

        int moved = service.archiveStaleRows(2026);
        assertEquals(1, moved, "只有旧册那一行未勾行够两个年度（退回行不算未勾）");

        List<TVehAlarmRow> stillCurrent = service.listCurrentRows("B-OLD");
        assertEquals(1, stillCurrent.size(), "旧册现行名单只剩进门被退回那一行");
        assertEquals(0, stillCurrent.stream().filter(r -> r.getStatus() == 0).count());
        List<TVehAlarmRow> archived = service.listArchivedRows("B-OLD");
        assertEquals(1, archived.size());
        assertEquals(C1, archived.get(0).getItemCode());

        // 往期一条不少：两层合计仍是原来两行，行次也没挪
        List<TVehAlarmRow> all = service.listRows(headIdOf("B-OLD"), null);
        assertEquals(2, all.size());
        assertEquals(1, all.get(0).getRowNo().intValue());
        // 新册的行走不着这条路
        assertEquals(1, service.listCurrentRows("B-NEW").size());
    }

    // 15. 进门门规兜底
    @Test
    @DisplayName("没册码/没进门件不进门")
    void noBookNo_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.receiveBook(book("  ", "AGENCY", "2026-09", line(C1, "1", "0"))));
        assertThrows(IllegalArgumentException.class, () -> service.receiveBook(null));
    }

    private Long headIdOf(String bookNo) {
        return jdbc.queryForObject(
                "SELECT id FROM t_veh_alarm_book WHERE active_key=? LIMIT 1", Long.class, bookNo);
    }
}
