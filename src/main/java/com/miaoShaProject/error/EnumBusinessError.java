package com.miaoShaProject.error;

import com.miaoShaProject.error.CommonError;

public enum EnumBusinessError implements CommonError {
    //通用错误类型
    PARAMETER_VALIDATION_ERROR(10001, "parameter not valid"),
    //未知错误
    UNKNOWN_ERROR(10002, "unknown error"),
    //2000开头为用户信息相关错误信息
    USER_NOT_EXIST(20001, "user not exist"),
    USER_LOGIN_FAIL(20002, "username or password not correct"),
    USER_NOT_LOGIN(20003, "user not login"),
    //3000开头为交易信息相关错误信息
    STOCK_NOT_ENOUGH(30001, "stock not enough"),
    MQ_SEND_FAIL(30002, "库存异步消息失败"),
    RATE_LIMIT(30003, "活动太火爆，请稍后再试")
    ;

    private EnumBusinessError(int errCode, String errMsg) {
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    private int errCode;
    private String errMsg;

    @Override
    public int getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }
}
