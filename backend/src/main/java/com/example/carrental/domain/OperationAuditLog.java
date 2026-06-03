package com.example.carrental.domain;

import com.example.carrental.common.Enums.UserRole;
import jakarta.persistence.*;

@Entity
@Table(name = "operation_audit_log", indexes = {
        @Index(name = "idx_audit_actor", columnList = "actor_user_id"),
        @Index(name = "idx_audit_path", columnList = "path"),
        @Index(name = "idx_audit_create_time", columnList = "create_time")
})
public class OperationAuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(length = 50)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole actorRole;

    @Column(nullable = false, length = 10)
    private String httpMethod;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(nullable = false, length = 80)
    private String action;

    private Integer responseStatus;

    @Column(nullable = false)
    private Boolean success;

    @Column(length = 80)
    private String clientIp;

    @Column(length = 255)
    private String userAgent;

    @Column(length = 500)
    private String errorMessage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = limit(actorUsername, 50);
    }

    public UserRole getActorRole() {
        return actorRole;
    }

    public void setActorRole(UserRole actorRole) {
        this.actorRole = actorRole;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = limit(path, 255);
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = limit(action, 80);
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = limit(clientIp, 80);
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = limit(userAgent, 255);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = limit(errorMessage, 500);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
