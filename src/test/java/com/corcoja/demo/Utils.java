package com.corcoja.demo;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.corcoja.demo.impl.RoundRobinLoadBalancer;
import com.corcoja.demo.impl.SimpleProvider;
import com.corcoja.demo.protocol.LoadBalancer;
import com.corcoja.demo.protocol.Provider;

class Utils {

    /**
     * This is a custom provider that counts the number of times {@link #check()} method was
     * invoked.
     * 
     * @implNote For testing purposes only!
     */
    static class CheckCountProvider extends SimpleProvider {

        private AtomicInteger checkCount = new AtomicInteger();

        CheckCountProvider(String customUuid, Integer maxConcurrentRequests,
                Duration oneRequestProcessingTime) {
            super(customUuid, maxConcurrentRequests, oneRequestProcessingTime);
        }

        @Override
        public Boolean check() {
            checkCount.incrementAndGet();
            return super.check();
        }

        Integer getCheckCount() {
            return checkCount.get();
        }
    }

    /**
     * This is a custom load balancer (based on {@link RoundRobinLoadBalancer}) that has an
     * additional method that will return all currently alive providers.
     * 
     * @implNote For testing purposes only!
     */
    static class AliveProvidersLoadBalancer extends RoundRobinLoadBalancer {

        AliveProvidersLoadBalancer(Long checkAliveInterval, Long aliveTimeout) {
            super(checkAliveInterval, aliveTimeout);
        }

        List<Provider> getAliveProviders() {
            return alivePings.entrySet().stream().filter(entry -> entry.getValue() >= 0)
                    .map(Map.Entry::getKey).collect(Collectors.toList());
        }
    }

    static List<String> getDummyProviderNames(Integer count) {
        return getDummyProviderNames(count, 0);
    }

    static List<String> getDummyProviderNames(Integer count, Integer offset) {
        return IntStream.range(offset, offset + count)
                .mapToObj(idx -> MessageFormat.format("provider{0}", idx))
                .collect(Collectors.toList());
    }

    static Provider createSimpleProvider() {
        return new SimpleProvider(Constants.providerMaxConcurrentRequests,
                Constants.providerRequestProcessingTime);
    }

    static Provider createSimpleProvider(String customUuid) {
        return new SimpleProvider(customUuid, Constants.providerMaxConcurrentRequests,
                Constants.providerRequestProcessingTime);
    }

    static Provider createCheckCountProvider(String customUuid) {
        return new CheckCountProvider(customUuid, Constants.providerMaxConcurrentRequests,
                Constants.providerRequestProcessingTime);
    }

    static LoadBalancer createRoundRobinLoadBalancer() {
        return new RoundRobinLoadBalancer(Constants.loadBalancerAliveInterval.toMillis(),
                Constants.loadBalancerAliveTimeout.toMillis());
    }

    static AliveProvidersLoadBalancer createAliveProvidersLoadBalancer() {
        return new AliveProvidersLoadBalancer(Constants.loadBalancerAliveInterval.toMillis(),
                Constants.loadBalancerAliveTimeout.toMillis());
    }
}
