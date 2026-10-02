package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 一册办完的回执：只填三个数——册上原有几行、落进库里几行、原样送回几行。
 * 送回 + 落库必须接得平原有的行数。三个数均回库现点现取，不拿屏上当下那份凑数。
 * 行数为零的册只写“空册”，不发核讫。
 *
 * @author fuce
 * @date 2026-09-28
 */
@ApiModel(value = "AlarmBookReceipt", description = "核收回执")
public class AlarmBookReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "册码")
    private String bookNo;

    /** 册情形 0办理中 1核讫 2整册退回 3空册 4已撤销 */
    @ApiModelProperty(value = "册情形 0办理中 1核讫 2整册退回 3空册 4已撤销")
    private Integer bookStatus;

    @ApiModelProperty(value = "册上原有行数（进门点的）")
    private int totalRows;

    @ApiModelProperty(value = "落进库里行数（回库现点现取）")
    private int storedRows;

    @ApiModelProperty(value = "原样送回行数（回库现点现取）")
    private int returnedRows;

    @ApiModelProperty(value = "是否核讫（有行数且办完才核讫，空册永不核讫）")
    private boolean nuclear;

    /** 整册退回时（超行顶）库里一行不留，缘故写在这里 */
    @ApiModelProperty(value = "整册退回缘故")
    private String reason;

    /** 二次递交时与已有行照面，一行不加、一行不减 */
    @ApiModelProperty(value = "是否二遍照面（未作任何改动）")
    private boolean duplicate;

    public AlarmBookReceipt() {
    }

    public AlarmBookReceipt(String bookNo) {
        this.bookNo = bookNo;
    }

    /**
     * 三数接得平：送回 + 落库 = 原有。整册退回（超顶）时原有 > 0 而落库 0，
     * 属于整册未进门，另由 reason 交代，不走行级接平口径。
     */
    public boolean isBalanced() {
        if (bookStatus != null && bookStatus == 2) {
            return storedRows == 0 && returnedRows == 0;
        }
        return totalRows == storedRows + returnedRows;
    }

    public String getBookNo() {
        return bookNo;
    }

    public void setBookNo(String bookNo) {
        this.bookNo = bookNo;
    }

    public Integer getBookStatus() {
        return bookStatus;
    }

    public void setBookStatus(Integer bookStatus) {
        this.bookStatus = bookStatus;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getStoredRows() {
        return storedRows;
    }

    public void setStoredRows(int storedRows) {
        this.storedRows = storedRows;
    }

    public int getReturnedRows() {
        return returnedRows;
    }

    public void setReturnedRows(int returnedRows) {
        this.returnedRows = returnedRows;
    }

    public boolean isNuclear() {
        return nuclear;
    }

    public void setNuclear(boolean nuclear) {
        this.nuclear = nuclear;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    /**
     * 事后挑一行回头看时的对账结果：行上结果与回执数必须出自同一回点算，
     * 差开的行当场报出来，一句“大差不差”抵不了一次交代。
     */
    @ApiModel(value = "AlarmBookAudit", description = "回执对账结果")
    public static class Audit implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "册码")
        private String bookNo;

        @ApiModelProperty(value = "是否接平")
        private boolean balanced;

        @ApiModelProperty(value = "回执：原有")
        private int receiptTotal;

        @ApiModelProperty(value = "回执：落库")
        private int receiptStored;

        @ApiModelProperty(value = "回执：送回")
        private int receiptReturned;

        @ApiModelProperty(value = "现点：落库")
        private int currentStored;

        @ApiModelProperty(value = "现点：送回")
        private int currentReturned;

        @ApiModelProperty(value = "与办结快照对不上的行")
        private List<String> diffRows = new ArrayList<>();

        public String getBookNo() {
            return bookNo;
        }

        public void setBookNo(String bookNo) {
            this.bookNo = bookNo;
        }

        public boolean isBalanced() {
            return balanced;
        }

        public void setBalanced(boolean balanced) {
            this.balanced = balanced;
        }

        public int getReceiptTotal() {
            return receiptTotal;
        }

        public void setReceiptTotal(int receiptTotal) {
            this.receiptTotal = receiptTotal;
        }

        public int getReceiptStored() {
            return receiptStored;
        }

        public void setReceiptStored(int receiptStored) {
            this.receiptStored = receiptStored;
        }

        public int getReceiptReturned() {
            return receiptReturned;
        }

        public void setReceiptReturned(int receiptReturned) {
            this.receiptReturned = receiptReturned;
        }

        public int getCurrentStored() {
            return currentStored;
        }

        public void setCurrentStored(int currentStored) {
            this.currentStored = currentStored;
        }

        public int getCurrentReturned() {
            return currentReturned;
        }

        public void setCurrentReturned(int currentReturned) {
            this.currentReturned = currentReturned;
        }

        public List<String> getDiffRows() {
            return diffRows;
        }

        public void setDiffRows(List<String> diffRows) {
            this.diffRows = diffRows;
        }
    }
}
