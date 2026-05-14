package com.app.ratelimiter.mcp.tools;

import com.app.ratelimiter.mcp.dto.AppBucketSummary;
import com.app.ratelimiter.mcp.dto.BucketStateResult;
import com.app.ratelimiter.service.mcp.BucketStateMcpService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BucketStateTools {

    private final BucketStateMcpService bucketStateMcpService;

    @Tool(name = "get_bucket_state",
          description = "Returns the current live token count for a specific app's bucket(s) directly from Redis. Shows real-time remaining tokens, depletion status, and near-depletion warnings. Capped at 100 buckets per app in per-IP mode.")
    public BucketStateResult getBucketState(
            @ToolParam(description = "The app ID to check bucket state for") long appInfoId) {
        return bucketStateMcpService.getBucketState(appInfoId);
    }

    @Tool(name = "get_all_bucket_states",
          description = "Returns a snapshot of bucket health across all enabled apps. Shows aggregate stats per app: total buckets, depleted count, near-depletion count, and average remaining percentage.")
    public List<AppBucketSummary> getAllBucketStates() {
        return bucketStateMcpService.getAllBucketStates();
    }
}
