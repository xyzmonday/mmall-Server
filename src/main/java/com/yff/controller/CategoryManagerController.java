package com.yff.controller;

import com.yff.common.Const;
import com.yff.common.ResponseCode;
import com.yff.common.ServerResponse;
import com.yff.entity.Category;
import com.yff.entity.User;
import com.yff.service.ICategoryService;
import com.yff.service.IUserService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 分类管理
 */
@Controller
@RequestMapping("/manage/category")
public class CategoryManagerController {

    @Autowired
    private IUserService iUserService;

    @Autowired
    private ICategoryService iCategoryService;

    /**
     * 增加分类
     */
    @GetMapping("/addCategory")
    @ResponseBody
    public ServerResponse<String> addCategory(HttpSession session,
                                              String categoryName,
                                              @RequestParam(value = "parentId", defaultValue = "0") Integer parentId) {
        //1. 是否登陆
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请先登录");
        }
        //2. 校验是否是管理员
        ServerResponse checkAdminRole = iUserService.checkAdminRole(user);
        if (!checkAdminRole.isSuccess()) {
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }

        //3.添加分类
        return iCategoryService.addCategory(categoryName, parentId);
    }

    /**
     * 更新分类名称
     *
     * @param session
     * @param categoryName
     * @param parentId
     * @return
     */
    @GetMapping("/updateCategoryName")
    @ResponseBody
    public ServerResponse<String> updateCategoryName(HttpSession session, String categoryName,
                                                     @RequestParam(value = "parentId", defaultValue = "0") Integer parentId) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请先登录");
        }
        //2. 校验是否是管理员
        ServerResponse checkAdminRole = iUserService.checkAdminRole(user);
        if (!checkAdminRole.isSuccess()) {
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }
        return iCategoryService.updateCategoryName(categoryName, parentId);
    }

    /**
     * 获取与当前节点平级的所有节点
     *
     * @param session
     * @param categoryId
     * @return
     */
    @GetMapping("/getCategory")
    @ResponseBody
    public ServerResponse<List<Category>> getChildrenParallelCategory(HttpSession session,
                                                                      @RequestParam(value = "categoryId", defaultValue = "0") Integer categoryId) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请先登录");
        }
        //2. 校验是否是管理员
        ServerResponse checkAdminRole = iUserService.checkAdminRole(user);
        if (!checkAdminRole.isSuccess()) {
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }
        return iCategoryService.getChildrenParallelCategoryByCategoryId(categoryId);
    }

    /**
     * 获取当前节点的子节点，并且递归它所有的子节点的孩子节点
     *
     * @param session
     * @param categoryId
     * @return
     */
    @GetMapping("/getDeepCategory")
    @ResponseBody
    public ServerResponse<List<Integer>> getCategoryAndChildrenCategory(HttpSession session, @RequestParam(value = "categoryId", defaultValue = "0") Integer categoryId) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请先登录");
        }
        //2. 校验是否是管理员
        ServerResponse checkAdminRole = iUserService.checkAdminRole(user);
        if (!checkAdminRole.isSuccess()) {
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }
        return iCategoryService.getCategoryAndChildrenCategoryByCategoryId(categoryId);
    }

}



