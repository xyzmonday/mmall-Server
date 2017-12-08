package com.yff.service.impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayMonitorService;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayMonitorServiceImpl;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.service.impl.AlipayTradeWithHBServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.yff.common.Const;
import com.yff.common.ServerResponse;
import com.yff.dao.*;
import com.yff.entity.*;
import com.yff.service.IOrderService;
import com.yff.util.BigDecimalUtil;
import com.yff.util.DateTimeUtil;
import com.yff.util.PropertiesUtil;
import com.yff.vo.OrderItemVo;
import com.yff.vo.OrderProductVo;
import com.yff.vo.OrderVo;
import com.yff.vo.ShippingVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.FileVisitor;
import java.util.*;

@Service("iOrderService")
public class OrderServiceImpl implements IOrderService {

    private static Log log = LogFactory.getLog(OrderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private PayInfoMapper payInfoMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ShippingMapper shippingMapper;

    // 支付宝当面付2.0服务
    private static AlipayTradeService tradeService;

    // 支付宝当面付2.0服务（集成了交易保障接口逻辑）
    private static AlipayTradeService tradeWithHBService;

    // 支付宝交易保障接口服务，供测试接口api使用，请先阅读readme.txt
    private static AlipayMonitorService monitorService;

    static {
        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("properties/zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();

        // 支付宝当面付2.0服务（集成了交易保障接口逻辑）
        tradeWithHBService = new AlipayTradeWithHBServiceImpl.ClientBuilder().build();

        /** 如果需要在程序中覆盖Configs提供的默认参数, 可以使用ClientBuilder类的setXXX方法修改默认参数 否则使用代码中的默认设置 */
        monitorService = new AlipayMonitorServiceImpl.ClientBuilder()
                .setGatewayUrl("http://mcloudmonitor.com/gateway.do").setCharset("GBK")
                .setFormat("json").build();
    }

    @Override
    public ServerResponse createOrder(Integer userId, Integer shippingId) {
        //1.获取已经勾选的商品的购物车
        List<Cart> carts = cartMapper.selectCheckedCartByUserId(userId);
        if (carts != null && carts.size() > 0) {

        }
        ServerResponse<List<OrderItem>> serverResponse = getOrderItemList(userId, carts);

        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        //2.计算总价
        List<OrderItem> orderItemList = serverResponse.getData();
        BigDecimal payment = getOrderTotalPrice(orderItemList);

        if (CollectionUtils.isEmpty(orderItemList)) {
            return ServerResponse.createByErrorMessage("购物车为空");
        }

        //3.生成订单
        Order order = assembleOrder(userId, shippingId, payment);

        if (order == null) {
            return ServerResponse.createByErrorMessage("生成订单错误");
        }

        //4.插入订单明细
        orderItemMapper.batchInsert(orderItemList);

        //5.减少产品的库存
        reduceProductStock(orderItemList);

        //6.清空购物车
        clearCart(carts);

        //7.返回给前端数据
        OrderVo orderVo = assembleOrderVo(order, orderItemList);
        return ServerResponse.createBySuccess(orderVo);
    }

    @Override
    public ServerResponse<String> cancelOrder(Integer userId, Long orderNo) {
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if (order == null) {
            return ServerResponse.createByErrorMessage("该用户的订单不存在");
        }

        if (order.getStatus() != Const.OrderStatusEnum.NO_PAY.getCode()) {
            return ServerResponse.createByErrorMessage("订单已经付款,无法取消");
        }

        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(Const.OrderStatusEnum.CANCELED.getCode());
        int result = orderMapper.updateByPrimaryKey(updateOrder);
        if (result <= 0) {
            return ServerResponse.createByErrorMessage("取消订单失败");
        }
        return ServerResponse.createBySuccess("取消成功");
    }

    @Override
    public ServerResponse getCheckedOrderProduct(Integer userId) {
        OrderProductVo orderProductVo = new OrderProductVo();

        //1.从购物车中获取订单详情
        List<Cart> carts = cartMapper.selectCheckedCartByUserId(userId);
        ServerResponse<List<OrderItem>> serverResponse = this.getOrderItemList(userId, carts);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        //2.获取OrderItemVo
        List<OrderItem> orderItems = serverResponse.getData();
        List<OrderItemVo> orderItemVoList = new ArrayList<>();

        BigDecimal payment = new BigDecimal("0");
        for (OrderItem orderItem : orderItems) {
            //计算总价
            payment = BigDecimalUtil.add(payment.doubleValue(), orderItem.getTotalPrice().doubleValue());
            OrderItemVo orderItemVo = this.assembleOrderItemVo(orderItem);
            orderItemVoList.add(orderItemVo);
        }
        orderProductVo.setProductTotalPrice(payment);
        orderProductVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        orderProductVo.setOrderItemVoList(orderItemVoList);
        return ServerResponse.createBySuccess(orderProductVo);
    }

    /**
     * 组建订单详情。
     *
     * @param order
     * @param orderItemList
     * @return
     */
    private OrderVo assembleOrderVo(Order order, List<OrderItem> orderItemList) {
        OrderVo orderVo = new OrderVo();
        //订单号
        orderVo.setOrderNo(order.getOrderNo());
        //支付金额
        orderVo.setPayment(order.getPayment());
        //支付类型
        orderVo.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());
        orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(orderVo.getPaymentType()).getValue());
        //运费
        orderVo.setPostage(order.getPostage());
        //订单状态
        orderVo.setStatus(order.getStatus());
        orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(orderVo.getStatus()).getValue());
        //发货地址
        Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId());
        if (shipping != null) {
            orderVo.setShippingId(shipping.getId());
            orderVo.setShippingVo(assembleShippingVo(shipping));
        }

        //支付时间
        orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
        orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
        orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
        orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));
        orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));

        //图片地址
        orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

        List<OrderItemVo> orderItemVoList = new ArrayList<>();
        for (OrderItem orderItem : orderItemList) {
            OrderItemVo orderItemVo = assembleOrderItemVo(orderItem);
            orderItemVoList.add(orderItemVo);
        }
        orderVo.setOrderItemVoList(orderItemVoList);
        return orderVo;
    }

    private OrderItemVo assembleOrderItemVo(OrderItem orderItem) {
        OrderItemVo orderItemVo = new OrderItemVo();
        orderItemVo.setOrderNo(orderItem.getOrderNo());
        orderItemVo.setProductId(orderItem.getProductId());
        orderItemVo.setProductName(orderItem.getProductName());
        orderItemVo.setProductImage(orderItem.getProductImage());
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
        orderItemVo.setQuantity(orderItem.getQuantity());
        orderItemVo.setTotalPrice(orderItem.getTotalPrice());

        orderItemVo.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));
        return orderItemVo;
    }

    private ShippingVo assembleShippingVo(Shipping shipping) {
        ShippingVo shippingVo = new ShippingVo();
        shippingVo.setReceiverName(shipping.getReceiverName());
        shippingVo.setReceiverAddress(shipping.getReceiverAddress());
        shippingVo.setReceiverProvince(shipping.getReceiverProvince());
        shippingVo.setReceiverCity(shipping.getReceiverCity());
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
        shippingVo.setReceiverMobile(shipping.getReceiverMobile());
        shippingVo.setReceiverZip(shipping.getReceiverZip());
        shippingVo.setReceiverPhone(shippingVo.getReceiverPhone());
        return shippingVo;
    }

    private void clearCart(List<Cart> carts) {
        for (Cart cart : carts) {
            cartMapper.deleteByPrimaryKey(cart.getId());
        }
    }

    private void reduceProductStock(List<OrderItem> orderItemList) {
        for (OrderItem orderItem : orderItemList) {
            Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());
            product.setStock(product.getStock() - orderItem.getQuantity());
            productMapper.updateByPrimaryKeySelective(product);
        }
    }

    private Order assembleOrder(Integer usrId, Integer shippingId, BigDecimal payment) {
        long orderNo = getOrderNo();
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode());
        order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());
        order.setPayment(payment);
        order.setUserId(usrId);
        order.setShippingId(shippingId);
        //创建时间
        //付款时间
        int result = orderMapper.insertSelective(order);
        if (result > 0) {
            return order;
        }
        return null;
    }

    private long getOrderNo() {
        long currentTime = System.currentTimeMillis();
        return currentTime + new Random().nextInt(100);
    }


    private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList) {
        BigDecimal payment = new BigDecimal("0");
        for (OrderItem orderItem : orderItemList) {
            payment = BigDecimalUtil.add(payment.doubleValue(), orderItem.getTotalPrice().doubleValue());
        }
        return payment;
    }

    /**
     * 获取该用户的订单详情列表
     *
     * @param userId
     * @param carts
     * @return
     */
    private ServerResponse<List<OrderItem>> getOrderItemList(Integer userId, List<Cart> carts) {
        List<OrderItem> orderItemList = new ArrayList<>();
        if (carts == null || carts.size() == 0) {
            return ServerResponse.createByErrorMessage("购物车为空");
        }

        //2.校验购物车的状态
        for (Cart cart : carts) {
            OrderItem orderItem = new OrderItem();
            Product product = productMapper.selectByPrimaryKey(cart.getProductId());
            if (Const.ProductStatusEnum.ON_SALE.getCode() != product.getStatus()) {
                return ServerResponse.createByErrorMessage("产品" + product.getName() + "不在售卖状态");
            }
            if (cart.getQuantity() > product.getStock()) {
                return ServerResponse.createByErrorMessage("产品" + product.getName() + "库存不足");
            }
            //3.组装OrderItem
            orderItem.setUserId(userId);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            //当时购买的单价
            orderItem.setCurrentUnitPrice(product.getPrice());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(), cart.getQuantity()));
            orderItem.setCreateTime(new Date());
            orderItem.setUpdateTime(new Date());
            orderItemList.add(orderItem);
        }
        return ServerResponse.createBySuccess(orderItemList);
    }

    @Override
    public ServerResponse<OrderVo> getOrderDetail(Integer userId, Long orderNo) {
        //1.查询该用户的信息
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if (order == null) {
            return ServerResponse.createByErrorMessage("没有找到订单");
        }

        //2.获取该订单的详情
        List<OrderItem> orderItems = orderItemMapper.selectByOrderNoAndUserId(userId, orderNo);
        OrderVo orderVo = this.assembleOrderVo(order, orderItems);
        return ServerResponse.createBySuccess(orderVo);
    }


    @Override
    public ServerResponse<PageInfo<OrderVo>> getOrderList(Integer userId, Integer pageNum, Integer pageSize) {
        //1.获取订单集合
        PageHelper.startPage(pageNum, pageSize);
        List<Order> orderList = orderMapper.selectByUserId(userId);
        List<OrderVo> orderVoList = this.assembleOrderVo(userId, orderList);
        PageInfo<OrderVo> pageInfo = new PageInfo<>(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    @Override
    public ServerResponse<PageInfo<OrderVo>> manageList(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Order> orderList = orderMapper.selectAll();
        List<OrderVo> orderVoList = this.assembleOrderVo(null, orderList);
        PageInfo<OrderVo> pageInfo = new PageInfo<>(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);

    }

    private List<OrderVo> assembleOrderVo(Integer userId, List<Order> orderList) {
        List<OrderVo> orderVoList = new ArrayList<>();
        List<OrderItem> orderItemList = null;
        for (Order order : orderList) {
            //如果是管理员不需要userId
            if (userId == null) {
                //todo 管理员查看
                orderItemList = orderItemMapper.selectByOrderNo(order.getOrderNo());
                ;
            } else {
                //获取订单明细
                orderItemList = orderItemMapper.selectByOrderNoAndUserId(userId, order.getOrderNo());
            }
            OrderVo orderVo = this.assembleOrderVo(order, orderItemList);
            orderVoList.add(orderVo);
        }
        return orderVoList;
    }

    @Override
    public ServerResponse<OrderVo> manageOrderDetail(Long orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order != null) {
            List<OrderItem> orderItems = orderItemMapper.selectByOrderNo(orderNo);
            OrderVo orderVo = this.assembleOrderVo(order, orderItems);
            return ServerResponse.createBySuccess(orderVo);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }

    @Override
    public ServerResponse<PageInfo<OrderVo>> manageSearch(Long orderNo, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order != null) {
            List<OrderItem> orderItems = orderItemMapper.selectByOrderNo(orderNo);
            OrderVo orderVo = this.assembleOrderVo(order, orderItems);
            PageInfo<OrderVo> pageInfo = new PageInfo<>(Lists.newArrayList(orderVo));
            return ServerResponse.createBySuccess(pageInfo);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }


    @Override
    public ServerResponse<String> manageSendGoods(Long orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order != null) {
            if (order.getStatus() == Const.OrderStatusEnum.PAID.getCode()) {
                order.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
                order.setSendTime(new Date());
                orderMapper.updateByPrimaryKeySelective(order);
                return ServerResponse.createBySuccess("发货成功");
            }
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }


    @Override
    public ServerResponse<Map<String, String>> pay(Integer userId, Long orderNo, String path) {
        Map<String, String> resultMap = new HashMap<>();
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if (order == null) {
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        resultMap.put("orderNo", String.valueOf(order.getOrderNo()));

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = order.getOrderNo().toString();

        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = new StringBuilder().append("mmall扫描支付").append(order.getOrderNo().toString()).toString();

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString();

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = new StringBuilder().append("订单").append(outTradeNo).append("购买商品共").append(totalAmount).append("元").toString();

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<>();

        //查询该订单的订单明细
        List<OrderItem> orderItems = orderItemMapper.selectByOrderNoAndUserId(userId, orderNo);
        for (OrderItem orderItem : orderItems) {
            long price = BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(), new Double(100).doubleValue()).longValue();
            GoodsDetail goodsDetail = GoodsDetail.newInstance(orderItem.getProductId().toString(),
                    orderItem.getProductName(), price,
                    orderItem.getQuantity().intValue());
            goodsDetailList.add(goodsDetail);
        }


        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                //支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))
                .setGoodsDetailList(goodsDetailList);

        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                log.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                File QRImgFolder = new File(path);
                if (!QRImgFolder.exists()) {
                    QRImgFolder.setWritable(true);
                    QRImgFolder.mkdirs();
                }

                // 将二维码保存到本地机器
                String qrPath = String.format(path + "/qr-%s.png", response.getOutTradeNo());
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);

                //将二维码图片上传到ftp服务器
                String qrFileName = String.format(path + "/qr-%s.png", response.getOutTradeNo());
                File targetFile = new File(path, qrFileName);
                log.info("qrPath:" + qrPath);

                String qrUrl = PropertiesUtil.getProperty("ftp.server.http.prefix") + qrFileName;
                resultMap.put("qrUrl", qrUrl);
                return ServerResponse.createBySuccess(resultMap);
            case FAILED:
                log.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");
            case UNKNOWN:
                log.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");
            default:
                log.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");
        }
    }

    // 简单打印应答
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            log.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                log.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            log.info("body:" + response.getBody());
        }
    }

    @Override
    public ServerResponse<String> aliPayCallback(Map<String, String> params) {
        Long orderNo = Long.parseLong(params.get("out_trade_no"));
        String tradeNo = params.get("tradeNo");
        String tradeStatus = params.get("trade_status");
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return ServerResponse.createByErrorMessage("非mmall的订单");
        }

        if (order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {
            //订单已经支付
            return ServerResponse.createByErrorMessage("支付宝重复调用");
        }

        //如果没有支付
        if (Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)) {
            //订单已经付款
            order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));
            order.setStatus(Const.OrderStatusEnum.PAID.getCode());
            orderMapper.updateByPrimaryKeySelective(order);
        }
        PayInfo payInfo = new PayInfo();
        payInfo.setUserId(order.getUserId());
        payInfo.setOrderNo(order.getOrderNo());
        payInfo.setCreateTime(new Date());
        payInfo.setUpdateTime(new Date());
        payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());
        payInfo.setPlatformStatus(tradeStatus);
        payInfoMapper.updateByPrimaryKeySelective(payInfo);
        return ServerResponse.createBySuccess();

    }

    @Override
    public ServerResponse<Boolean> queryOrderPayStatus(Integer userId, Long orderNo) {
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if (order == null) {
            return ServerResponse.createByErrorMessage("订单不存在");
        }

        if (order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {
            //如果已经支付
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }


}
