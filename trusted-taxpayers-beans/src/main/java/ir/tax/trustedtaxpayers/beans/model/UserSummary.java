package ir.tax.trustedtaxpayers.beans.model;

import java.util.UUID;

public class UserSummary {
    private final UUID id;
    private final String username;
    private final String email;
    private final String status;
    private final boolean active;

    public UserSummary(UUID id, String username, String email, String status, boolean active) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.status = status;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }
}


