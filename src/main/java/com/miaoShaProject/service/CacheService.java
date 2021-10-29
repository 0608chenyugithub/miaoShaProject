package com.miaoShaProject.service;

//封装本地缓存
public interface CacheService {
    //存
    void setCommonCache(String key, Object value);

    //取
    Object getFromCommonCache(String key);
}
