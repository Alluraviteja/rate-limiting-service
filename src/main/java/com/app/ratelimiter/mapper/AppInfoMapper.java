package com.app.ratelimiter.mapper;

import com.app.ratelimiter.dto.request.AppInfoRequest;
import com.app.ratelimiter.dto.response.AppInfoResponse;
import com.app.ratelimiter.model.AppInfo;
import org.springframework.stereotype.Component;

@Component
public class AppInfoMapper {

    public AppInfo toEntity(AppInfoRequest request) {
        return AppInfo.builder()
                .serviceName(request.serviceName())
                .serviceUrl(request.serviceUrl())
                .servicePort(request.servicePort())
                .description(request.description())
                .enabled(false)
                .build();
    }

    public AppInfoResponse toResponse(AppInfo entity) {
        return new AppInfoResponse(
                entity.getId(),
                entity.getServiceName(),
                entity.getServiceUrl(),
                entity.getServicePort(),
                entity.getDescription(),
                entity.getEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
