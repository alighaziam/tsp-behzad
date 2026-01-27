package ir.tax.trustedtaxpayers.biz.service;

import ir.tax.trustedtaxpayers.application.service.SettingService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.OutboxMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * سرویس مدیریت Jobها
 * امکان اجرای دستی و تنظیم زمان Jobها
 */
@Service
@Slf4j
public class JobManagementService {

    private final SettingService settingService;
    private final PatternDeferredSubmissionJob patternDeferredSubmissionJob;
    private final Optional<OutboxDispatcher> outboxDispatcher;
    private final OutboxMessageRepository outboxMessageRepository;

    @Autowired
    public JobManagementService(SettingService settingService,
                                PatternDeferredSubmissionJob patternDeferredSubmissionJob,
                                OutboxMessageRepository outboxMessageRepository,
                                Optional<OutboxDispatcher> outboxDispatcher) {
        this.settingService = settingService;
        this.patternDeferredSubmissionJob = patternDeferredSubmissionJob;
        this.outboxMessageRepository = outboxMessageRepository;
        this.outboxDispatcher = outboxDispatcher;
    }

    private final Map<String, Boolean> jobEnabledStatus = new ConcurrentHashMap<>();

    /**
     * دریافت لیست تمام Jobها
     */
    public List<Map<String, Object>> getAllJobs() {
        List<Map<String, Object>> jobs = new ArrayList<>();

        // Pattern Deferred Submission Job
        jobs.add(getJobInfo("patternDeferredSubmission", "ارسال درخواست‌های الگوی معوق",
                "application.pattern.deferred-dispatch.fixed-delay-ms",
                "application.pattern.deferred-dispatch.batch-size"));

        // Outbox Dispatcher Job
        jobs.add(getJobInfo("outboxDispatcher", "ارسال پیام‌های Outbox",
                "application.outbox.dispatcher.fixed-delay-ms",
                null));

        return jobs;
    }

    /**
     * دریافت اطلاعات یک Job
     */
    public Optional<Map<String, Object>> getJob(String jobName) {
        return getAllJobs().stream()
                .filter(job -> jobName.equals(job.get("name")))
                .findFirst();
    }

    /**
     * اجرای دستی یک Job
     */
    public void executeJob(String jobName) {
        log.info("Manual execution of job: {}", jobName);
        switch (jobName) {
            case "patternDeferredSubmission":
                patternDeferredSubmissionJob.dispatchApproved();
                break;
            case "outboxDispatcher":
                outboxDispatcher.ifPresentOrElse(
                    OutboxDispatcher::dispatch,
                    () -> {
                        throw new IllegalStateException("OutboxDispatcher is not available. Kafka may be disabled.");
                    }
                );
                break;
            default:
                throw new IllegalArgumentException("Job نامعتبر: " + jobName);
        }
    }

    /**
     * به‌روزرسانی تنظیمات زمان‌بندی Job
     */
    public void updateJobSchedule(String jobName, Map<String, String> scheduleConfig) {
        String delayKey = getDelayKey(jobName);
        String batchKey = getBatchKey(jobName);

        if (scheduleConfig.containsKey("fixedDelayMs")) {
            String delayValue = scheduleConfig.get("fixedDelayMs");
            settingService.saveOrUpdate(delayKey, delayValue);
            log.info("Updated {} to {}", delayKey, delayValue);
        }

        if (scheduleConfig.containsKey("batchSize") && batchKey != null) {
            String batchValue = scheduleConfig.get("batchSize");
            settingService.saveOrUpdate(batchKey, batchValue);
            log.info("Updated {} to {}", batchKey, batchValue);
        }

        if (scheduleConfig.containsKey("cron")) {
            String cronValue = scheduleConfig.get("cron");
            try {
                CronExpression.parse(cronValue);
                settingService.saveOrUpdate(delayKey + ".cron", cronValue);
                log.info("Updated cron expression for {} to {}", jobName, cronValue);
            } catch (Exception e) {
                throw new IllegalArgumentException("فرمت Cron نامعتبر: " + cronValue, e);
            }
        }
    }

    /**
     * فعال/غیرفعال کردن Job
     */
    public void toggleJob(String jobName, boolean enabled) {
        String enabledKey = "job." + jobName + ".enabled";
        settingService.saveOrUpdate(enabledKey, String.valueOf(enabled));
        jobEnabledStatus.put(jobName, enabled);
        log.info("Job {} is now {}", jobName, enabled ? "enabled" : "disabled");
    }

    /**
     * بررسی اینکه آیا Job فعال است یا نه
     */
    public boolean isJobEnabled(String jobName) {
        return jobEnabledStatus.computeIfAbsent(jobName, name -> {
            String enabledKey = "job." + name + ".enabled";
            return settingService.getBooleanValue(enabledKey, true);
        });
    }

    private Map<String, Object> getJobInfo(String name, String description, String delayKey, String batchKey) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", name);
        info.put("description", description);
        info.put("enabled", isJobEnabled(name));

        String delayValue = settingService.getValue(delayKey, "60000");
        info.put("fixedDelayMs", delayValue);

        if (batchKey != null) {
            String batchValue = settingService.getValue(batchKey, "25");
            info.put("batchSize", batchValue);
        }

        String cronKey = delayKey + ".cron";
        Optional<String> cronValue = settingService.getValue(cronKey);
        if (cronValue.isPresent()) {
            info.put("cron", cronValue.get());
        }

        return info;
    }

    private String getDelayKey(String jobName) {
        switch (jobName) {
            case "patternDeferredSubmission":
                return "application.pattern.deferred-dispatch.fixed-delay-ms";
            case "outboxDispatcher":
                return "application.outbox.dispatcher.fixed-delay-ms";
            default:
                throw new IllegalArgumentException("Job نامعتبر: " + jobName);
        }
    }

    private String getBatchKey(String jobName) {
        switch (jobName) {
            case "patternDeferredSubmission":
                return "application.pattern.deferred-dispatch.batch-size";
            default:
                return null;
        }
    }
}

