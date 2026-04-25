package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomatedReviewService {

    private final DocumentRepository documentRepository;

    public Map<String, Object> performPreliminaryReview(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        // 1. Format Check
        boolean formatValid = checkFormat(file);
        result.put("formatValid", formatValid);
        if (!formatValid) {
            result.put("message", "Invalid file format");
            result.put("passed", false); // Explicitly set passed to false
            return result;
        }

        // 2. Content Compliance
        boolean contentCompliant = checkContentCompliance(file);
        result.put("contentCompliant", contentCompliant);
        
        // 3. Similarity Analysis
        double similarityScore = calculateSimilarity(file);
        result.put("similarityScore", similarityScore);

        boolean passed = formatValid && contentCompliant && similarityScore < 0.8;
        result.put("passed", passed);
        
        if (!passed) {
             if (!contentCompliant) {
                 result.put("message", "Content compliance check failed");
             } else if (similarityScore >= 0.8) {
                 result.put("message", "Document is too similar to existing content");
             }
        }
        
        return result;
    }

    private boolean checkFormat(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (contentType.equals("application/pdf") || 
                                       contentType.equals("application/msword") ||
                                       contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                                       contentType.startsWith("image/"));
    }

    private boolean checkContentCompliance(MultipartFile file) {
        // Simple keyword check on filename
        String filename = file.getOriginalFilename();
        if (filename != null) {
             String[] sensitiveWords = {"赌博", "色情", "暴力", "违禁", "非法", "代考", "枪手"};
             for (String word : sensitiveWords) {
                 if (filename.contains(word)) {
                     return false;
                 }
             }
        }
        return true; 
    }

    private double calculateSimilarity(MultipartFile file) {
        try {
            String md5 = DigestUtils.md5DigestAsHex(file.getInputStream());
            List<Document> existingDocs = documentRepository.findByMd5(md5);
            
            // Check if any non-deleted document has the same MD5
            boolean exists = existingDocs.stream()
                .anyMatch(d -> !d.getStatus().name().equals("DELETED"));
                
            return exists ? 1.0 : 0.0;
        } catch (IOException e) {
            log.error("Failed to calculate MD5", e);
            return 0.0; // Assume unique on error
        }
    }
}
