package com.app.ratelimiter.service.impl;

import com.app.ratelimiter.dto.request.AppInfoRequest;
import com.app.ratelimiter.dto.response.AppInfoResponse;
import com.app.ratelimiter.exception.ResourceAlreadyExistsException;
import com.app.ratelimiter.exception.ResourceNotFoundException;
import com.app.ratelimiter.mapper.AppInfoMapper;
import com.app.ratelimiter.model.AppInfo;
import com.app.ratelimiter.repository.AppInfoRepository;
import com.app.ratelimiter.service.AppInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppInfoServiceImpl implements AppInfoService {

    private final AppInfoRepository appInfoRepository;
    private final AppInfoMapper mapper;

    @Override
    @Transactional
    public AppInfoResponse create(AppInfoRequest request) {
        if (appInfoRepository.existsByServiceName(request.serviceName())) {
            throw new ResourceAlreadyExistsException("App already registered with serviceName: " + request.serviceName());
        }
        AppInfo saved = appInfoRepository.save(mapper.toEntity(request));
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
        return appInfoRepository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public AppInfoResponse update(Long id, AppInfoRequest request) {
        AppInfo appInfo = findById(id);
        appInfo.setServicePort(request.servicePort());
        appInfo.setDescription(request.description());
        log.info("App updated for serviceName={}", appInfo.getServiceName());
        return mapper.toResponse(appInfoRepository.save(appInfo));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AppInfo appInfo = findById(id);
        appInfoRepository.delete(appInfo);
        log.info("App deleted for serviceName={}", appInfo.getServiceName());
    }

    private AppInfo findById(Long id) {
        return appInfoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("App not found with id: " + id));
    }
}
