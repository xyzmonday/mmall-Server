package com.yff.service;

import com.github.pagehelper.PageInfo;
import com.yff.common.ServerResponse;
import com.yff.entity.Product;
import com.yff.vo.ProductDetailVo;
import com.yff.vo.ProductListVo;

public interface IProductService {

    /**
     * 修改产品的销售状态
     * @param productId
     * @param status
     * @return
     */
    ServerResponse<String> setSaleStatus(Integer productId, Integer status);

    /**
     * 更新或新增产品
     * @param product
     * @return
     */
    ServerResponse<String> saveOrUpdateProduct(Product product);

    /**
     * 查询产品的详情
     * @param productId
     * @return
     */
    ServerResponse<ProductDetailVo> manageProductDetail(Integer productId);

    /**
     * 分页获取产品列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    ServerResponse<PageInfo> getProductList(Integer pageNum,Integer pageSize);

    /**
     * 条件搜索产品列表
     * @param productName
     * @param productId
     * @param pageNum
     * @param pageSize
     * @return
     */
    ServerResponse<PageInfo> searchProduct(String productName,Integer productId,
                                           Integer pageNum, Integer pageSize);

    ServerResponse<ProductDetailVo> getProductDetail(Integer productId);

    /**
     * 获取该分类下所有能够匹配keyword的产品列表
     * @param keyword
     * @param categoryId
     * @param pageNum
     * @param pageSize
     * @return
     */
    ServerResponse<PageInfo<ProductListVo>> getProductByKeywordAndCategoryId(String keyword,
                                                                             Integer categoryId,
                                                                             Integer pageNum,
                                                                             Integer pageSize,
                                                                             String orderBy);
}
