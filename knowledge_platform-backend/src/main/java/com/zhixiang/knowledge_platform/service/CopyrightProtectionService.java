package com.zhixiang.knowledge_platform.service;

import com.zhixiang.knowledge_platform.dto.request.AdminReportHandleRequest;
import com.zhixiang.knowledge_platform.entity.CopyrightReport;
import com.zhixiang.knowledge_platform.entity.Document;
import com.zhixiang.knowledge_platform.entity.User;
import com.zhixiang.knowledge_platform.enums.ReportResolutionAction;
import com.zhixiang.knowledge_platform.enums.ReportStatus;
import com.zhixiang.knowledge_platform.enums.ReportType;
import com.zhixiang.knowledge_platform.repository.CopyrightReportRepository;
import com.zhixiang.knowledge_platform.repository.DocumentRepository;
import com.zhixiang.knowledge_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识产权保护服务
 * 处理侵权举报和版权保护
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CopyrightProtectionService {

    private final CopyrightReportRepository reportRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    private static final String DOCUMENT_NOT_FOUND = "文档不存在";

    /**
     * 提交侵权举报
     */
    @Transactional
    public CopyrightReport submitReport(Long reporterId, ReportRequest request) {
        log.info("用户 {} 举报文档 {}", reporterId, request.getDocumentId());

        // 1. 验证文档
        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException(DOCUMENT_NOT_FOUND));

        // 2. 验证举报人不是文档上传者
        if (document.getUploaderId().equals(reporterId)) {
            throw new IllegalArgumentException("不能举报自己上传的文档");
        }

        // 3. 检查是否已举报过该文档
        if (reportRepository.existsPendingReportForDocument(request.getDocumentId())) {
            throw new IllegalStateException("该文档已有待处理的举报");
        }

        // 4. 创建举报记录
        CopyrightReport report = new CopyrightReport();
        report.setDocumentId(request.getDocumentId());
        report.setReporterId(reporterId);
        report.setReportType(request.getReportType());
        report.setDescription(request.getDescription());
        report.setEvidenceUrls(request.getEvidenceUrls());
        report.setContactInfo(request.getContactInfo());
        report.setStatus(ReportStatus.PENDING);

        reportRepository.save(report);

        log.info("举报提交成功，举报ID: {}", report.getId());
        return report;
    }

    /**
     * 获取用户的举报记录
     */
    public Page<CopyrightReport> getUserReports(Long reporterId, Pageable pageable) {
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId, pageable);
    }

    public CopyrightReport getReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("举报记录不存在"));
    }

    /**
     * 获取全部举报记录（管理员）
     */
    public Page<CopyrightReport> getAllReports(Pageable pageable) {
        return reportRepository.findAll(pageable);
    }

    /**
     * 获取管理员举报列表
     */
    public Page<CopyrightReport> getAdminReports(List<ReportStatus> statuses,
                                                 ReportType reportType,
                                                 Long documentId,
                                                 Pageable pageable) {
        if (statuses != null && !statuses.isEmpty()) {
            if (reportType != null && documentId != null) {
                return reportRepository.findByStatusInAndReportTypeAndDocumentIdOrderByCreatedAtDesc(
                        statuses, reportType, documentId, pageable);
            }
            if (reportType != null) {
                return reportRepository.findByStatusInAndReportTypeOrderByCreatedAtDesc(
                        statuses, reportType, pageable);
            }
            if (documentId != null) {
                return reportRepository.findByStatusInAndDocumentIdOrderByCreatedAtDesc(
                        statuses, documentId, pageable);
            }
            return reportRepository.findByStatusInOrderByCreatedAtDesc(statuses, pageable);
        }

        if (reportType != null && documentId != null) {
            return reportRepository.findByReportTypeAndDocumentIdOrderByCreatedAtDesc(
                    reportType, documentId, pageable);
        }
        if (reportType != null) {
            return reportRepository.findByReportTypeOrderByCreatedAtDesc(reportType, pageable);
        }
        if (documentId != null) {
            return reportRepository.findByDocumentIdOrderByCreatedAtDesc(documentId, pageable);
        }

        return reportRepository.findAll(pageable);
    }

    /**
     * 获取待处理的举报列表（管理员）
     */
    public Page<CopyrightReport> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(
                ReportStatus.PENDING, pageable);
    }

    @Transactional
    public CopyrightReport markInvestigating(Long reportId, Long handlerId, String comment) {
        log.info("管理员 {} 将举报 {} 标记为调查中", handlerId, reportId);

        User handler = userRepository.findById(handlerId)
                .orElseThrow(() -> new IllegalArgumentException("处理人不存在"));
        if (!handler.isAdmin()) {
            throw new SecurityException("只有管理员才能处理举报");
        }

        CopyrightReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("举报记录不存在"));

        if (report.isHandled()) {
            throw new IllegalStateException("该举报已被处理");
        }
        if (report.isInvestigating()) {
            throw new IllegalStateException("该举报已处于调查中");
        }

        report.markInvestigating(handlerId, comment);
        reportRepository.save(report);
        return report;
    }

    /**
     * 处理举报（管理员）
     */
    @Transactional
    public CopyrightReport handleReport(Long reportId, Long handlerId,
                                        AdminReportHandleRequest request) {
        log.info("管理员 {} 处理举报 {}", handlerId, reportId);

        User handler = userRepository.findById(handlerId)
                .orElseThrow(() -> new IllegalArgumentException("处理人不存在"));
        if (!handler.isAdmin()) {
            throw new SecurityException("只有管理员才能处理举报");
        }

        CopyrightReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("举报记录不存在"));

        if (!report.canBeHandled()) {
            throw new IllegalStateException("该举报已被处理");
        }

        boolean confirmed = Boolean.TRUE.equals(request.getConfirmed());
        Document document = null;
        if (confirmed) {
            document = documentRepository.findById(report.getDocumentId())
                    .orElseThrow(() -> new IllegalArgumentException(DOCUMENT_NOT_FOUND));
        }

        ReportStatus status = confirmed ? ReportStatus.CONFIRMED : ReportStatus.REJECTED;
        String actionTaken = buildActionTaken(report, request);
        report.handle(handlerId, request.getComment(), actionTaken, status);
        reportRepository.save(report);

        if (document != null) {
            handleConfirmedReport(document, report, request.getDocumentAction());
        }

        log.info("举报处理完成，结果: {}", status);
        return report;
    }

    /**
     * 处理确认的举报
     */
    private void handleConfirmedReport(Document document, CopyrightReport report,
                                       ReportResolutionAction documentAction) {
        ReportResolutionAction finalAction = documentAction != null
                ? documentAction
                : getRecommendedAction(report.getReportType());

        switch (finalAction) {
            case DELETE_DOCUMENT:
                document.delete(buildDocumentReason(report, true));
                log.info("文档 {} 因举报确认被删除", document.getId());
                break;
            case TAKE_DOWN_DOCUMENT:
                document.reject(buildDocumentReason(report, false));
                log.info("文档 {} 因举报确认被下架", document.getId());
                break;
            case NO_DOCUMENT_CHANGE:
                log.info("举报 {} 已确认，但文档 {} 保持原状态", report.getId(), document.getId());
                break;
            default:
                throw new IllegalStateException("未知的文档处置动作");
        }

        if (finalAction != ReportResolutionAction.NO_DOCUMENT_CHANGE) {
            documentRepository.save(document);
        }
    }

    private ReportResolutionAction getRecommendedAction(ReportType reportType) {
        return switch (reportType) {
            case COPYRIGHT_INFRINGEMENT, PLAGIARISM, ILLEGAL_CONTENT -> ReportResolutionAction.DELETE_DOCUMENT;
            case INAPPROPRIATE_CONTENT, SENSITIVE_CONTENT, FALSE_INFORMATION, SPAM, OTHER -> ReportResolutionAction.TAKE_DOWN_DOCUMENT;
        };
    }

    private String buildActionTaken(CopyrightReport report, AdminReportHandleRequest request) {
        if (!Boolean.TRUE.equals(request.getConfirmed())) {
            return request.getActionNote();
        }

        ReportResolutionAction action = request.getDocumentAction() != null
                ? request.getDocumentAction()
                : getRecommendedAction(report.getReportType());
        String base = action.getDescription();
        String note = request.getActionNote();
        return note == null || note.isBlank() ? base : base + "：" + note.trim();
    }

    private String buildDocumentReason(CopyrightReport report, boolean deleted) {
        String action = deleted ? "删除" : "下架";
        return action + "原因：" + report.getReportType().getDescription() + " - " + report.getDescription();
    }

    /**
     * 获取文档的举报记录
     */
    public java.util.List<CopyrightReport> getDocumentReports(Long documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new IllegalArgumentException(DOCUMENT_NOT_FOUND);
        }

        return reportRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
    }

    /**
     * 获取举报统计
     */
    public ReportStatistics getReportStatistics() {
        long pending = reportRepository.countByStatus(ReportStatus.PENDING);
        long investigating = reportRepository.countByStatus(ReportStatus.INVESTIGATING);
        long confirmed = reportRepository.countByStatus(ReportStatus.CONFIRMED);
        long rejected = reportRepository.countByStatus(ReportStatus.REJECTED);

        return new ReportStatistics(pending, investigating, confirmed, rejected);
    }

    /**
     * 举报请求
     */
    public static class ReportRequest {
        private Long documentId;
        private ReportType reportType;
        private String description;
        private String evidenceUrls;
        private String contactInfo;

        public Long getDocumentId() {
            return documentId;
        }

        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }

        public ReportType getReportType() {
            return reportType;
        }

        public void setReportType(ReportType reportType) {
            this.reportType = reportType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getEvidenceUrls() {
            return evidenceUrls;
        }

        public void setEvidenceUrls(String evidenceUrls) {
            this.evidenceUrls = evidenceUrls;
        }

        public String getContactInfo() {
            return contactInfo;
        }

        public void setContactInfo(String contactInfo) {
            this.contactInfo = contactInfo;
        }
    }

    /**
     * 举报统计
     */
    public static class ReportStatistics {
        private final long pending;
        private final long investigating;
        private final long confirmed;
        private final long rejected;

        public ReportStatistics(long pending, long investigating,
                                long confirmed, long rejected) {
            this.pending = pending;
            this.investigating = investigating;
            this.confirmed = confirmed;
            this.rejected = rejected;
        }

        public long getPending() {
            return pending;
        }

        public long getInvestigating() {
            return investigating;
        }

        public long getConfirmed() {
            return confirmed;
        }

        public long getRejected() {
            return rejected;
        }
    }
}
