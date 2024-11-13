package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Api(tags = "通用接口")
@RestController
@RequestMapping("/admin/common")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file){
        log.info("上传文件信息：{}", file);

        try {
            //获取原始文件名
            String orignalName = file.getOriginalFilename();
            //截取原始文件名后缀，这个方法的截取包含.
            String suffix = orignalName.substring(orignalName.lastIndexOf("."));
            //获取随机UUID并拼接原始后缀
            String newFileName = UUID.randomUUID().toString() + suffix;

            String fileUrl = aliOssUtil.upload(file.getBytes(), newFileName);
            return Result.success(fileUrl);

        } catch (IOException e) {
            log.error(MessageConstant.UPLOAD_FAILED, e);
        }

        return Result.error(MessageConstant.UPLOAD_FAILED);
    }


}
