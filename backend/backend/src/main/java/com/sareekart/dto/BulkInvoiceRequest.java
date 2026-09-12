package com.sareekart.dto;

import java.util.List;

public class BulkInvoiceRequest {
    private List<Long> orderIds;
    private String format; // PDF or EXCEL

    public List<Long> getOrderIds() {
        return orderIds;
    }
    public void setOrderIds(List<Long> orderIds) {
        this.orderIds = orderIds;
    }
    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }
}
