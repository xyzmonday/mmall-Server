package com.yff.dao;

import com.yff.entity.Cart;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CartMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Cart record);

    int insertSelective(Cart record);

    Cart selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Cart record);

    int updateByPrimaryKey(Cart record);

    /**
     * 查询userId的购物车
     * @param userId
     * @param productId
     * @return
     */
    Cart selectCart(@Param("userId") Integer userId, @Param("productId") Integer productId);

    List<Cart> selectCartByUserId(Integer userId);

    int selectCartProductCheckedStatusByUserId(Integer userId);

    int deleteProductByIds(@Param("userId") Integer userId,@Param("productIdList") List<String> productIdList);

    int selectOrUnSelect(@Param("userId") Integer userId,@Param("checked") Integer checked,@Param("productId") Integer productId);

    int selectCartProductCount(Integer userId);

    /**
     * 查询用户已经勾选的商品的购物车
     * @return
     */
    List<Cart> selectCheckedCartByUserId(Integer userId);
}