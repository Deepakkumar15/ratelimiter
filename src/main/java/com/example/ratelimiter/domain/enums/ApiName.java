package com.example.ratelimiter.domain.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.example.ratelimiter.constants.ApiUrlConstants.HEALTH_CHECK_API_URL;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ApiName {
    HEALTH_CHECK(HEALTH_CHECK_API_URL);

    private final String url;
}
