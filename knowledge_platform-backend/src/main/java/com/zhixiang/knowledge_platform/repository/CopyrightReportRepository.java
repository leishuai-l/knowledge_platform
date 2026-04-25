package com.zhixiang.knowledge_platform.repository;

import com.zhixiang.knowledge_platform.entity.CopyrightReport;
import com.zhixiang.knowledge_platform.enums.ReportStatus;
import com.zhixiang.knowledge_platform.enums.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 侵权举报 Repository
 */
@Repository
public interface CopyrightReportRepository extends JpaRepository<CopyrightReport, Long> {

    /**
     * 根据文档ID查找举报记录
     */
    List<CopyrightReport> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    /**
     * 根据举报人ID查找举报记录
     */
    Page<CopyrightReport> findByReporterIdOrderByCreatedAtDesc(Long reporterId, Pageable pageable);

    /**
     * 根据状态查找举报记录
     */
    Page<CopyrightReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    /**
     * 根据举报类型查找举报记录
     */
    Page<CopyrightReport> findByReportTypeOrderByCreatedAtDesc(ReportType reportType, Pageable pageable);

    Page<CopyrightReport> findByStatusInOrderByCreatedAtDesc(List<ReportStatus> statuses, Pageable pageable);

    Page<CopyrightReport> findByStatusInAndReportTypeOrderByCreatedAtDesc(List<ReportStatus> statuses, ReportType reportType, Pageable pageable);

    Page<CopyrightReport> findByStatusInAndDocumentIdOrderByCreatedAtDesc(List<ReportStatus> statuses, Long documentId, Pageable pageable);

    Page<CopyrightReport> findByStatusInAndReportTypeAndDocumentIdOrderByCreatedAtDesc(List<ReportStatus> statuses, ReportType reportType, Long documentId, Pageable pageable);

    Page<CopyrightReport> findByReportTypeAndDocumentIdOrderByCreatedAtDesc(ReportType reportType, Long documentId, Pageable pageable);

    Page<CopyrightReport> findByDocumentIdOrderByCreatedAtDesc(Long documentId, Pageable pageable);

    /**
     * 查找待处理的举报数量
     */
    @Query("SELECT COUNT(r) FROM CopyrightReport r WHERE r.status = :status")
    long countByStatus(@Param("status") ReportStatus status);

    /**
     * 查找处理人的举报记录
     */
    Page<CopyrightReport> findByHandlerIdOrderByCreatedAtDesc(Long handlerId, Pageable pageable);

    /**
     * 检查文档是否已被举报
     */
    @Query("SELECT COUNT(r) > 0 FROM CopyrightReport r WHERE r.documentId = :documentId AND r.status IN ('PENDING', 'INVESTIGATING')")
    boolean existsPendingReportForDocument(@Param("documentId") Long documentId);

    /**
     * 统计文档被举报次数
     */
    @Query("SELECT COUNT(r) FROM CopyrightReport r WHERE r.documentId = :documentId")
    long countReportsByDocument(@Param("documentId") Long documentId);
}
