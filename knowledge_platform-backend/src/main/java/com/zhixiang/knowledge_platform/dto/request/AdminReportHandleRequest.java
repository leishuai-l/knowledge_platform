package com.zhixiang.knowledge_platform.dto.request;

import com.zhixiang.knowledge_platform.enums.ReportResolutionAction;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminReportHandleRequest {

    @NotNull(message = "confirmed is required")
    private Boolean confirmed;

    @NotBlank(message = "comment is required")
    private String comment;

    private ReportResolutionAction documentAction;

    private String actionNote;

    @AssertTrue(message = "documentAction is required when confirming a report")
    public boolean isDocumentActionValid() {
        return Boolean.FALSE.equals(confirmed) || documentAction != null;
    }
}
