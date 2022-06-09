package com.corcoja.demo.impl;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import com.corcoja.demo.protocol.Provider;

public class SimpleProvider implements Provider {

    private static Logger logger = LogManager.getLogger(SimpleProvider.class);

    private final Integer maxConcurrentRequests;

    private final Duration oneRequestProcessingTime;

    private final String uuid;

    private AtomicInteger currentRequests = new AtomicInteger();

    private Lock availabilityLock = new ReentrantLock();

    private Semaphore semaphore;

    /**
     * Create a new instance of {@link SimpleProvider}. Each time this provider is invoked, it will
     * create an artificial load on it by sleeping for {@link #oneRequestProcessingTime}. Creating
     * {@link #maxConcurrentRequests} requests at the same time, will make the provider
     * unavailable/unresponsive until all requests finish their artificial processing, i. e. at most
     * {@link #oneRequestProcessingTime}.
     * 
     * @param maxConcurrentRequests Max concurrent requests that this provider can handle.
     * @param oneRequestProcessingTime One request artificial processing time.
     */
    public SimpleProvider(Integer maxConcurrentRequests, Duration oneRequestProcessingTime) {
        this(UUID.randomUUID().toString(), maxConcurrentRequests, oneRequestProcessingTime);
    }

    /**
     * Create a new instance of {@link SimpleProvider}. Each time this provider is invoked, it will
     * create an artificial load on it by sleeping for {@link #oneRequestProcessingTime}. Creating
     * {@link #maxConcurrentRequests} requests at the same time, will make the provider
     * unavailable/unresponsive until all requests finish their artificial processing, i. e. at most
     * {@link #oneRequestProcessingTime}.
     * 
     * @param customUuid Custom UUID for this provider.
     * @param maxConcurrentRequests Max concurrent requests that this provider can handle.
     * @param oneRequestProcessingTime One request artificial processing time.
     */
    public SimpleProvider(String customUuid, Integer maxConcurrentRequests,
            Duration oneRequestProcessingTime) {

        // Sanity check
        if (Strings.isEmpty(customUuid)) {
            throw new IllegalArgumentException("UUID cannot be `null` or empty!");
        }
        if (maxConcurrentRequests <= 0) {
            throw new IllegalArgumentException(
                    "Maximum allowed concurrent requests must be greater than zero!");
        }
        if (oneRequestProcessingTime.isNegative()) {
            throw new IllegalArgumentException("Processing time for one request must be positive!");
        }

        this.uuid = customUuid;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.oneRequestProcessingTime = oneRequestProcessingTime;
        this.semaphore = new Semaphore(maxConcurrentRequests);
    }

    @Override
    public String get() {

        availabilityLock.lock();

        // Acquire the semaphore and increase the load by incrementing the current number of
        // concurrent requests
        semaphore.acquireUninterruptibly();
        currentRequests.incrementAndGet();

        // Create a timer that will release the semaphore after pre-defined processing time
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                semaphore.release();
                currentRequests.decrementAndGet();
            }
        };
        Timer timer = new Timer(MessageFormat.format("timer_{0}", UUID.randomUUID()));
        timer.schedule(task, oneRequestProcessingTime.toMillis());

        availabilityLock.unlock();
        return uuid;
    }

    @Override
    public Boolean check() {

        availabilityLock.lock();
        semaphore.acquireUninterruptibly();
        semaphore.release();
        availabilityLock.unlock();

        return true;
    }

    @Override
    public Float getCurrentLoad() {
        return (float) currentRequests.get() / (float) maxConcurrentRequests;
    }

    @Override
    public void setAvailability(Boolean availability) {

        if (Boolean.FALSE.equals(availability)) {
            if (!availabilityLock.tryLock()) {
                logger.warn("Availability already set to `true`.");
            }
        } else {
            availabilityLock.unlock();
        }
    }
}
