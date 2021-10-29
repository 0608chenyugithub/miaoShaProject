package com.miaoShaProject.controller;


import com.google.common.util.concurrent.RateLimiter;
import com.miaoShaProject.error.BusinessException;
import com.miaoShaProject.error.EnumBusinessError;
import com.miaoShaProject.mq.MqProducer;
import com.miaoShaProject.response.CommonReturnType;
import com.miaoShaProject.service.ItemService;
import com.miaoShaProject.service.OrderService;
import com.miaoShaProject.service.PromoService;
import com.miaoShaProject.service.model.OrderModel;
import com.miaoShaProject.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.*;
import java.util.function.BinaryOperator;

@Controller("order")
@RequestMapping("/order")
public class OrderController extends BaseController{
    @Autowired
    private OrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private ItemService itemService;

    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    private RateLimiter orderCreateRateLimiter;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(20);
        orderCreateRateLimiter = RateLimiter.create(10);
    }

    @RequestMapping(value = "/generatetoken", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType generateToken(@RequestParam(name="itemId") Integer itemId,
                                        @RequestParam(name="promoId") Integer promoId) throws BusinessException {
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EnumBusinessError.USER_NOT_LOGIN, "用户没有登录");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null) {
            throw new BusinessException(EnumBusinessError.USER_NOT_LOGIN, "用户没有登录");
        }
        //获取秒杀令牌
        String promoToken = promoService.generateSecondKillToken(promoId, itemId, userModel.getId());
        if (promoToken == null) {
            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR, "生成令牌失败");
        }

        return CommonReturnType.create(promoToken);
    }

    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name="itemId") Integer itemId,
                                        @RequestParam(name="amount") Integer amount,
                                        @RequestParam(name="promoId", required = false) Integer promoId,
                                        @RequestParam(name="promoToken") String promoToken) throws BusinessException {
//        Boolean isLogin = (Boolean) this.httpServletRequest.getSession().getAttribute("IS_LOGIN");
//        if (isLogin == null || !isLogin.booleanValue()) {
//            throw new BusinessException(EnumBusinessError.USER_NOT_LOGIN, "用户没有登录");
//        }
        if(!orderCreateRateLimiter.tryAcquire()) {
            throw new BusinessException(EnumBusinessError.RATE_LIMIT);
        }
        String token = httpServletRequest.getParameterMap().get("token")[0];
//        if (StringUtils.isEmpty(token)) {
//            throw new BusinessException(EnumBusinessError.USER_NOT_LOGIN, "用户没有登录");
//        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if (userModel == null) {
            throw new BusinessException(EnumBusinessError.USER_NOT_LOGIN, "用户没有登录");
        }

//        UserModel userModel = (UserModel) this.httpServletRequest.getSession().getAttribute("LOGIN_USER");
//        OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId, amount);
        //校验秒杀令牌是否正确
        if(promoId != null) {
            String inRedisPromoToken = (String) redisTemplate.opsForValue().get("promo_token_"+promoId+"_userid_"+userModel.getId()+"_itemid_"+itemId);
            if (inRedisPromoToken != null && StringUtils.equals(inRedisPromoToken, promoToken)) {
                throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
        }
//        //判断库存是否已售罄
//        if (redisTemplate.hasKey("promo_item_stock_invalid_"+itemId)) {
//            throw new BusinessException(EnumBusinessError.STOCK_NOT_ENOUGH);
//        }
        //同步调用线程池的submit方法
        //拥塞窗口为20的等待队列，用来队列化泄洪
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                //加入库存流水init状态
                String stockLogId = itemService.initStockLog(itemId, amount);
                //再去完成
                if(!mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId, promoId, amount, stockLogId)) {
                    throw new BusinessException(EnumBusinessError.UNKNOWN_ERROR,"下单失败");
                }
                return null;
            }
        });
        try {
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new BusinessException(EnumBusinessError.UNKNOWN_ERROR);
        } catch (ExecutionException e) {
            e.printStackTrace();
            throw new BusinessException(EnumBusinessError.UNKNOWN_ERROR);
        }
        return CommonReturnType.create(null);
    }
}
