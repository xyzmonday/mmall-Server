package com.yff.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.yff.common.ServerResponse;
import com.yff.dao.CategoryMapper;
import com.yff.entity.Category;
import com.yff.service.ICategoryService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;
import java.util.Set;


@Service(value = "iCategoryService")
public class CategoryServiceImpl implements ICategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public ServerResponse<String> addCategory(String categoryName, Integer parentId) {
        if (parentId == null || StringUtils.isBlank(categoryName)) {
            return ServerResponse.createByErrorMessage("参数错误");
        }

        Category category = new Category();
        category.setName(categoryName);
        category.setParentId(parentId);
        category.setCreateTime(new Date());
        category.setStatus(true);
        int result = categoryMapper.insert(category);
        if (result <= 0) {
            return ServerResponse.createByErrorMessage("添加分类失败");
        }
        return ServerResponse.createBySuccess("添加分类成功!!!");
    }

    @Override
    public ServerResponse<String> updateCategoryName(String categoryName, Integer categoryId) {
        if (categoryId == null || StringUtils.isBlank(categoryName)) {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        Category category = new Category();
        category.setName(categoryName);
        category.setId(categoryId);
        int result = categoryMapper.updateByPrimaryKeySelective(category);
        if (result <= 0) {
            return ServerResponse.createByErrorMessage("更新失败");
        }
        return ServerResponse.createByErrorMessage("更新成功!!!");
    }

    @Override
    public ServerResponse<List<Category>> getChildrenParallelCategoryByCategoryId(Integer categoryId) {
        List<Category> categoryList = categoryMapper.getChildrenCategoryByCategoryId(categoryId);
        if(CollectionUtils.isEmpty(categoryList)) {
            System.out.println("未找到当前分类的子分类");
        }
        return ServerResponse.createBySuccess(categoryList);
    }

    public ServerResponse<List<Integer>> getCategoryAndChildrenCategoryByCategoryId(Integer categoryId) {
        Set<Category> categorySet = Sets.newHashSet();
        findChildCategory(categorySet,categoryId);
        List<Integer> categoryIdList = Lists.newArrayList();
        if(categoryId != null) {
            for (Category category : categorySet) {
                categoryIdList.add(category.getId());
            }
        }
        return ServerResponse.createBySuccess(categoryIdList);
    }

    private Set<Category> findChildCategory(Set<Category> categorySet,Integer categoryId) {
        Category category = categoryMapper.selectByPrimaryKey(categoryId);
        if(category != null) {
            categorySet.add(category);
        }
        //这里没有查询到，mybatis返回的是空集合
        List<Category> list = categoryMapper.getChildrenCategoryByCategoryId(categoryId);
        for (Category item : list) {
            findChildCategory(categorySet,item.getId());
        }
        return categorySet;
    }
}
