package com.crm.service;

import com.crm.vo.FileUrlVO;
import org.springframework.web.multipart.MultipartFile;

public interface CommonService {
    /**
     * 文件上传
     * @param multipartFile
     * @return
     */
    FileUrlVO upload(MultipartFile  multipartFile);
}
