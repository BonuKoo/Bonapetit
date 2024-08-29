package com.eatmate.global.service.FileService.impl;

import com.eatmate.dao.repository.teampost.TeamPostRepository;
import com.eatmate.domain.entity.post.TeamPost;
import com.eatmate.global.domain.UploadFileOfTeamPost;
import com.eatmate.global.service.FileService.FileService;
import com.eatmate.global.service.FileStore.FileStoreOfAccount;
import com.eatmate.global.service.FileStore.FileStoreOfTeamPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;


@Slf4j
@Service("fileServiceOfTeamPostImpl")
@RequiredArgsConstructor
public class FileServiceOfTeamPostImpl implements FileService {

    private final TeamPostRepository teamPostRepository;
    private final FileStoreOfTeamPost fileStore;

    @Override
    public ResponseEntity<Resource> downloadAttach(Long id) throws MalformedURLException {

        //파일 갖고 있냐 없냐 검증
        UploadFileOfTeamPost uploadFile = teamPostRepository.findById(id)
                .map(TeamPost::getFiles)
                .flatMap(files -> files.stream().findFirst())
                .orElse(null);

        if (uploadFile == null){
            return ResponseEntity.notFound().build();
        }

        String storeFileName = uploadFile.getStoreFileName();
        String uploadFileName = uploadFile.getUploadFileName();

        UrlResource resource = new UrlResource("file:" + fileStore.getFullPath(storeFileName));
        log.info("uploadFileName={}",uploadFileName);

        String encodedUploadFileName = UriUtils.encode(uploadFileName, StandardCharsets.UTF_8);
        String contentDisposition = "attachment; filename=\"" + encodedUploadFileName + "\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

}