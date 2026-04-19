package com.app.ratelimiter.service.impl;

import com.app.ratelimiter.dto.request.AppRequest;
import com.app.ratelimiter.dto.response.AppResponse;
import com.app.ratelimiter.exception.ResourceAlreadyExistsException;
import com.app.ratelimiter.exception.ResourceNotFoundException;
import com.app.ratelimiter.mapper.AppMapper;
import com.app.ratelimiter.model.App;
import com.app.ratelimiter.repository.AppRepository;
import com.app.ratelimiter.service.AppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppServiceImpl implements AppService {

    private final AppRepository appRepository;
    private final AppMapper mapper;

    @Override
    @Transactional
    public AppResponse create(AppRequest request) {
        if (appRepository.existsByAppId(request.appId())) {
            throw new ResourceAlreadyExistsException("App already registered with appId: " + request.appId());
        }
        App saved = appRepository.save(mapper.toEntity(request));
        log.info("App registered with appId={}", saved.getAppId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AppResponse getById(Long id) {
        return mapper.toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppResponse> getAll(Pageable pageable) {
        return appRepository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public AppResponse update(Long id, AppRequest request) {
        App app = findById(id);
        app.setDescription(request.description());
        log.info("App updated for appId={}", app.getAppId());
        return mapper.toResponse(appRepository.save(app));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        App app = findById(id);
        appRepository.delete(app);
        log.info("App deleted for appId={}", app.getAppId());
    }

    private App findById(Long id) {
        return appRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("App not found with id: " + id));
    }
}
