package com.haruhi.botServer.utils.excel;


public class BaseImportBody {
    // 行下标
    private Integer rowIndex;

    public Integer getRowIndex() {
        return rowIndex == null ? 0 : rowIndex;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
    }
}
