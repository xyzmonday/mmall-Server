package com.yff.service.impl;

import com.google.common.base.Splitter;
import com.yff.common.Const;
import com.yff.common.ResponseCode;
import com.yff.common.ServerResponse;
import com.yff.dao.CartMapper;
import com.yff.dao.ProductMapper;
import com.yff.entity.Cart;
import com.yff.entity.Product;
import com.yff.service.ICartService;
import com.yff.util.BigDecimalUtil;
import com.yff.vo.CartProductVo;
import com.yff.vo.CartVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service("iCartService")
public class CartServiceImpl implements ICartService {

    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;

    @Override
    public ServerResponse<CartVo> add(Integer userId, Integer count, Integer productId) {
        if (productId == null || count == 0) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),
                    ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        //1.通过userId，productId查询是否存在该购物车
        Cart cart = cartMapper.selectCart(userId, productId);
        if (cart == null) {
            //2.如果没有，那么新建一个购物车，将该产品放入该购物车

        } else {  cart = new Cart();
            cart.setQuantity(count);
            cart.setProductId(productId);
            cart.setChecked(Const.Cart.CHECKED);
            cart.setUserId(userId);
            cart.setCreateTime(new Date());
            cart.setUpdateTime(new Date());
            cartMapper.insertSelective(cart);
            //3.如果已经存在
            cart.setQuantity(cart.getQuantity() + count);
            cartMapper.updateByPrimaryKeySelective(cart);
        }
        return this.list(userId);
    }

    @Override
    public ServerResponse<CartVo> update(Integer userId, Integer productId, Integer count) {
        if (productId == null || count == 0) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),
                    ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Cart cart = cartMapper.selectCart(userId, productId);
        if (cart != null) {
            cart.setQuantity(count);
            cartMapper.updateByPrimaryKeySelective(cart);
        }
        return this.list(userId);
    }

    @Override
    public ServerResponse<CartVo> delete(Integer userId, String productIds) {
        if (StringUtils.isBlank(productIds)) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),
                    ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        List<String> productIdList = Splitter.on(",").splitToList(productIds);
        int result = cartMapper.deleteProductByIds(userId, productIdList);
        if (result <= 0) {
            return ServerResponse.createByErrorMessage("删除失败");
        }
        return this.list(userId);
    }

    @Override
    public ServerResponse<CartVo> selectOrUnSelect(Integer userId, Integer checked, Integer productId) {
        cartMapper.selectOrUnSelect(userId, checked, productId);
        return this.list(userId);
    }

    @Override
    public ServerResponse<Integer> getCarProductCount(Integer userId) {
        if (userId == null) {
            return ServerResponse.createBySuccess(0);
        }
        return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
    }

    @Override
    public ServerResponse<CartVo> list(Integer userId) {
        CartVo cartVo = this.getCartVoLimit(userId);
        return ServerResponse.createBySuccess(cartVo);
    }


    private CartVo getCartVoLimit(Integer useId) {
        CartVo cartVo = new CartVo();
        List<Cart> carts = cartMapper.selectCartByUserId(useId);
        List<CartProductVo> cartProductVoList = new ArrayList<>();
        BigDecimal cartTotalPrice = new BigDecimal("0");
        if (carts != null) {
            for (Cart cart : carts) {
                //组装cart字段给CartProductVo
                CartProductVo cartProductVo = new CartProductVo();
                cartProductVo.setId(cart.getId());
                cartProductVo.setUserId(cart.getUserId());
                cartProductVo.setProductId(cart.getProductId());

                //组装Product字段给CartProductVo
                Product product = productMapper.selectByPrimaryKey(cart.getProductId());
                if (product != null) {
                    cartProductVo.setProductMainImage(product.getMainImage());
                    cartProductVo.setProductName(product.getName());
                    cartProductVo.setProductSubtitle(product.getSubtitle());
                    cartProductVo.setProductStatus(product.getStatus());
                    cartProductVo.setProductPrice(product.getPrice());
                    cartProductVo.setProductStock(product.getStock());
                    //判断库存
                    int buyLimitCount = 0;
                    if (product.getStock() >= cart.getQuantity()) {
                        //库存充足的时候
                        buyLimitCount = cart.getQuantity();
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
                    } else {
                        buyLimitCount = product.getStock();
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
                        //购物车中更新有效库存
                        Cart cartForQuantityUpdate = new Cart();
                        cartForQuantityUpdate.setId(cart.getId());
                        cartForQuantityUpdate.setQuantity(buyLimitCount);
                        cartMapper.updateByPrimaryKeySelective(cartForQuantityUpdate);
                    }

                    //设置cartProductVo
                    cartProductVo.setQuantity(buyLimitCount);
                    //计算该购物车总价
                    cartProductVo.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(), cartProductVo.getQuantity()));
                    //购物车全选
                    cartProductVo.setProductChecked(cart.getChecked());
                }

                //如果该购物车被选中，那么该购物车里面的产品将被计算到购物车总价中
                if (cart.getChecked() == Const.Cart.CHECKED) {
                    cartTotalPrice = BigDecimalUtil.add(cartTotalPrice.doubleValue(),cartProductVo.getProductTotalPrice().doubleValue());
                }
                cartProductVoList.add(cartProductVo);
            }
        }

        //组装CartVo
        cartVo.setCartTotalPrice(cartTotalPrice);
        cartVo.setCartProductVoList(cartProductVoList);
        cartVo.setAllChecked(getAllCheckedStatus(useId));
        return cartVo;
    }

    /**
     * 计算该用户下的所有购物车的选中状态
     *
     * @param userId
     * @return
     */
    private boolean getAllCheckedStatus(Integer userId) {
        if (userId == null) {
            return false;
        }
        return cartMapper.selectCartProductCheckedStatusByUserId(userId) == 0;

    }
}
