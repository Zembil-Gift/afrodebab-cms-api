package com.afrodebab.cms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai-overview")
public class AiOverviewProperties {

    private int pollIntervalMinutes = 1;
    private int maxJobDescriptionChars = 3000;
    private int maxAttempts = 3;

    public int getPollIntervalMinutes() { return pollIntervalMinutes; }
    public void setPollIntervalMinutes(int pollIntervalMinutes) { this.pollIntervalMinutes = pollIntervalMinutes; }

    public int getMaxJobDescriptionChars() { return maxJobDescriptionChars; }
    public void setMaxJobDescriptionChars(int maxJobDescriptionChars) { this.maxJobDescriptionChars = maxJobDescriptionChars; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public long getPollIntervalMs() {
        return pollIntervalMinutes * 60_000L;
    }
}
