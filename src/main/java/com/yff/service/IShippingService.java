package com.yff.service;

import com.github.pagehelper.PageInfo;
import com.yff.common.ServerResponse;
import com.yff.entity.Shipping;

import java.util.Map;

public interface IShippingService {

    ServerResponse<Map<String,Object>> add(Integer userId, Shipping shipping);
    ServerResponse<String> delete(Integer userId,Integer shippingId);
    ServerResponse<String> update(Integer userId,Shipping shipping);
    ServerResponse<Shipping> select(Integer userId,Integer shippingId);

    ServerResponse<PageInfo<Shipping>> list(Integer userId,int pageNum,int pageSize);
}
