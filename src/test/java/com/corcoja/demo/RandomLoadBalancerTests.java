package com.corcoja.demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.corcoja.demo.error.MaxLoadException;
import com.corcoja.demo.protocol.LoadBalancer;
import com.corcoja.demo.protocol.Provider;

public class RandomLoadBalancerTests {

    @Test
    public void testRandomOneProvider() {
        String customUuid = Constants.dummyProviderName;
        Provider provider = Utils.createSimpleProvider(customUuid);

        LoadBalancer loadBalancer = Utils.createRandomLoadBalancer();
        loadBalancer.registerProviders(List.of(provider));

        System.out.println(MessageFormat.format(
                "Send {0} requests to the provider. All of them must return the same UUID",
                Constants.providerMaxConcurrentRequests));

        for (int i = 0; i < Constants.providerMaxConcurrentRequests; i++) {
            assertEquals(customUuid, loadBalancer.get());
        }
    }

    @ParameterizedTest
    @CsvSource({"10,10", "5,25", "1,10", "1,1", "1,0", "10,100", "7,63"})
    public void testRandomMultipleProviders(Integer providerCount, Integer requests) {
        List<String> providerNames = Utils.getDummyProviderNames(providerCount);
        LoadBalancer loadBalancer = Utils.createRandomLoadBalancer();

        // Create providers and register them on the load balancer
        List<Provider> providers = providerNames.stream().map(Utils::createSimpleProvider)
                .collect(Collectors.toList());
        loadBalancer.registerProviders(providers);

        System.out.println(
                "Check that requests to load balancer will be forwarded to random providers");

        Map<String, Integer> responses = new HashMap<>();

        for (int i = 0; i < requests; i++) {
            String uuid = assertDoesNotThrow(() -> loadBalancer.get());
            responses.put(uuid, responses.getOrDefault(uuid, 0) + 1);
        }

        // @formatter:off
        String distribution = providerNames.stream()
                .map(key -> (float) responses.getOrDefault(key, 0))
                .map(acc -> requests > 0 ? acc / (float) requests : -1.0f)
                .map(rate -> String.format("%.02f", rate))
                .collect(Collectors.joining( ", " ));
        // @formatter:on

        System.out.println(MessageFormat.format("Request distribution: [{0}]", distribution));
    }

    @ParameterizedTest
    @CsvSource({"1,11", "2,25", "5,99", "10,999"})
    public void testRandomMultipleProvidersOverloaded(Integer providerCount, Integer requests) {
        List<String> providerNames = Utils.getDummyProviderNames(providerCount);
        LoadBalancer loadBalancer = Utils.createRandomLoadBalancer();

        // Create providers and register them on the load balancer
        List<Provider> providers = providerNames.stream().map(Utils::createSimpleProvider)
                .collect(Collectors.toList());
        loadBalancer.registerProviders(providers);

        System.out.println("Check that load balancer will throw an exception once maximum "
                + "capacity is reached on all providers");

        assertThrows(MaxLoadException.class, () -> {
            for (int i = 0; i < requests; i++) {
                loadBalancer.get();
            }
        });
    }
}
