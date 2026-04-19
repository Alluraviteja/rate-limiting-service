package com.app.ratelimiter.mapper;

import com.app.ratelimiter.dto.request.AppRequest;
import com.app.ratelimiter.dto.response.AppResponse;
import com.app.ratelimiter.model.App;
import org.springframework.stereotype.Component;

@Component
public class AppMapper {

    public App toEntity(AppRequest request) {
        return App.builder()
                .appId(request.appId())
                .port(request.port())
                .description(request.description())
                .enabled(false)
                .build();
    }

    public AppResponse toResponse(App entity) {
        return new AppResponse(
                entity.getId(),
                entity.getAppId(),
                entity.getPort(),
                entity.getDescription(),
                entity.getEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
