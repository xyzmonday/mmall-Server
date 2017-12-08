package com.yff.controller;

import com.github.pagehelper.PageInfo;
import com.yff.common.Const;
import com.yff.common.ResponseCode;
import com.yff.common.ServerResponse;
import com.yff.entity.User;
import com.yff.service.IOrderService;
import com.yff.service.IUserService;
import com.yff.vo.OrderVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/manage/order")
public class OrderManageController {

    @Autowired
    private IOrderService iOrderService;
    @Autowired
    private IUserService iUserService;

    @GetMapping("/list")
    @ResponseBody
    public ServerResponse<PageInfo<OrderVo>> getPageOrderList(HttpSession session, @RequestParam(value = "pageNum", required = false
            , defaultValue = "1") Integer pageNum, @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请先登录");
        }
        //2. 校验是否是管理员
        ServerResponse checkAdminRole = iUserService.checkAdminRole(user);
        if (!checkAdminRole.isSuccess()) {
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }
        return iOrderService.manageList(pageNum, pageSize);
    }


    @GetMapping("/detail")
    @ResponseBody
    public ServerResponse<OrderVo> getOrderDetail(HttpSession session, Long orderNo) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请先登录");
        }
        //2. 校验是否是管理员
        ServerResponse checkAdminRole = iUserService.checkAdminRole(user);
        if (!checkAdminRole.isSuccess()) {
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }
        return iOrderService.manageOrderDetail(orderNo);
    }


    @GetMapping("/search")
    @ResponseBody
    public ServerResponse<PageInfo<OrderVo>> orderSearch(HttpSession session, Long orderNo,
                                                         @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                         @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请先登录");
        }
        //2. 校验是否是管理员
        ServerResponse checkAdminRole = iUserService.checkAdminRole(user);
        if (!checkAdminRole.isSuccess()) {
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }
        return iOrderService.manageSearch(orderNo, pageNum, pageSize);
    }

    @GetMapping("/sendGoods")
    @ResponseBody
    public ServerResponse<String> sendGoods(HttpSession session, Long orderNo) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，请先登录");
        }
        //2. 校验是否是管理员
        ServerResponse checkAdminRole = iUserService.checkAdminRole(user);
        if (!checkAdminRole.isSuccess()) {
            return ServerResponse.createByErrorMessage("不是管理员，无权限操作");
        }
        return iOrderService.manageSendGoods(orderNo);
    }


}
