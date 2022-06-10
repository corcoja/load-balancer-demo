package com.corcoja.demo.impl;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import com.corcoja.demo.error.MaxLoadException;
import com.corcoja.demo.error.ProviderNotFoundException;
import com.corcoja.demo.protocol.Provider;

public class RandomLoadBalancer extends BaseLoadBalancer {

    private Random random = new Random();

    public RandomLoadBalancer(Long checkAliveInterval, Long aliveTimeout) {
        super(checkAliveInterval, aliveTimeout);
    }

    @Override
    public String get() throws MaxLoadException {

        // Sanity check
        if (providers.isEmpty()) {
            throw new ProviderNotFoundException("Load Balancer has no registered providers!");
        }

        // Get all alive providers
        List<Provider> aliveProviders =
                alivePings.entrySet().stream().filter(entry -> entry.getValue() >= 0)
                        .map(Map.Entry::getKey).collect(Collectors.toList());

        // Check if there are alive provides
        if (aliveProviders.isEmpty()) {
            throw new MaxLoadException("All providers are down!");
        }

        return aliveProviders.get(random.nextInt(aliveProviders.size())).get();
    }
}
