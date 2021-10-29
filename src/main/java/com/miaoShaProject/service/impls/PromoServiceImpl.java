package com.miaoShaProject.service.impls;

import com.miaoShaProject.dao.PromoDOMapper;
import com.miaoShaProject.dataObject.PromoDO;
import com.miaoShaProject.error.BusinessException;
import com.miaoShaProject.error.EnumBusinessError;
import com.miaoShaProject.service.ItemService;
import com.miaoShaProject.service.PromoService;
import com.miaoShaProject.service.UserService;
import com.miaoShaProject.service.model.ItemModel;
import com.miaoShaProject.service.model.PromoModel;
import com.miaoShaProject.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class PromoServiceImpl implements PromoService {
    @Autowired
    private PromoDOMapper promoDOMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);

        //dataObject->model
        PromoModel promoModel = convertFromDataObject(promoDO);
        if (promoDO == null) {
            return null;
        }
        DateTime now = new DateTime();
        if (promoModel.getStartDate().isAfterNow()) {
            promoModel.setStatus(1);
        }
        else if(promoModel.getStartDate().isBeforeNow()) {
            promoModel.setStatus(3);
        }
        else {
            promoModel.setStatus(2);
        }

        return promoModel;
    }

    private PromoModel convertFromDataObject(PromoDO promoDO) {
        if (promoDO == null) {
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO, promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));

        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (promoDO.getItemId() == null || promoDO.getItemId() == 0) {
            return;
        }
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());
        redisTemplate.opsForValue().set("promo_item_stock_"+itemModel.getId(), itemModel.getStock());

        //将大闸数字设置到redis内
        redisTemplate.opsForValue().set("promo_door_count_"+promoId, itemModel.getStock().intValue()*5);
    }

    @Override
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) {
        //判断库存是否已售罄
        if (redisTemplate.hasKey("promo_item_stock_invalid_"+itemId)) {
            return null;
        }

        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null) {
            return null;
        }

//        UserModel userModel = userService.getUserById(userId);
        UserModel userModel = userService.getUserByIdInCache(userId);
        if (userModel == null) {
            return null;
        }

        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        //dataObject->model
        PromoModel promoModel = convertFromDataObject(promoDO);
        if (promoDO == null) {
            return null;
        }
        DateTime now = new DateTime();
        if (promoModel.getStartDate().isAfterNow()) {
            promoModel.setStatus(1);
        }
        else if(promoModel.getStartDate().isBeforeNow()) {
            promoModel.setStatus(3);
        }
        else {
            promoModel.setStatus(2);
        }
        if (promoModel.getStatus().intValue() != 2) {
            return null;
        }

        //获取秒杀大闸的count数量
        long result = redisTemplate.opsForValue().increment("promo_door_count_"+promoId, -1);
        if (result < 0) {
            return null;
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("promo_token_"+promoId+"_userid_"+userId+"_itemid_"+itemId, token);
        redisTemplate.expire("promo_token_"+promoId, 5, TimeUnit.MINUTES);

        return token;
    }
}
