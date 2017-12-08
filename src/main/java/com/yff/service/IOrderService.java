package com.yff.service;

import com.github.pagehelper.PageInfo;
import com.yff.common.ServerResponse;
import com.yff.vo.OrderVo;

import javax.servlet.http.HttpSession;
import java.util.Map;

public interface IOrderService {
    ServerResponse<Map<String, String>> pay(Integer userId, Long orderNo, String path);

    ServerResponse<String> aliPayCallback(Map<String, String> params);

    /**
     * 查询订单支付状态
     * @param userId
     * @param orderNo
     * @return
     */
    ServerResponse<Boolean> queryOrderPayStatus(Integer userId, Long orderNo);

    /**
     * 创建订单
     * @param userId
     * @param shippingId
     * @return
     */
    ServerResponse  createOrder(Integer userId,Integer shippingId);

    /**
     * 取消订单
     * @param userId
     * @param orderNo
     * @return
     */
    ServerResponse<String> cancelOrder(Integer userId,Long orderNo);

    /**
     * 获取用户订单中选中的商品信息
     * @param userId
     * @return
     */
    ServerResponse getCheckedOrderProduct(Integer userId);

    /**
     * 获取订单明细
     * @param userId
     * @param orderNo
     * @return
     */
    ServerResponse<OrderVo> getOrderDetail(Integer userId, Long orderNo);

    /**
     * 获取用户的订单列表
     * @param userId
     * @param pageNum
     * @param pageSize
     * @return
     */
    ServerResponse<PageInfo<OrderVo>> getOrderList(Integer userId,Integer pageNum,Integer pageSize);

    /**
     * 管理员获取订单列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    ServerResponse<PageInfo<OrderVo>> manageList(Integer pageNum,Integer pageSize);


    /**
     * 管理员查看订单明细
     * @param orderNo
     * @return
     */
    ServerResponse<OrderVo> manageOrderDetail(Long orderNo);

    /**
     * 管理员分页查询订单明细
     * @param orderNo
     * @param pageNum
     * @param pageSize
     * @return
     */
    ServerResponse<PageInfo<OrderVo>> manageSearch(Long orderNo,Integer pageNum,Integer pageSize);

    /**
     * 管理员订单发货
     * @param orderNo
     * @return
     */
    ServerResponse<String> manageSendGoods(Long orderNo);

}
