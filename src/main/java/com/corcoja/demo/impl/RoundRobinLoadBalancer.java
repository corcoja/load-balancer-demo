package com.corcoja.demo.impl;

import java.util.List;
import java.util.function.IntPredicate;
import com.corcoja.demo.error.MaxLoadException;
import com.corcoja.demo.error.ProviderNotFoundException;
import com.corcoja.demo.protocol.Provider;

public class RoundRobinLoadBalancer extends BaseLoadBalancer {

    private Integer lastIdx = -1;

    public RoundRobinLoadBalancer(Long checkAliveInterval, Long aliveTimeout) {
        super(checkAliveInterval, aliveTimeout);
    }

    @Override
    public String get() throws MaxLoadException {

        // Sanity check
        if (providers.isEmpty()) {
            throw new ProviderNotFoundException("Load Balancer has no registered providers!");
        }

        // Get next provider in Round Robin sequence
        Integer count = providers.size();
        Integer iterationIdx = lastIdx + 1;

        // Helper function to identify if the provider at a certain iteration is alive
        IntPredicate isProviderAtIterationAlive = i -> {
            Provider provider = providers.get(i % count);
            return alivePings.get(provider) >= 0 && provider.getCurrentLoad() < 1.0;
        };

        // Loop through providers until we find one that is available and not overloaded
        while (!isProviderAtIterationAlive.test(iterationIdx)) {

            // If we have made an entire loop around all providers and none of them are alive, throw
            // an exception
            if (iterationIdx % count == lastIdx || lastIdx == -1 && iterationIdx.equals(count)) {
                throw new MaxLoadException("All providers are down!");
            }

            // Current provider is not alive, skip it and move on to the next on
            iterationIdx = iterationIdx + 1;
        }

        // Send request to the provider and update last provider index
        lastIdx = iterationIdx % count;
        return providers.get(lastIdx).get();
    }

    @Override
    public void registerProviders(List<Provider> providers) {
        super.registerProviders(providers);

        // Reset the index to start over the iteration on providers
        lastIdx = -1;
    }
}
