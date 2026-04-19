package com.app.ratelimiter.service;

import com.app.ratelimiter.dto.request.AppRequest;
import com.app.ratelimiter.dto.response.AppResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppService {

    AppResponse create(AppRequest request);

    AppResponse getById(Long id);

    Page<AppResponse> getAll(Pageable pageable);

    AppResponse update(Long id, AppRequest request);

    void delete(Long id);
}
