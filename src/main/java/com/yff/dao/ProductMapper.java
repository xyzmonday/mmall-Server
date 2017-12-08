package com.yff.dao;

import com.yff.entity.Product;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Product record);

    int insertSelective(Product record);

    Product selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Product record);

    int updateByPrimaryKey(Product record);

    List<Product> selectList();

    List<Product> searchList(@Param("productName") String productName,
                             @Param("productId") Integer productId);

    /**
     * 查询满足该分类下的能够匹配keyword的产品列表
     * @param keyword
     * @param categoryIds
     * @return
     */
    List<Product> selectProductByKeywordAndCategoryIds(@Param("keyword") String keyword,
                                                       @Param("categoryIds") List<Integer> categoryIds);
}