package com.yff.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.github.pagehelper.PageInfo;
import com.yff.common.Const;
import com.yff.common.ResponseCode;
import com.yff.common.ServerResponse;
import com.yff.entity.Order;
import com.yff.entity.User;
import com.yff.service.IOrderService;
import com.yff.vo.OrderVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Controller
@RequestMapping("/order")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private IOrderService iOrderService;


    @GetMapping("/create")
    @ResponseBody
    public ServerResponse create(HttpSession session, Integer shippingId) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.createOrder(user.getId(), shippingId);
    }


    @GetMapping("/cancel")
    @ResponseBody
    public ServerResponse cancel(HttpSession session, Long orderNo) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.cancelOrder(user.getId(), orderNo);
    }

    //获取购物车中已经选中的商品
    @GetMapping("/getOrderCartProduct")
    @ResponseBody
    public ServerResponse getOrderCartProduct(HttpSession session) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getCheckedOrderProduct(user.getId());
    }

    @GetMapping("/detail")
    @ResponseBody
    public ServerResponse<OrderVo> getOrderDetail(HttpSession session, Long orderNo) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderDetail(user.getId(), orderNo);
    }


    @GetMapping("/list")
    @ResponseBody
    public ServerResponse<PageInfo> getPageOrderList(HttpSession session, @RequestParam(value = "pageNum", required = false
            , defaultValue = "1") Integer pageNum, @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderList(user.getId(), pageNum, pageSize);
    }


    @GetMapping("/pay")
    @ResponseBody
    public ServerResponse<Map<String, String>> pay(HttpSession session, HttpServletRequest request,
                                                   Long orderNo) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }
        String path = request.getSession().getServletContext().getRealPath("upload");
        return iOrderService.pay(user.getId(), orderNo, path);
    }

    @GetMapping("/alipayCallback")
    @ResponseBody
    public Object alipayCallback(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, String> params = new HashMap<>();
        Iterator<String> it = parameterMap.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String[] values = parameterMap.get(key);
            String result = "";
            for (int i = 0; i < values.length; i++) {
                result = i == values.length - 1 ? result + values[i] : result + values[i] + ",";
            }
            params.put(key, result);
        }
        logger.info("支付宝回调sign:{},trade_status:{},参数:{}", parameterMap.get("sign"), parameterMap.get("trade_status"), params.toString());

        //验证回调的正确定，并且还要防止重复通知

        //1.除去sign_type
        params.remove("sign_type");

        //2.签名验证
        try {
            boolean alipaySignature = AlipaySignature.rsaCheckV2(params, Configs.getPublicKey(), "utf-8",
                    Configs.getSignType());
            if (!alipaySignature) {
                return ServerResponse.createByErrorMessage("非法请求，验证不通过哦");
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //todo 各种支付回调验证
        ServerResponse<String> serverResponse = iOrderService.aliPayCallback(params);
        if (!serverResponse.isSuccess()) {
            return Const.AlipayCallback.RESPONSE_FAILED;
        }
        return Const.AlipayCallback.RESPONSE_SUCCESS;
    }


    @GetMapping("/queryOrderPayStatus")
    @ResponseBody
    public ServerResponse<Boolean> queryOrderPayStatus(HttpSession session, Long orderNo) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        ServerResponse<Boolean> serverResponse = iOrderService.queryOrderPayStatus(user.getId(), orderNo);
        if (serverResponse.isSuccess()) {
            return ServerResponse.createBySuccess(true);
        }
        return ServerResponse.createBySuccess(false);
    }
}
