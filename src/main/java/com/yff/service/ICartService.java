package com.yff.service;

import com.yff.common.ServerResponse;
import com.yff.vo.CartVo;

public interface ICartService {
    /**
     * 将productId产品添加到cart。
     * 注意这里要返回该用户所有的购物车数据
     * @Param userId
     * @param count
     * @param productId
     * @return
     */
    ServerResponse<CartVo> add(Integer userId, Integer productId,Integer count);

    ServerResponse<CartVo> list (Integer userId);

    ServerResponse<CartVo> update(Integer userId,Integer productId,Integer count);

    ServerResponse<CartVo> delete(Integer userId,String productIds);

    ServerResponse<CartVo> selectOrUnSelect(Integer userId, Integer checked,Integer productId);

    ServerResponse<Integer> getCarProductCount(Integer userId);
}
