package com.ga.bankdesk.repository;

import com.ga.bankdesk.enums.ReportStatus;
import com.ga.bankdesk.model.SlaReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SlaReportRepository extends JpaRepository<SlaReport, Long> {

    Optional<SlaReport> findByReportPeriod(String reportPeriod);
    Optional<SlaReport> findFirstByStatusOrderByReportPeriodDesc(ReportStatus status);
    List<SlaReport> findAllByOrderByReportPeriodDesc();

    //Atomic claim (for month)
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO sla_reports (report_period, status, generated_by, generated_at) " +
            "VALUES (:period, 'GENERATING', CAST(:userId AS BIGINT), now()) " +
            "ON CONFLICT (report_period) DO UPDATE " +
            "SET status = 'GENERATING', generated_by = CAST(:userId AS BIGINT), generated_at = now() " +
            "WHERE sla_reports.status = 'FAILED'",
            nativeQuery = true)
    int claim(@Param("period") String period, @Param("userId") Long userId);
}
