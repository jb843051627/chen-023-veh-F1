package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 一册进门形态：一册一次、一次性、整册导入，不接逐行零递。
 * 机构自录与市级转递是两条来路，走同一道门。
 *
 * @author fuce
 * @date 2026-09-28
 */
@ApiModel(value = "AlarmBookPayload", description = "月度报警处置核收册进门件")
public class AlarmBookPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "册码（必填）")
    private String bookNo;

    /** AGENCY 机构自录 / CITY 各市转递 */
    @ApiModelProperty(value = "来路 AGENCY机构自录 CITY市级转递")
    private String sourceKind;

    @ApiModelProperty(value = "来路机构/地市名目")
    private String sourceName;

    @ApiModelProperty(value = "所报月度 YYYY-MM（报送年度由系统按月度落定，不由人填）")
    private String reportMonth;

    @ApiModelProperty(value = "整册各行，次序即各市合成册时的月度文件行次序")
    private List<RowLine> rows;

    /**
     * 一行两个数，一块儿递上来：采集报警条数 + 仍未处置条数，缺任一个都不许谁替它补。
     * 认车只认车辆营运代号 itemCode；没有代号（哪怕递了车牌）一律按没对上办。
     */
    @ApiModel(value = "AlarmBookRowLine", description = "进门行")
    public static class RowLine implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "车辆营运代号（认车唯一凭据）")
        private String itemCode;

        @ApiModelProperty(value = "本行采集报警条数")
        private BigDecimal hoursNum;

        @ApiModelProperty(value = "本行仍未处置条数")
        private BigDecimal qty;

        public String getItemCode() {
            return itemCode;
        }

        public void setItemCode(String itemCode) {
            this.itemCode = itemCode;
        }

        public BigDecimal getHoursNum() {
            return hoursNum;
        }

        public void setHoursNum(BigDecimal hoursNum) {
            this.hoursNum = hoursNum;
        }

        public BigDecimal getQty() {
            return qty;
        }

        public void setQty(BigDecimal qty) {
            this.qty = qty;
        }
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

    public List<RowLine> getRows() {
        return rows;
    }

    public void setRows(List<RowLine> rows) {
        this.rows = rows;
    }
}
