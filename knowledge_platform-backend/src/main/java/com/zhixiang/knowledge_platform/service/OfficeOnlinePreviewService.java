package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfficeOnlinePreviewService {

    @Value("${zhixiang.file.preview.office-online.enabled:false}")
    private boolean enabled;

    @Value("${zhixiang.file.preview.office-online.provider:custom}")
    private String provider;

    @Value("${zhixiang.file.preview.office-online.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    @Value("${zhixiang.file.preview.office-online.embed-url-template:}")
    private String embedUrlTemplate;

    private final JwtUtil jwtUtil;

    public boolean isEnabled() {
        return enabled;
    }

    public String getProvider() {
        return provider;
    }

    public String generatePreviewToken(Long documentId, Long userId, String role) {
        return jwtUtil.generateOfficePreviewToken(documentId, userId, role);
    }

    public String buildPreviewSourceUrl(Long documentId, String previewToken) {
        String baseUrl = trimTrailingSlash(publicBaseUrl);
        String encodedToken = UriUtils.encode(previewToken, StandardCharsets.UTF_8);
        return baseUrl + "/api/documents/" + documentId + "/preview-source?previewToken=" + encodedToken;
    }

    public String buildOnlinePreviewUrl(String sourceUrl) {
        if (!enabled || sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }

        String encodedSourceUrl = UriUtils.encode(sourceUrl, StandardCharsets.UTF_8);
        if (embedUrlTemplate != null && !embedUrlTemplate.isBlank()) {
            return embedUrlTemplate.replace("{sourceUrl}", encodedSourceUrl);
        }

        return null;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
