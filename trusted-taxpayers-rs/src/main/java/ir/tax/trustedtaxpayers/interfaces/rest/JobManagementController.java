package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.biz.service.JobManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * کنترلر مدیریت Jobها برای ادمین
 * امکان اجرای دستی Jobها و تنظیم زمان اجرای آنها
 */
@RestController
@RequestMapping("/api/admin/jobs")
@Tag(name = "Job Management", description = "مدیریت Jobها توسط ادمین")
@PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
public class JobManagementController {

    private final JobManagementService jobManagementService;

    public JobManagementController(JobManagementService jobManagementService) {
        this.jobManagementService = jobManagementService;
    }

    @Operation(summary = "دریافت لیست تمام Jobها")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllJobs() {
        return ResponseEntity.ok(jobManagementService.getAllJobs());
    }

    @Operation(summary = "دریافت اطلاعات یک Job")
    @GetMapping("/{jobName}")
    public ResponseEntity<Map<String, Object>> getJob(@PathVariable String jobName) {
        return jobManagementService.getJob(jobName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "اجرای دستی یک Job")
    @PostMapping("/{jobName}/execute")
    public ResponseEntity<Map<String, Object>> executeJob(@PathVariable String jobName) {
        try {
            jobManagementService.executeJob(jobName);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Job با موفقیت اجرا شد",
                    "jobName", jobName
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "خطا در اجرای Job: " + e.getMessage(),
                    "jobName", jobName
            ));
        }
    }

    @Operation(summary = "به‌روزرسانی تنظیمات زمان‌بندی Job")
    @PutMapping("/{jobName}/schedule")
    public ResponseEntity<Map<String, Object>> updateJobSchedule(
            @PathVariable String jobName,
            @RequestBody Map<String, String> scheduleConfig) {
        try {
            jobManagementService.updateJobSchedule(jobName, scheduleConfig);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "تنظیمات Job با موفقیت به‌روزرسانی شد",
                    "jobName", jobName
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "خطا در به‌روزرسانی تنظیمات: " + e.getMessage(),
                    "jobName", jobName
            ));
        }
    }

    @Operation(summary = "غیرفعال/فعال کردن یک Job")
    @PutMapping("/{jobName}/toggle")
    public ResponseEntity<Map<String, Object>> toggleJob(
            @PathVariable String jobName,
            @RequestBody Map<String, Boolean> request) {
        try {
            boolean enabled = request.getOrDefault("enabled", true);
            jobManagementService.toggleJob(jobName, enabled);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", enabled ? "Job فعال شد" : "Job غیرفعال شد",
                    "jobName", jobName,
                    "enabled", enabled
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "خطا در تغییر وضعیت Job: " + e.getMessage(),
                    "jobName", jobName
            ));
        }
    }
}

