package com.yff.controller;

import com.github.pagehelper.PageInfo;
import com.yff.common.ServerResponse;
import com.yff.entity.Product;
import com.yff.service.IProductService;
import com.yff.vo.ProductDetailVo;
import com.yff.vo.ProductListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private IProductService iProductService;

    /**
     * 获取产品详情
     *
     * @param productId
     * @return
     */
    @GetMapping("/detail")
    @ResponseBody
    public ServerResponse<ProductDetailVo> detail(Integer productId) {
        return iProductService.getProductDetail(productId);
    }

    /**
     * 获取categoryId分类下（包括值分类），能够匹配keyword的产品列表
     * @param keyword
     * @param categoryId
     * @param pageNum
     * @param pageSize
     * @param orderBy
     * @return
     */
    @GetMapping("/list")
    @ResponseBody
    public ServerResponse<PageInfo<ProductListVo>> search(@RequestParam(value = "keyword", required = false) String keyword,
                                                          @RequestParam(value = "categoryId", required = false) Integer categoryId,
                                                          @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                          @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                                          @RequestParam(value = "orderBy", required = false) String orderBy) {
        return iProductService.getProductByKeywordAndCategoryId(keyword, categoryId, pageNum, pageSize, orderBy);
    }
}
