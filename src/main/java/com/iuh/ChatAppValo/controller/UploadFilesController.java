package com.iuh.ChatAppValo.controller;

import com.iuh.ChatAppValo.entity.Account;
import com.iuh.ChatAppValo.jwt.response.MessageResponse;
import com.iuh.ChatAppValo.services.AmazonS3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/upload")
public class UploadFilesController {
    @Autowired
    private AmazonS3Service s3Service;

    /**
     * tải các file được chọn lên s3 rồi trả về danh sach url
     * @param account
     * @param files
     * @return
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> upload(@AuthenticationPrincipal Account account, @RequestParam List<MultipartFile> files){
        List<String> filesUrl = new ArrayList<String>();
        if (files.isEmpty())
            return ResponseEntity.badRequest().body(new MessageResponse("No file is selected"));
        for (MultipartFile file:files) {
            String fileUrl = s3Service.uploadFile(file);
            filesUrl.add(fileUrl);
        }
        return ResponseEntity.ok(filesUrl);
    }
}
