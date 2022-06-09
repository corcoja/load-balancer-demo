package com.corcoja.demo.protocol;

import java.util.List;
import com.corcoja.demo.error.MaxLoadException;
import com.corcoja.demo.error.ProviderAlreadyRegisteredException;
import com.corcoja.demo.error.ProviderNotFoundException;

public interface LoadBalancer {

    /**
     * Register a list of provider instances to the Load Balancer. The previous providers will be
     * overwritten.
     * 
     * @param providers The list of providers
     */
    void registerProviders(List<Provider> providers);

    /**
     * Registers a new provider on the Load Balancer.
     * 
     * @param provider New provider to be registered.
     * 
     * @throws ProviderAlreadyRegisteredException Thrown if the provider is already registered with
     *         this Load Balancer.
     */
    void addProvider(Provider provider) throws ProviderAlreadyRegisteredException;

    /**
     * Unregister an existing provider from the Load Balancer.
     * 
     * @param provider The provider to be unregistered.
     * 
     * @throws ProviderNotFoundException Thrown if the provider has not been found (i. e. not
     *         registered on the Load Balancer).
     */
    void removeProvider(Provider provider) throws ProviderNotFoundException;

    /**
     * Passes the {@code get} request to one of the registered providers.
     * 
     * @return Unique identifier.
     * 
     * @throws MaxLoadException Thrown if all registered providers are at their maximum load.
     */
    String get() throws MaxLoadException;
}
