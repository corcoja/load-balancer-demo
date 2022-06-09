package com.corcoja.demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import com.corcoja.demo.protocol.Provider;

public class SimpleProviderTests {

    @Test
    public void testProviderConcurrentRequests() {
        Provider provider = Utils.createSimpleProvider();

        Integer requests = 16;

        Float maxLoad =
                Math.min(1.0f, (float) requests / (float) Constants.providerMaxConcurrentRequests);

        Duration processingWaitTime =
                Constants.providerRequestProcessingTime.multipliedBy(1 + (long) Math
                        .ceil((float) requests / (float) Constants.providerMaxConcurrentRequests));

        // Create a thread pool to execute multiple concurrent requests to provider
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        for (int i = 0; i < requests; i++) {
            final int iteration = i;
            executor.submit(() -> {
                System.out.println(MessageFormat.format("Iteration {0}, provider {1}", iteration,
                        provider.get()));
            });
        }

        // @formatter:off
        Awaitility.await()
                .pollInterval(Duration.ofMillis(500))
                .atMost(Duration.ofSeconds(2))
                .until(() -> Math.abs(maxLoad - provider.getCurrentLoad()) < Constants.eps);
        // @formatter:on

        System.out.println(MessageFormat.format("Current load: {0}", provider.getCurrentLoad()));

        // @formatter:off
        Awaitility.await()
                .pollInterval(Duration.ofSeconds(2))
                .atMost(processingWaitTime)
                .until(() -> provider.getCurrentLoad() < Constants.eps);
        // @formatter:on
    }

    @Test
    public void testProviderAvailability() {
        Provider provider = Utils.createSimpleProvider();

        // Set provider as unavailable
        provider.setAvailability(false);

        // Provider must be unavailable when invoking check method
        CompletableFuture<Boolean> checkFuture = CompletableFuture.supplyAsync(provider::check);
        assertThrows(TimeoutException.class, () -> checkFuture.get(2, TimeUnit.SECONDS));

        // Provider must be also unavailable when invoking get method
        CompletableFuture<String> getFuture = CompletableFuture.supplyAsync(provider::get);
        assertThrows(TimeoutException.class, () -> getFuture.get(2, TimeUnit.SECONDS));

        // Set provider as available
        provider.setAvailability(true);

        // Now all requests to provider must work and return immediately
        assertDoesNotThrow(() -> assertEquals(Boolean.TRUE, checkFuture.get()));
        assertDoesNotThrow(() -> assertNotNull(getFuture.get()));
    }
}
