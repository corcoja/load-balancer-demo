package com.corcoja.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import com.corcoja.demo.protocol.LoadBalancer;
import com.corcoja.demo.protocol.Provider;

public class RoundRobinLoadBalancerTests {

    @Test
    public void testRoundRobinOneProvider() {
        String customUuid = Constants.dummyProviderName;
        Provider provider = Utils.createSimpleProvider(customUuid);

        LoadBalancer loadBalancer = Utils.createRoundRobinLoadBalancer();
        loadBalancer.registerProviders(List.of(provider));

        System.out.println(MessageFormat.format(
                "Send {0} requests to the provider. All of them must return the same UUID",
                Constants.providerMaxConcurrentRequests));

        for (int i = 0; i < Constants.providerMaxConcurrentRequests; i++) {
            assertEquals(customUuid, loadBalancer.get());
        }
    }

    @Test
    public void testRoundRobinMultipleProviders() {
        List<String> providerNames = Utils.getDummyProviderNames(5);
        LoadBalancer loadBalancer = Utils.createRoundRobinLoadBalancer();

        // Create providers and register them on the load balancer
        List<Provider> providers = providerNames.stream().map(Utils::createSimpleProvider)
                .collect(Collectors.toList());
        loadBalancer.registerProviders(providers);

        System.out
                .println("Check that requests to load balancer will be forwarded to all providers");

        for (int i = 0; i < Constants.providerMaxConcurrentRequests * providerNames.size(); i++) {
            Integer providerIdx = i % providerNames.size();
            assertEquals(providerNames.get(providerIdx), loadBalancer.get());
        }

        System.out.println("The load on each provider must be at its peak, since we invoked "
                + "the maximum allowed request for each provider");

        for (Provider provider : providers) {
            assertTrue(Math.abs(1.0 - provider.getCurrentLoad()) < Constants.eps);
        }
    }
}
