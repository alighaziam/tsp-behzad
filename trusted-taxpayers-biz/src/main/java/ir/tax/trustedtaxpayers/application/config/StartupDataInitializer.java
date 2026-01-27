package ir.tax.trustedtaxpayers.application.config;

import ir.tax.trustedtaxpayers.application.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@Profile("!test")
public class StartupDataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupDataInitializer.class);

    private final SettingService settingService;

    public StartupDataInitializer(SettingService settingService) {
        this.settingService = settingService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // اگر کلید هست، هیچ؛ اگر نیست مقدار پیش‌فرض ثبت می‌شود
        Map<String, String> defaults = Map.of(
                "rate.limit.enabled", "true",
                "cb.enabled", "true",
                "cache.enabled", "true"
        );

        int created = 0;
        for (Map.Entry<String, String> e : defaults.entrySet()) {
            String key = e.getKey();
            if (settingService.getValue(key).isEmpty()) {
                settingService.saveOrUpdate(key, e.getValue());
                created++;
            }
        }
        if (created > 0) {
            logger.info("Startup settings seeded: {} entries", created);
        } else {
            logger.info("Startup settings already present, no seeding needed");
        }
    }
}


