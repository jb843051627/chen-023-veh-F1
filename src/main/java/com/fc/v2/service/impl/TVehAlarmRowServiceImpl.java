package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TVehAlarmRowMapper;
import com.fc.v2.model.auto.TVehAlarmRow;
import com.fc.v2.service.ITVehAlarmRowService;

/**
 * 报警处置核收行 Service业务层处理（batch-process 形状：整批提交）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TVehAlarmRowServiceImpl implements ITVehAlarmRowService {

    private static final int MAX_ROWS = 500;
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;

    @javax.annotation.Resource
    private TVehAlarmRowMapper vehAlarmRowMapper;

    @Override
    public TVehAlarmRow selectTVehAlarmRowById(Long id) {
        return this.vehAlarmRowMapper.selectById(id);
    }

    @Override
    public int submitBatch(String batchNo, List<TVehAlarmRow> rows) {
        String no = rows.get(0).getBatchNo();
        java.util.List<TVehAlarmRow> errors = new java.util.ArrayList<TVehAlarmRow>();
        int seq = 0;
        for (TVehAlarmRow r : rows) {
            if (r.getItemCode() == null || r.getItemCode().trim().isEmpty()
                    || r.getQty() == null
                    || r.getQty().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                seq++;
                r.setRowNo(Integer.valueOf(seq));
                r.setBatchNo(no);
                r.setStatus(STATUS_FAIL);
                this.vehAlarmRowMapper.insert(r);
                errors.add(r);
            }
        }
        if (!errors.isEmpty()) {
            return 0;
        }
        int ok = 0;
        for (TVehAlarmRow r : rows) {
            r.setBatchNo(no);
            r.setStatus(STATUS_OK);
            this.vehAlarmRowMapper.insert(r);
            ok++;
        }
        return ok;
    }

    @Override
    public List<TVehAlarmRow> listErrors(String batchNo) {
        return this.vehAlarmRowMapper.selectList(new QueryWrapper<TVehAlarmRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_FAIL));
    }
}
