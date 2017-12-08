package com.yff.service.impl;

import com.google.common.collect.Lists;
import com.yff.service.IFileService;
import com.yff.util.FTPUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service("iFileService")
public class FileServiceImpl implements IFileService {

    public String upload(MultipartFile file, String path) {
        //1.获取文件名
        String fileName = file.getOriginalFilename();
        //2.获取扩展名
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
        String uploadFileName = UUID.randomUUID().toString() + "." + ext;
        System.out.println("开始上传文件，上传的文件名:"+uploadFileName);
        System.out.println("上传的路径为:"+path);

        File fileDir = new File(path);
        if(!fileDir.exists()) {
            fileDir.setWritable(true);
            fileDir.mkdirs();
        }

        File targetFile = new File(path,uploadFileName);
        try {
            file.transferTo(targetFile);
            FTPUtil.uploadFile(Lists.newArrayList(targetFile));
            //已经上传到ftp服务器上
            targetFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("上传文件出错:" + e.getMessage());
        }
        return targetFile.getName();
    }
}
