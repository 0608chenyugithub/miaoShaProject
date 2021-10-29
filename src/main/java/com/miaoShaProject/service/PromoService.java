package com.miaoShaProject.service;

import com.miaoShaProject.service.model.PromoModel;

public interface PromoService {
    public PromoModel getPromoByItemId(Integer itemId);

    //活动发布
    void publishPromo(Integer promoId);

    //生成秒杀用的令牌
    String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId);
}
