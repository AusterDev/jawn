package com.github.AusterDev.jawn.core.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class VerificationSession {
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("degree_type")
    private String degreeType;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("verified")
    private boolean verified;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("created_at")
    private String createdAt;

    public VerificationSession() {}

    public VerificationSession(String userId, String sessionId) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.degreeType = null;
        this.verified = false;
        this.createdAt = Instant.now().toString();
        this.reason = null;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDegreeType() { return degreeType; }
    public void setDegreeType(String degreeType) { this.degreeType = degreeType; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getReason() {
        return reason;
    }
    public void setReason(String reason) { this.reason = reason; }
}