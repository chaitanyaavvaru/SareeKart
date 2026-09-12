package com.sareekart.dto;

import java.util.List;

public class InvoiceListResponse {
    private List<Long> orderIds;

    public InvoiceListResponse() {}

    public InvoiceListResponse(List<Long> orderIds) {
        this.orderIds = orderIds;
    }

    public List<Long> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<Long> orderIds) {
        this.orderIds = orderIds;
    }
}
