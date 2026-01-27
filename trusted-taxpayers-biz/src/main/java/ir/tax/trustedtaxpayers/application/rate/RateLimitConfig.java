package ir.tax.trustedtaxpayers.application.rate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfig {
    private boolean enabled;
    private long capacity;
    private long refillTokens;
    private Duration period;
    private RateLimited.KeyStrategy keyStrategy;
    private Set<String> whitelist;
    private Set<String> blacklist;

    public Set<String> safeWhitelist() {
        return whitelist == null ? Collections.emptySet() : whitelist;
    }

    public Set<String> safeBlacklist() {
        return blacklist == null ? Collections.emptySet() : blacklist;
    }
}


