package com.app.ratelimiter.mcp.dto;

public record IpBlockEntry(String ip, long blocks) {}
