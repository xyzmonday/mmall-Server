package com.yff.service;

import com.yff.common.ServerResponse;
import com.yff.entity.Category;

import javax.servlet.http.HttpSession;
import java.util.List;

public interface ICategoryService {

    /**
     * 增加一个分类
     *
     * @param categoryName
     * @param parentId
     * @return
     */
    ServerResponse<String> addCategory(String categoryName, Integer parentId);

    /**
     * 更新分类名称
     *
     * @param categoryName
     * @param categoryId
     * @return
     */
    ServerResponse<String> updateCategoryName(String categoryName,Integer categoryId);

    /**
     * 获取当前分类的子分类（下一级分类）
     * @param parentId
     * @return
     */
    ServerResponse<List<Category>> getChildrenParallelCategoryByCategoryId(Integer parentId);

    /**
     * 递归获取所有子节点的id（包括自己的id）
     * @param categoryId
     * @return
     */
    ServerResponse<List<Integer>> getCategoryAndChildrenCategoryByCategoryId(Integer categoryId);
}
