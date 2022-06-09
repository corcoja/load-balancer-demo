package com.corcoja.demo.protocol;

public interface Provider {

    /**
     * Returns an unique identifier of the provider instance.
     * 
     * @return Unique identifier.
     * 
     * @note This method will return immediately if the load on the provider allows this, otherwise,
     *       for simulation purposes it will block the execution until the load decreases.
     */
    String get();

    /**
     * Check if the provider is alive or not.
     * 
     * @return {@code True} if alive, {@code False} otherwise.
     * 
     * @note This method will return immediately if the load on the provider allows this, otherwise,
     *       for simulation purposes it will block the execution until the load decreases.
     */
    Boolean check();

    /**
     * Returns the load percentage on the provider.
     * 
     * @return A {@code Float} value between {@code 0.0}, meaning 0% (no load) and {@code 1.0},
     *         meaning 100% (full load).
     * 
     * @note Will always return immediately the provider's load.
     */
    Float getCurrentLoad();

    /**
     * Sets the availability of the provider. To simulate a provider that currently is not available
     * (e. g. overloaded with requests, shut down, crashed, restarting, etc.), set the availability
     * to {@code False}. If set to {@code True}, the availability of the provider will be defined by
     * its simulated load (see {@link #get()}, {@link #check()} and {@link #getCurrentLoad()})
     * 
     * @param availability The availability to be set.
     */
    void setAvailability(Boolean availability);
}
