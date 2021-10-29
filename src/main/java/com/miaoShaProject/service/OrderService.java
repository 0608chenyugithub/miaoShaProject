package com.miaoShaProject.service;

import com.miaoShaProject.error.BusinessException;
import com.miaoShaProject.service.model.OrderModel;

public interface OrderService {
    OrderModel createOrder(Integer user_id, Integer item_id, Integer promoId, Integer amount, String stockLogId) throws BusinessException;
}
