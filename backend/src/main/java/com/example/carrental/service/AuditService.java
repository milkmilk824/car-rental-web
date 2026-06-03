package com.example.carrental.service;

import com.example.carrental.common.PageResult;
import com.example.carrental.common.Enums.UserRole;
import com.example.carrental.domain.OperationAuditLog;
import com.example.carrental.dto.AuditDtos;
import com.example.carrental.repository.OperationAuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditService {

    private final OperationAuditLogRepository auditLogRepository;

    public AuditService(OperationAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async("auditExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditCommand command) {
        OperationAuditLog log = new OperationAuditLog();
        log.setActorUserId(command.actorUserId());
        log.setActorUsername(command.actorUsername());
        log.setActorRole(command.actorRole());
        log.setHttpMethod(command.httpMethod());
        log.setPath(command.path());
        log.setAction(command.httpMethod() + " " + command.path());
        log.setResponseStatus(command.responseStatus());
        log.setSuccess(command.success());
        log.setClientIp(command.clientIp());
        log.setUserAgent(command.userAgent());
        log.setErrorMessage(command.errorMessage());
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PageResult<AuditDtos.AuditLogResponse> list(int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        return PageResult.from(auditLogRepository.findAll(pageRequest).map(this::toResponse));
    }

    private AuditDtos.AuditLogResponse toResponse(OperationAuditLog log) {
        return new AuditDtos.AuditLogResponse(
                log.getId(),
                log.getActorUserId(),
                log.getActorUsername(),
                log.getActorRole(),
                log.getHttpMethod(),
                log.getPath(),
                log.getAction(),
                log.getResponseStatus(),
                log.getSuccess(),
                log.getClientIp(),
                log.getUserAgent(),
                log.getErrorMessage(),
                log.getCreateTime()
        );
    }

    public record AuditCommand(
            Long actorUserId,
            String actorUsername,
            UserRole actorRole,
            String httpMethod,
            String path,
            int responseStatus,
            boolean success,
            String clientIp,
            String userAgent,
            String errorMessage
    ) {
    }
}
