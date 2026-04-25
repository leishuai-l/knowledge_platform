package com.zhixiang.knowledge_platform.dto.request;

import com.zhixiang.knowledge_platform.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReportSubmitRequest {

    @NotNull(message = "文档ID不能为空")
    private Long documentId;

    @NotNull(message = "举报类型不能为空")
    private ReportType reportType;

    @NotBlank(message = "详细描述不能为空")
    @Size(min = 20, max = 500, message = "描述长度必须在20-500字之间")
    private String description;

    private String contactInfo;

    private List<String> evidenceLinks;

    private List<String> imageEvidences;

    private List<String> fileEvidences;
}
