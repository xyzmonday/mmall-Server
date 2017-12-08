package com.yff.controller;

import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.yff.common.Const;
import com.yff.common.ResponseCode;
import com.yff.common.ServerResponse;
import com.yff.entity.Product;
import com.yff.entity.User;
import com.yff.service.IFileService;
import com.yff.service.IProductService;
import com.yff.service.IUserService;
import com.yff.util.PropertiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.InputStream;
import java.util.Map;

@Controller
@RequestMapping("/manage/product")
public class ProductManageController {

    @Autowired
    private IUserService iUserService;
    @Autowired
    private IProductService iProductService;
    @Autowired
    private IFileService iFileService;

    @GetMapping("/save")
    @ResponseBody
    public ServerResponse<String> productSave(HttpSession session,Product product) {
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

        return iProductService.saveOrUpdateProduct(product);
    }

    @GetMapping("/setSaleStatus")
    @ResponseBody
    public ServerResponse<String> setSaleStatus(HttpSession session, Integer productId, Integer status) {
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
        return iProductService.setSaleStatus(productId, status);
    }

    @RequestMapping("/detail")
    @ResponseBody
    public ServerResponse getDetail(HttpSession session, Integer productId) {
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
        return iProductService.manageProductDetail(productId);
    }

    @GetMapping("/list")
    @ResponseBody
    public ServerResponse<PageInfo> getProductList(HttpSession session, @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
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
        return iProductService.getProductList(pageNum, pageSize);
    }


    @GetMapping("/search")
    @ResponseBody
    public ServerResponse<PageInfo> searchProduct(HttpSession session,
                                                  @RequestParam(value = "productName", required = false) String productName,
                                                  @RequestParam(value = "productId", required = false) Integer productId,
                                                  @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                  @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
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
        return iProductService.searchProduct(productName, productId, pageNum, pageSize);
    }

    @PostMapping("/upload")
    @ResponseBody
    public ServerResponse<Map<String, String>> upload(HttpSession session, @RequestParam(value = "uploadFile", required = false) MultipartFile file, HttpServletRequest request) {
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
        if (file == null) {
            return ServerResponse.createByErrorMessage("文件为空");
        }
        //这里获取的路径为E://spring-workspace//mmall//src//main//webapp//upload
        String realPath = request.getSession().getServletContext().getRealPath("upload");
        String targetFileName = iFileService.upload(file, realPath);
        String url = PropertiesUtil.getProperty("ftp.server.http.prefix") + "upload/" + targetFileName;

        Map<String, String> fileMap = Maps.newHashMap();
        fileMap.put("uri", targetFileName);
        fileMap.put("url", url);
        return ServerResponse.createBySuccess(fileMap);
    }

    @PostMapping("/richtextImgUpload")
    @ResponseBody
    public Map<String, Object> richtextImgUpload(HttpSession session, @RequestParam(value = "uploadFile", required = false) MultipartFile file,
                                                 HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> resultMap = Maps.newHashMap();
        //1. 是否登陆
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            resultMap.put("success",false);
            resultMap.put("msg","请登录管理员");
            return resultMap;
        }
        //2. 校验是否是管理员
        ServerResponse checkAdminRole = iUserService.checkAdminRole(user);
        if (!checkAdminRole.isSuccess()) {
            resultMap.put("success",false);
            resultMap.put("msg","请登录管理员");
            return resultMap;
        }
        if (file == null) {
            resultMap.put("success",false);
            resultMap.put("msg","文件为空");
            return resultMap;
        }
        //富文本中对于返回值有自己的要求,我们使用是simditor所以按照simditor的要求进行返回
//        {
//            "success": true/false,
//                "msg": "error message", # optional
//            "file_path": "[real file path]"
//        }
        //这里获取的路径为E://spring-workspace//mmall//src//main//webapp//upload
        String realPath = request.getSession().getServletContext().getRealPath("upload");
        String targetFileName = iFileService.upload(file, realPath);
        String url = PropertiesUtil.getProperty("ftp.server.http.prefix") + "upload/" + targetFileName;
        resultMap.put("success",true);
        resultMap.put("msg","上传成功");
        resultMap.put("file_path",url);
        response.addHeader("Access-Control-Allow-Headers","X-File-Name");
        return resultMap;
    }
}
