package com.app.ratelimiter.mapper;

import com.app.ratelimiter.dto.request.AppRequest;
import com.app.ratelimiter.dto.response.AppResponse;
import com.app.ratelimiter.model.AppInfo;
import org.springframework.stereotype.Component;

@Component
public class AppInfoMapper {

    public AppInfo toEntity(AppRequest request) {
        return AppInfo.builder()
                .serviceName(request.serviceName())
                .serviceUrl(request.serviceUrl())
                .description(request.description())
                .enabled(false)
                .build();
    }

    public AppResponse toResponse(AppInfo entity) {
        return new AppResponse(
                entity.getId(),
                entity.getServiceName(),
                entity.getServiceUrl(),
                entity.getDescription(),
                entity.getEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
