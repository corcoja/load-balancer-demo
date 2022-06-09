package com.corcoja.demo;

import java.time.Duration;

class Constants {

    /**
     * Epsilon.
     */
    static final Float eps = (float) Math.pow(10, -8);

    /**
     * Max concurrent requests that one provider can handle.
     */
    static final Integer providerMaxConcurrentRequests = 10;

    /**
     * The artificial processing time for one provider's request.
     */
    static final Duration providerRequestProcessingTime = Duration.ofSeconds(5);

    /*
     * Time interval the load balancer will check if the registered providers are alive or not.
     */
    static final Duration loadBalancerAliveInterval = Duration.ofSeconds(5);

    /**
     * Timeout until a provider is marked unavailable.
     */
    static final Duration loadBalancerAliveTimeout = Duration.ofSeconds(2);

    /**
     * Idem.
     */
    static final String dummyProviderName = "dummy_provider_name";
}
