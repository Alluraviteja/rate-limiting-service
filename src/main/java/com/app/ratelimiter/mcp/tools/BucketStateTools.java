package com.app.ratelimiter.mcp.tools;

import com.app.ratelimiter.mcp.dto.AppBucketSummary;
import com.app.ratelimiter.mcp.dto.BucketStateResult;
import com.app.ratelimiter.service.mcp.BucketStateMcpService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BucketStateTools {

    private final BucketStateMcpService bucketStateMcpService;

    @McpTool(
            name = "get_bucket_state",
            description = "Returns the current live token count for a specific app's bucket(s) directly from Redis. Shows real-time remaining tokens, depletion status, and near-depletion warnings. Capped at 100 buckets per app in per-IP mode.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false, openWorldHint = false)
    )
    public BucketStateResult getBucketState(
            @McpToolParam(description = "The app ID to check bucket state for") long appInfoId) {
        return bucketStateMcpService.getBucketState(appInfoId);
    }

    @McpTool(
            name = "get_all_bucket_states",
            description = "Returns a snapshot of bucket health across all enabled apps. Shows aggregate stats per app: total buckets, depleted count, near-depletion count, and average remaining percentage.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false, openWorldHint = false)
    )
    public List<AppBucketSummary> getAllBucketStates() {
        return bucketStateMcpService.getAllBucketStates();
    }
}
