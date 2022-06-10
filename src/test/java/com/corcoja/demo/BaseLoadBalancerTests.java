package com.corcoja.demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.corcoja.demo.Utils.AliveProvidersLoadBalancer;
import com.corcoja.demo.error.MaxLoadException;
import com.corcoja.demo.error.ProviderNotFoundException;
import com.corcoja.demo.protocol.LoadBalancer;
import com.corcoja.demo.protocol.Provider;

public class BaseLoadBalancerTests {

    @Test
    public void testProvidersAllAlive() {
        List<String> providerNames = Utils.getDummyProviderNames(5);
        LoadBalancer loadBalancer = Utils.createRoundRobinLoadBalancer();

        // Create providers and register them on the load balancer
        List<Provider> providers = providerNames.stream().map(Utils::createCheckCountProvider)
                .collect(Collectors.toList());
        loadBalancer.registerProviders(providers);

        Integer x = 2;
        System.out.println(MessageFormat.format(
                "Wait until Load Balancer will check alive providers at least {0} times for each provider",
                x));

        // @formatter:off
        assertDoesNotThrow(() -> Awaitility.await()
                .forever()
                .pollInterval(Constants.providerRequestProcessingTime.dividedBy(2))
                .until(() -> providers.stream().allMatch(
                        provider -> ((Utils.CheckCountProvider) provider).getCheckCount() >= x)));
        // @formatter:on
    }

    @ParameterizedTest
    @CsvSource({"5,2", "4,4", "3,0"})
    public void testProvidersUnavailable(Integer providerCount, Integer providersDown) {
        List<String> providerNames = Utils.getDummyProviderNames(providerCount);
        AliveProvidersLoadBalancer loadBalancer = Utils.createAliveProvidersLoadBalancer();

        // Create providers and register them on the load balancer
        List<Provider> providers = providerNames.stream().map(Utils::createCheckCountProvider)
                .collect(Collectors.toList());
        loadBalancer.registerProviders(providers);

        System.out
                .println("Wait until Load Balancer will check alive providers for the first time");

        // @formatter:off
        assertDoesNotThrow(() -> Awaitility.await()
                .forever()
                .pollInterval(Constants.providerRequestProcessingTime.dividedBy(2))
                .until(() -> providers.stream().allMatch(
                        provider -> ((Utils.CheckCountProvider) provider).getCheckCount() > 0)));
        // @formatter:on

        System.out.println(MessageFormat.format("Kill first {0} providers", providersDown));

        for (int i = 0; i < providersDown; i++) {
            providers.get(i).setAvailability(false);
        }

        System.out.println(MessageFormat.format(
                "Check that the number of alive providers decreased by {0}", providersDown));

        // @formatter:off
        assertDoesNotThrow(() -> Awaitility.await()
                .forever()
                .pollInterval(Constants.providerRequestProcessingTime.dividedBy(2))
                .until(() -> loadBalancer.getAliveProviders().size() == providers.size() - providersDown ));
        // @formatter:on
    }

    @ParameterizedTest
    @CsvSource({"5,2,3", "10,4,10", "5,5,5", "4,0,8"})
    public void testProvidersRecover(Integer providerCount, Integer providersDown,
            Integer requestsPerProvider) {

        List<String> providerNames = Utils.getDummyProviderNames(providerCount);
        AliveProvidersLoadBalancer loadBalancer = Utils.createAliveProvidersLoadBalancer();

        // Create providers and register them on the load balancer
        List<Provider> providers = providerNames.stream()
                .map((name) -> Utils.createCheckCountProvider(name, requestsPerProvider * 2))
                .collect(Collectors.toList());
        loadBalancer.registerProviders(providers);

        System.out
                .println("Wait until Load Balancer will check alive providers for the first time");

        // @formatter:off
        assertDoesNotThrow(() -> Awaitility.await()
                .forever()
                .pollInterval(Constants.providerRequestProcessingTime.dividedBy(2))
                .until(() -> providers.stream().allMatch(
                        provider -> ((Utils.CheckCountProvider) provider).getCheckCount() > 0)));
        // @formatter:on

        System.out.println(MessageFormat.format("Kill first {0} providers", providersDown));

        for (int i = 0; i < providersDown; i++) {
            providers.get(i).setAvailability(false);
        }

        System.out.println(MessageFormat.format(
                "Check that the number of alive providers decreased by {0}", providersDown));

        // @formatter:off
        assertDoesNotThrow(() -> Awaitility.await()
                .forever()
                .pollInterval(Constants.providerRequestProcessingTime.dividedBy(2))
                .until(() -> loadBalancer.getAliveProviders().size() == providers.size() - providersDown));
        // @formatter:on

        System.out.println(MessageFormat.format(
                "Check that all requests to load balancer will not be forwarded to the first {0} providers",
                providersDown));

        Map<String, Integer> responses = new HashMap<>();

        if (providerCount.equals(providersDown)) {
            System.out.println(
                    "All providers are down, check a request to load balancer will raise an exception");
            assertThrows(MaxLoadException.class, loadBalancer::get);
        } else {
            for (int i = 0; i < (providerCount - providersDown) * requestsPerProvider; i++) {
                assertDoesNotThrow(() -> {
                    String uuid = loadBalancer.get();
                    responses.put(uuid, responses.getOrDefault(uuid, 0) + 1);
                });
            }
        }

        for (int i = 0; i < providerNames.size(); i++) {
            String providerName = providerNames.get(i);
            Integer expectedCount = i < providersDown ? 0 : requestsPerProvider;
            assertEquals(responses.getOrDefault(providerName, 0), expectedCount);
        }

        System.out.println(MessageFormat.format("Recover the first two providers", providersDown));

        for (int i = 0; i < providersDown; i++) {
            providers.get(i).setAvailability(true);
        }

        System.out.println("Check that all providers are alive");

        // @formatter:off
        assertDoesNotThrow(() -> Awaitility.await()
                .forever()
                .pollInterval(Constants.providerRequestProcessingTime.dividedBy(2))
                .until(() -> loadBalancer.getAliveProviders().size() == providers.size()));
        // @formatter:on

        System.out
                .println("Check that requests to load balancer will be forwarded to all providers");

        responses.clear();

        for (int i = 0; i < providerCount * requestsPerProvider; i++) {
            assertDoesNotThrow(() -> {
                String uuid = loadBalancer.get();
                responses.put(uuid, responses.getOrDefault(uuid, 0) + 1);
            });
        }

        for (int i = 0; i < providerNames.size(); i++) {
            String providerName = providerNames.get(i);
            assertEquals(responses.get(providerName), requestsPerProvider);
        }
    }

    @ParameterizedTest
    @CsvSource({"4,2,3", "5,1,2", "3,0,5"})
    void testProvidersAdd(Integer providerCount, Integer newProviders,
            Integer requestsPerProvider) {
        List<String> providerNames = Utils.getDummyProviderNames(providerCount);
        AliveProvidersLoadBalancer loadBalancer = Utils.createAliveProvidersLoadBalancer();

        // Create providers and register them on the load balancer
        List<Provider> providers = providerNames.stream()
                .map((name) -> Utils.createCheckCountProvider(name, requestsPerProvider))
                .collect(Collectors.toList());
        loadBalancer.registerProviders(providers);

        System.out
                .println("Wait until Load Balancer will check alive providers for the first time");

        // @formatter:off
        assertDoesNotThrow(() -> Awaitility.await()
                .forever()
                .pollInterval(Constants.providerRequestProcessingTime.dividedBy(2))
                .until(() -> providers.stream().allMatch(
                        provider -> ((Utils.CheckCountProvider) provider).getCheckCount() > 0)));
        // @formatter:on

        System.out.println(MessageFormat.format("Add {0} new providers", newProviders));

        List<String> newProviderNames = Utils.getDummyProviderNames(newProviders, providerCount);

        for (String newProviderName : newProviderNames) {
            loadBalancer.addProvider(Utils.createCheckCountProvider(newProviderName));
        }

        System.out.println(MessageFormat
                .format("Check that the number of providers increased by {0}", newProviders));

        // @formatter:off
        assertDoesNotThrow(() -> Awaitility.await()
                .forever()
                .pollInterval(Constants.providerRequestProcessingTime.dividedBy(2))
                .until(() -> loadBalancer.getAliveProviders().size() == providerCount + newProviders));
        // @formatter:on

        System.out.println(MessageFormat.format(
                "Check that all requests to load balancer will be forwarded to all {0} providers",
                providerCount + newProviders));

        Map<String, Integer> responses = new HashMap<>();
        List<String> allProviderNames =
                Stream.concat(providerNames.stream(), newProviderNames.stream())
                        .collect(Collectors.toList());

        for (int i = 0; i < allProviderNames.size() * requestsPerProvider; i++) {
            assertDoesNotThrow(() -> {
                String uuid = loadBalancer.get();
                responses.put(uuid, responses.getOrDefault(uuid, 0) + 1);
            });
        }

        for (String providerName : allProviderNames) {
            assertEquals(responses.getOrDefault(providerName, 0), requestsPerProvider);
        }
    }

    @ParameterizedTest
    @CsvSource({"5,2,3", "4,3,10", "3,0,5", "6,6,7"})
    void testProvidersRemove(Integer providerCount, Integer providersToRemove,
            Integer requestsPerProvider) {
        List<String> providerNames = Utils.getDummyProviderNames(providerCount);
        AliveProvidersLoadBalancer loadBalancer = Utils.createAliveProvidersLoadBalancer();

        // Create providers and register them on the load balancer
        List<Provider> providers = providerNames.stream()
                .map((name) -> Utils.createCheckCountProvider(name, requestsPerProvider + 1))
                .collect(Collectors.toList());
        loadBalancer.registerProviders(providers);

        System.out
                .println("Wait until Load Balancer will check alive providers for the first time");

        // @formatter:off
        assertDoesNotThrow(() -> Awaitility.await()
                .forever()
                .pollInterval(Constants.providerRequestProcessingTime.dividedBy(2))
                .until(() -> providers.stream().allMatch(
                        provider -> ((Utils.CheckCountProvider) provider).getCheckCount() > 0)));
        // @formatter:on

        System.out.println(MessageFormat
                .format("Send {0} requests to update the provider index to point to the last one "
                        + "(in case of Round Robin Load Balancer)", providerCount));

        Map<String, Integer> responses = new HashMap<>();

        for (int i = 0; i < providerCount; i++) {
            assertDoesNotThrow(() -> {
                String uuid = loadBalancer.get();
                responses.put(uuid, responses.getOrDefault(uuid, 0) + 1);
            });
        }

        for (String providerName : providerNames) {
            assertEquals(1, responses.getOrDefault(providerName, 0));
        }

        System.out.println(MessageFormat.format("Remove last {0} providers", providersToRemove));

        for (int i = providerCount - providersToRemove; i < providerCount; i++) {
            loadBalancer.removeProvider(providers.get(i));
        }

        List<String> updatedProviderNames =
                providerNames.subList(0, providerCount - providersToRemove);

        System.out.println(MessageFormat
                .format("Check that the number of providers decreased by {0}", providersToRemove));

        // @formatter:off
        assertDoesNotThrow(() -> Awaitility.await()
                .forever()
                .pollInterval(Constants.providerRequestProcessingTime.dividedBy(2))
                .until(() -> loadBalancer.getAliveProviders().size() == updatedProviderNames.size()));
        // @formatter:on

        if (updatedProviderNames.isEmpty()) {
            System.out.println(
                    "All providers removed from load balancer. Check exception thrown when sending a request");
            assertThrows(ProviderNotFoundException.class, loadBalancer::get);
        } else {
            System.out.println(MessageFormat.format(
                    "Check that all requests to load balancer will be forwarded to the updated {0} providers",
                    updatedProviderNames.size()));

            responses.clear();

            for (int i = 0; i < updatedProviderNames.size() * requestsPerProvider; i++) {
                assertDoesNotThrow(() -> {
                    String uuid = loadBalancer.get();
                    responses.put(uuid, responses.getOrDefault(uuid, 0) + 1);
                });
            }

            for (String providerName : updatedProviderNames) {
                assertEquals(responses.getOrDefault(providerName, 0), requestsPerProvider);
            }
        }
    }
}
