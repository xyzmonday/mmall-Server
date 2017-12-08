package com.yff.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yff.common.ResponseCode;
import com.yff.common.ServerResponse;
import com.yff.dao.ShippingMapper;
import com.yff.entity.Shipping;
import com.yff.service.IShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("iShippingService")
public class ShippingServiceImpl implements IShippingService {

    @Autowired
    private ShippingMapper shippingMapper;

    @Override
    public ServerResponse<Map<String, Object>> add(Integer userId, Shipping shipping) {
        shipping.setUserId(userId);
        shipping.setCreateTime(new Date());
        shipping.setUpdateTime(new Date());
        int result = shippingMapper.insertSelective(shipping);
        if (result > 0) {
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("shippingId", shipping.getId());
            return ServerResponse.createBySuccess("新建地址成功", resultMap);
        }
        return ServerResponse.createByErrorMessage("新建地址失败");
    }

    @Override
    public ServerResponse<String> delete(Integer userId, Integer shippingId) {
        if (shippingId == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        int result = shippingMapper.deleteByShippingIdAndUserId(userId, shippingId);
        if (result <= 0) {
            return ServerResponse.createByErrorMessage("删除地址失败");
        }
        return ServerResponse.createBySuccess("删除地址成功");
    }

    @Override
    public ServerResponse<String> update(Integer userId, Shipping shipping) {
        if (shipping == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        //设置userId作为shipping修改的条件
        shipping.setUserId(userId);
        int result = shippingMapper.updateByShippingIdAndUserId(shipping);
        if (result <= 0) {
            return ServerResponse.createByErrorMessage("更新地址失败");
        }
        return ServerResponse.createBySuccess("更新地址成功");
    }

    @Override
    public ServerResponse<Shipping> select(Integer userId, Integer shippingId) {
        if (shippingId == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Shipping shipping = shippingMapper.selectByShippingIdAndUserId(userId, shippingId);
        if (shipping == null) {
            return ServerResponse.createByErrorMessage("查询地址失败");
        }
        return ServerResponse.createBySuccess("查询地址成功", shipping);
    }

    @Override
    public ServerResponse<PageInfo<Shipping>> list(Integer userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Shipping> shippings = shippingMapper.selectByUserId(userId);
        PageInfo<Shipping> pageInfo = new PageInfo<>(shippings);
        return ServerResponse.createBySuccess("查询成功", pageInfo);
    }
}


