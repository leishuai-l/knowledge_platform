package com.zhixiang.knowledge_platform.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReportEvidenceDTO {
    private List<String> links;
    private List<String> images;
    private List<String> files;
}
