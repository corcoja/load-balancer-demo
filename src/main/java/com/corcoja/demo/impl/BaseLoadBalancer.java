package com.corcoja.demo.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.corcoja.demo.error.ProviderAlreadyRegisteredException;
import com.corcoja.demo.error.ProviderNotFoundException;
import com.corcoja.demo.protocol.LoadBalancer;
import com.corcoja.demo.protocol.Provider;

public abstract class BaseLoadBalancer implements LoadBalancer {

    private static final Integer PROVIDER_UNAVAILABLE_RESET_PINGS = -2;

    private static Logger logger = LogManager.getLogger(BaseLoadBalancer.class);

    private final Executor aliveCheckExecutor;

    private final Executor timeoutExecutor;

    private final Timer providerAliveTimer;

    private final Long aliveTimeout;

    protected List<Provider> providers;

    protected Map<Provider, Integer> alivePings;

    protected BaseLoadBalancer(Long checkAliveInterval, Long aliveTimeout) {

        // Sanity check
        if (checkAliveInterval <= 0) {
            throw new IllegalArgumentException("Check alive interval must be greater than 0!");
        }
        if (checkAliveInterval <= 0) {
            throw new IllegalArgumentException(
                    "Provider alive check timeout must be greater than 0!");
        }
        if (aliveTimeout >= checkAliveInterval) {
            throw new IllegalArgumentException(
                    "Check alive interval must be greater than provider alive check timeout!");
        }

        providers = Collections.synchronizedList(new ArrayList<>());
        alivePings = Collections.synchronizedMap(new HashMap<Provider, Integer>());

        this.aliveTimeout = aliveTimeout;

        // Create the thread pools for alive provider checks
        aliveCheckExecutor = Executors.newCachedThreadPool();
        timeoutExecutor = Executors.newCachedThreadPool();

        // Create a timer that will check from time to time if the providers are alive
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                logger.debug("Time {} fired!", providerAliveTimer);
                timerFired();
            }
        };

        providerAliveTimer =
                new Timer(MessageFormat.format("provider_alive_timer_{0}", UUID.randomUUID()));
        providerAliveTimer.scheduleAtFixedRate(timerTask, checkAliveInterval, checkAliveInterval);
    }

    public void registerProviders(List<Provider> providers) {

        // Replace existing providers
        this.providers = Collections.synchronizedList(providers);
        this.alivePings = Collections.synchronizedMap(
                providers.stream().collect(Collectors.toMap(Function.identity(), provider -> 0)));
    }

    public void addProvider(Provider provider) throws ProviderAlreadyRegisteredException {

        // Sanity check
        if (alivePings.containsKey(provider)) {
            throw new ProviderAlreadyRegisteredException(
                    MessageFormat.format("Provider {0} already registered!", provider));
        }

        providers.add(provider);
        alivePings.put(provider, 0);
    }

    public void removeProvider(Provider provider) throws ProviderNotFoundException {

        // Sanity check
        if (!alivePings.containsKey(provider)) {
            throw new ProviderNotFoundException(MessageFormat
                    .format("Provider {0} not registered on this Load Balancer!", provider));
        }

        providers.remove(provider);
        alivePings.remove(provider);
    }

    private void timerFired() {

        // Check if the provider is alive in a dedicated thread (one thread per provider)
        for (Entry<Provider, Integer> entry : alivePings.entrySet()) {

            aliveCheckExecutor.execute(() -> {
                Boolean result = false;

                try {
                    result = CompletableFuture.supplyAsync(entry.getKey()::check, timeoutExecutor)
                            .get(aliveTimeout, TimeUnit.MILLISECONDS);
                    logger.debug("Provider {} still alive!", entry.getKey());
                } catch (InterruptedException e) {
                    logger.error("Timer {} thread interrupted!", providerAliveTimer);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | TimeoutException e) {
                    logger.error("Provider {} not responding! Marking it as not alive!",
                            entry.getKey());
                } finally {

                    if (Boolean.TRUE.equals(result)) {

                        // Increment the count of subsequent successful pings
                        alivePings.put(entry.getKey(), entry.getValue() + 1);
                    } else {

                        // Reset the number of pings
                        alivePings.put(entry.getKey(), PROVIDER_UNAVAILABLE_RESET_PINGS);
                    }
                }
            });
        }
    }
}
