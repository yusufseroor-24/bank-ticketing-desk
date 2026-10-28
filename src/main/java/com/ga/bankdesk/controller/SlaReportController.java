package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.SlaReportResponse;
import com.ga.bankdesk.model.SlaReport;
import com.ga.bankdesk.security.AppUserDetails;
import com.ga.bankdesk.service.SlaReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/sla-reports")
@PreAuthorize("hasRole('ADMIN')")
public class SlaReportController {

    private final SlaReportService reportService;

    //ex: /api/admin/sla-reports?month=2026-09
    @PostMapping
    public ResponseEntity<SlaReportResponse> generate(@AuthenticationPrincipal AppUserDetails userDetails, @RequestParam String month) {
        SlaReport report = reportService.generate(reportService.parseMonth(month), userDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(SlaReportResponse.from(report));
    }

    @GetMapping
    public List<SlaReportResponse> list() {
        return reportService.listReports().stream().map(SlaReportResponse::from).toList();
    }

    @GetMapping("/latest/download")
    public ResponseEntity<Resource> downloadLatest() {
        Path file = reportService.latestReportFile();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new FileSystemResource(file));
    }
}
