package com.miaoShaProject.service.impls;

import com.miaoShaProject.controller.viewObject.UserVO;
import com.miaoShaProject.dao.OrderInfoDOMapper;
import com.miaoShaProject.dao.SequenceInfoDOMapper;
import com.miaoShaProject.dao.StockLogDOMapper;
import com.miaoShaProject.dataObject.OrderInfoDO;
import com.miaoShaProject.dataObject.SequenceInfoDO;
import com.miaoShaProject.dataObject.StockLogDO;
import com.miaoShaProject.error.BusinessException;
import com.miaoShaProject.error.EnumBusinessError;
import com.miaoShaProject.service.ItemService;
import com.miaoShaProject.service.OrderService;
import com.miaoShaProject.service.UserService;
import com.miaoShaProject.service.model.ItemModel;
import com.miaoShaProject.service.model.OrderModel;
import com.miaoShaProject.service.model.UserModel;
import com.mysql.cj.x.protobuf.MysqlxCrud;
import org.apache.catalina.User;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderInfoDOMapper orderInfoDOMapper;

    @Autowired
    private SequenceInfoDOMapper sequenceInfoDOMapper;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws BusinessException{
        //校验下单状态，下单商品是否存在，用户是否合法，数量是否正确
//        ItemModel itemModel = itemService.getItemById(itemId);
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null) {
            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR, "商品信息不存在");
        }
//
////        UserModel userModel = userService.getUserById(userId);
//        UserModel userModel = userService.getUserByIdInCache(userId);
//        if (userModel == null) {
//            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR, "用户信息不存在");
//        }
        if (amount < 0 || amount > 99) {
            throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR, "数量信息不正确");
        }
        //校验活动信息
//        if (promoId != null) {
//            if (promoId.intValue() != itemModel.getPromoModel().getId()) {
//                throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR, "活动信息不正确");
//            }
//            else if (itemModel.getPromoModel().getStatus().intValue() != 2){
//                //校验活动是否在进行中
//                throw new BusinessException(EnumBusinessError.PARAMETER_VALIDATION_ERROR, "活动信息不正确");
//            }
//        }
        //落单减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if (!result) {
            throw new BusinessException(EnumBusinessError.STOCK_NOT_ENOUGH);
        }
        //订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        if (promoId != null) {
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        }
        else {
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setPromoId(promoId);
        orderModel.setOrderPrice(itemModel.getPrice().multiply(new BigDecimal(amount)));
        //生成订单号
        orderModel.setId(generateOrderNo());
        OrderInfoDO orderInfoDO = convertFromOrderModel(orderModel);
        orderInfoDOMapper.insertSelective(orderInfoDO);
        //加上商品销量
        itemService.increaseSales(itemId, amount);
        //设置库存流水状态为成功
        StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
        if (stockLogDO == null) {
            throw new BusinessException(EnumBusinessError.UNKNOWN_ERROR);
        }
        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);
        //最近一次事务提交成功后执行
//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//            @Override
//            public void afterCommit() {
//                boolean mqResult = itemService.asyncDecreaseStock(itemId, amount);
////                if (!mqResult) {
////                    itemService.increaseStock(itemId, amount);
////                    throw new BusinessException(EnumBusinessError.MQ_SEND_FAIL);
////                }
//            }
//        });
        //返回前端
        return orderModel;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private String generateOrderNo() {
        StringBuilder stringBuilder = new StringBuilder();
        //订单号有16位
        //前8位为时间信息，年月日
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);
        //中间6位为自增序列
        int sequence = 0;
        SequenceInfoDO sequenceInfoDO = sequenceInfoDOMapper.getSequenceByName("order_info");
        sequence = sequenceInfoDO.getCurrentValue();
        sequenceInfoDO.setCurrentValue(sequenceInfoDO.getCurrentValue() + sequenceInfoDO.getStep());
        sequenceInfoDOMapper.updateByNameSelective(sequenceInfoDO);
        String sequenceStr = String.valueOf(sequence);
        for (int i=0;i<6-sequenceStr.length();i++) {
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);
        //后两位为分库分表位
        stringBuilder.append("00");

        return stringBuilder.toString();
    }
    private OrderInfoDO convertFromOrderModel(OrderModel orderModel) {
        if (orderModel == null) {
            return null;
        }
        OrderInfoDO orderInfoDO = new OrderInfoDO();
        BeanUtils.copyProperties(orderModel, orderInfoDO);
        orderInfoDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderInfoDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderInfoDO;
    }
}
