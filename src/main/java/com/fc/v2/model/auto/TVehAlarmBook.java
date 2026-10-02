package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 月度报警处置核收册头对象 t_veh_alarm_book
 *
 * @author fuce
 * @date 2026-09-28
 */
@TableName("t_veh_alarm_book")
@ApiModel(value = "TVehAlarmBook", description = "月度报警处置核收册头")
public class TVehAlarmBook implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 月度报警处置核收册码 */
    @TableField("book_no")
    @ApiModelProperty(value = "月度报警处置核收册码")
    private String bookNo;

    /** 来路 AGENCY机构自录 CITY市级转递（两条来路一道门） */
    @TableField("source_kind")
    @ApiModelProperty(value = "来路 AGENCY/CITY")
    private String sourceKind;

    /** 来路机构/地市名目 */
    @TableField("source_name")
    @ApiModelProperty(value = "来路机构/地市名目")
    private String sourceName;

    /** 所报月度(YYYY-MM) */
    @TableField("report_month")
    @ApiModelProperty(value = "所报月度")
    private String reportMonth;

    /** 进门所属报送年度（系统落，不让人填） */
    @TableField("report_year")
    @ApiModelProperty(value = "进门所属报送年度")
    private Integer reportYear;

    /** 册情形 0办理中 1核讫 2整册退回 3空册 4已撤销 */
    @TableField("status")
    @ApiModelProperty(value = "册情形 0办理中 1核讫 2整册退回 3空册 4已撤销")
    private Integer status;

    /** 核讫标记 0未发 1已发（空册永不发） */
    @TableField("nuclear_flag")
    @ApiModelProperty(value = "核讫标记")
    private Integer nuclearFlag;

    /** 册上原有行数（进门那一步点的） */
    @TableField("total_rows")
    @ApiModelProperty(value = "册上原有行数")
    private Integer totalRows;

    /** 落进库里行数（办结那一回点算的快照） */
    @TableField("stored_rows")
    @ApiModelProperty(value = "落进库里行数")
    private Integer storedRows;

    /** 原样送回行数（办结那一回点算的快照） */
    @TableField("returned_rows")
    @ApiModelProperty(value = "原样送回行数")
    private Integer returnedRows;

    /** 办结时点的逐行结果快照(JSON)，供事后对账报差行 */
    @TableField("row_snapshot")
    @ApiModelProperty(value = "逐行结果快照")
    private String rowSnapshot;

    /** 撤册标记 0在册 1已撤 */
    @TableField("void_flag")
    @ApiModelProperty(value = "撤册标记")
    private Integer voidFlag;

    /** 有效册码=book_no，撤册置NULL，配合唯一键防二遍 */
    @TableField("active_key")
    @ApiModelProperty(value = "有效册码唯一键")
    private String activeKey;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookNo() {
        return bookNo;
    }

    public void setBookNo(String bookNo) {
        this.bookNo = bookNo;
    }

    public String getSourceKind() {
        return sourceKind;
    }

    public void setSourceKind(String sourceKind) {
        this.sourceKind = sourceKind;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getReportMonth() {
        return reportMonth;
    }

    public void setReportMonth(String reportMonth) {
        this.reportMonth = reportMonth;
    }

    public Integer getReportYear() {
        return reportYear;
    }

    public void setReportYear(Integer reportYear) {
        this.reportYear = reportYear;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getNuclearFlag() {
        return nuclearFlag;
    }

    public void setNuclearFlag(Integer nuclearFlag) {
        this.nuclearFlag = nuclearFlag;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getStoredRows() {
        return storedRows;
    }

    public void setStoredRows(Integer storedRows) {
        this.storedRows = storedRows;
    }

    public Integer getReturnedRows() {
        return returnedRows;
    }

    public void setReturnedRows(Integer returnedRows) {
        this.returnedRows = returnedRows;
    }

    public String getRowSnapshot() {
        return rowSnapshot;
    }

    public void setRowSnapshot(String rowSnapshot) {
        this.rowSnapshot = rowSnapshot;
    }

    public Integer getVoidFlag() {
        return voidFlag;
    }

    public void setVoidFlag(Integer voidFlag) {
        this.voidFlag = voidFlag;
    }

    public String getActiveKey() {
        return activeKey;
    }

    public void setActiveKey(String activeKey) {
        this.activeKey = activeKey;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
