package com.app.ratelimiter.service;

import com.app.ratelimiter.dto.request.AppInfoRequest;
import com.app.ratelimiter.dto.response.AppInfoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppInfoService {

    AppInfoResponse create(AppInfoRequest request);

    AppInfoResponse getById(Long id);

    Page<AppInfoResponse> getAll(Pageable pageable);

    AppInfoResponse update(Long id, AppInfoRequest request);

    void delete(Long id);
}
