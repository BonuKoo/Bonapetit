package com.eatmate.global.controller;

import com.eatmate.global.service.FileService.FileService;
import com.eatmate.global.service.FileStore.FileStoreOfAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.MalformedURLException;

@RequestMapping("/file/account")
@Controller
@Slf4j
public class UploadFileAccountController {

    private final FileService fileService;
    private final FileStoreOfAccount fileStore;

    public UploadFileAccountController(@Qualifier("fileServiceOfAccountImpl") FileService fileService, FileStoreOfAccount fileStore) {
        this.fileService = fileService;
        this.fileStore = fileStore;
    }

    @ResponseBody
    @GetMapping("/images/{filename}")
    public Resource downloadImage(@PathVariable String filename) throws MalformedURLException {
        return new UrlResource("file:" + fileStore.getFullPath(filename));
    }

    @GetMapping("/attach/{id}")
    private ResponseEntity<Resource> downloadAttach(@PathVariable Long id) throws MalformedURLException {
        return fileService.downloadAttach(id);
    }
}
