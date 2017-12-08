package com.yff.controller;

import com.github.pagehelper.PageInfo;
import com.yff.common.Const;
import com.yff.common.ResponseCode;
import com.yff.common.ServerResponse;
import com.yff.entity.Shipping;
import com.yff.entity.User;
import com.yff.service.IShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping("/shipping")
public class ShippingController {

    @Autowired
    private IShippingService iShippingService;


    @GetMapping("/add")
    @ResponseBody
    public ServerResponse<Map<String, Object>> add(HttpSession session, Shipping shipping) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iShippingService.add(user.getId(), shipping);
    }

    @GetMapping("/delete")
    @ResponseBody
    public ServerResponse<String> delete(HttpSession session, Integer shippingId) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iShippingService.delete(user.getId(), shippingId);
    }

    @GetMapping("/update")
    @ResponseBody
    public ServerResponse<String> update(HttpSession session, Shipping shipping) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iShippingService.update(user.getId(), shipping);
    }

    @GetMapping("/select")
    @ResponseBody
    public ServerResponse<Shipping> update(HttpSession session, Integer shippingId) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iShippingService.select(user.getId(), shippingId);
    }

    @GetMapping("/list")
    @ResponseBody
    public ServerResponse<PageInfo<Shipping>> list(HttpSession session, @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                                                   @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iShippingService.list(user.getId(), pageNum, pageSize);
    }
}
