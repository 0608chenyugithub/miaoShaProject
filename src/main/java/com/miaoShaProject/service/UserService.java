package com.miaoShaProject.service;

import com.miaoShaProject.error.BusinessException;
import com.miaoShaProject.service.model.UserModel;
import org.apache.catalina.User;

public interface UserService {
    UserModel getUserById(Integer id);

    //通过缓存获取用户对象
    UserModel getUserByIdInCache(Integer id);

    void register(UserModel userModel) throws BusinessException;
    UserModel validateLogin(String telphone, String encrptPassword) throws BusinessException;
}
