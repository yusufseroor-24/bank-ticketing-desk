package com.ga.bankdesk.service;

import com.ga.bankdesk.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "application/pdf");

    public String store(MultipartFile file){
        validate(file);

        try{
            Path uploadPath = Paths.get(uploadDir);
            if(!Files.exists(uploadPath)){
                Files.createDirectories(uploadPath);
            }

            //generate file name using random UUID and file name
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName); //adds the path and the file name together
            file.transferTo(filePath);

            return fileName;
        } catch(IOException e){
            throw new RuntimeException("Could not store file", e);
        }
    }

    public List<String> storeAll(List<MultipartFile> files){
        return files.stream()
                .map(this::store)
                .toList();
    }

    private void validate(MultipartFile file){
        if(file.isEmpty()){
            throw new BusinessRuleException("File must not be empty");
        }
        if(!ALLOWED_TYPES.contains(file.getContentType())){
            throw new BusinessRuleException("Only PNG, JPEG, and PDF files are allowed");
        }
    }
}
