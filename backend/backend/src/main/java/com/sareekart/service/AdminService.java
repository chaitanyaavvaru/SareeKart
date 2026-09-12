package com.sareekart.service;

import com.sareekart.dto.response.AdminDashboardResponse;
import com.sareekart.dto.response.ProductResponse;

import java.util.List;

public interface AdminService {

    AdminDashboardResponse getDashboardStats();

    List<ProductResponse> getLowStockProducts(int threshold);
}
