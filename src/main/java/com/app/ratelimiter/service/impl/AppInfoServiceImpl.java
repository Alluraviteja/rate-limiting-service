package com.app.ratelimiter.service.impl;

import com.app.ratelimiter.dto.request.AppInfoRequest;
import com.app.ratelimiter.dto.response.AppInfoResponse;
import com.app.ratelimiter.exception.ResourceAlreadyExistsException;
import com.app.ratelimiter.exception.ResourceNotFoundException;
import com.app.ratelimiter.mapper.AppInfoMapper;
import com.app.ratelimiter.model.AppInfo;
import com.app.ratelimiter.repository.AppInfoRepository;
import com.app.ratelimiter.service.AppInfoService;
import com.app.ratelimiter.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppInfoServiceImpl implements AppInfoService {

    private final AppInfoRepository appInfoRepository;
    private final AppInfoMapper mapper;
    private final RateLimitService rateLimitService;

    private final ConcurrentHashMap<Long, AppInfo> appInfoCache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public AppInfoResponse create(AppInfoRequest request) {
        if (appInfoRepository.existsByServiceName(request.serviceName())) {
            throw new ResourceAlreadyExistsException("App already registered with serviceName: " + request.serviceName());
        }
        AppInfo saved = appInfoRepository.save(mapper.toEntity(request));
        appInfoCache.put(saved.getId(), saved);
        log.info("App registered with serviceName={}", saved.getServiceName());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AppInfoResponse getById(Long id) {
        return mapper.toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppInfoResponse> getAll(Pageable pageable) {
        Page<AppInfo> page = appInfoRepository.findAll(pageable);
        page.forEach(app -> appInfoCache.put(app.getId(), app));
        return page.map(mapper::toResponse);
    }

    @Override
    @Transactional
    public AppInfoResponse update(Long id, AppInfoRequest request) {
        AppInfo appInfo = findById(id);
        appInfoCache.remove(id);
        rateLimitService.evictAppInfoCache(appInfo.getServiceName(), appInfo.getServicePort());
        appInfo.setDisplayName(request.displayName());
        appInfo.setServicePort(request.servicePort());
        appInfo.setDescription(request.description());
        appInfo.setFailOpen(request.failOpen());
        AppInfo saved = appInfoRepository.save(appInfo);
        appInfoCache.put(saved.getId(), saved);
        log.info("App updated for serviceName={}", saved.getServiceName());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AppInfo appInfo = findById(id);
        rateLimitService.evictAppInfoCache(appInfo.getServiceName(), appInfo.getServicePort());
        appInfoRepository.delete(appInfo);
        appInfoCache.remove(id);
        log.info("App deleted for serviceName={}", appInfo.getServiceName());
    }

    private AppInfo findById(Long id) {
        return appInfoCache.computeIfAbsent(id, key -> appInfoRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("App not found with id: " + key)));
    }
}
