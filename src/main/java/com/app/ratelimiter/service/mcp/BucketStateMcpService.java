package com.app.ratelimiter.service.mcp;

import com.app.ratelimiter.mcp.dto.AppBucketSummary;
import com.app.ratelimiter.mcp.dto.BucketStateResult;

import java.util.List;

public interface BucketStateMcpService {

    BucketStateResult getBucketState(Long appInfoId);

    List<AppBucketSummary> getAllBucketStates();
}
