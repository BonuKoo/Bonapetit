package com.eatmate.global.service.FileService;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.net.MalformedURLException;

public interface FileService {
    ResponseEntity<Resource> downloadAttach(Long id) throws MalformedURLException;

}