/**
 * Copyright 2012 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix.strategy;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategyDefault;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifierDefault;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHookDefault;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherDefault;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherFactory;
import com.netflix.hystrix.strategy.properties.HystrixArchaiusHelper;
import com.netflix.hystrix.strategy.properties.HystrixDynamicProperties;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategyDefault;

/**
 * Registry for plugin implementations that allows global override and handles the retrieval of correct implementation based on order of precedence:
 * <ol>
 * <li>plugin registered globally via <code>register</code> methods in this class</li>
 * <li>plugin registered and retrieved using the resolved {@link HystrixDynamicProperties} (usually Archaius, see get methods for property names)</li>
 * <li>plugin registered and retrieved using the JDK {@link ServiceLoader}</li>
 * <li>default implementation</li>
 * </ol>
 * 
 * The exception to the above order is the {@link HystrixDynamicProperties} implmentation 
 * which is only loaded through the ServiceLoader (see the {@link HystrixPlugins#getDynamicProperties() getter} for more details).
 * <p>
 * See the Hystrix GitHub Wiki for more information: <a href="https://github.com/Netflix/Hystrix/wiki/Plugins">https://github.com/Netflix/Hystrix/wiki/Plugins</a>.
 */
public class HystrixPlugins {

    private final static HystrixPlugins INSTANCE = new HystrixPlugins();

    /* package */ final AtomicReference<HystrixEventNotifier> notifier = new AtomicReference<HystrixEventNotifier>();
    /* package */ final AtomicReference<HystrixConcurrencyStrategy> concurrencyStrategy = new AtomicReference<HystrixConcurrencyStrategy>();
    /* package */ final AtomicReference<HystrixMetricsPublisher> metricsPublisher = new AtomicReference<HystrixMetricsPublisher>();
    /* package */ final AtomicReference<HystrixPropertiesStrategy> propertiesFactory = new AtomicReference<HystrixPropertiesStrategy>();
    /* package */ final AtomicReference<HystrixCommandExecutionHook> commandExecutionHook = new AtomicReference<HystrixCommandExecutionHook>();
    /* package */ final HystrixDynamicProperties dynamicProperties;

    private HystrixPlugins() {
        //This will load Archaius if its in the classpath.
        dynamicProperties = resolveDynamicProperties();
    }

    public static HystrixPlugins getInstance() {
        return INSTANCE;
    }

    /**
     * Reset all of the HystrixPlugins to null.  You may invoke this directly, or it also gets invoked via <code>Hystrix.reset()</code>
     */
    public static void reset() {
        getInstance().notifier.set(null);
        getInstance().concurrencyStrategy.set(null);
        getInstance().metricsPublisher.set(null);
        getInstance().propertiesFactory.set(null);
        getInstance().commandExecutionHook.set(null);
        HystrixMetricsPublisherFactory.reset();
    }

    /**
     * Retrieve instance of {@link HystrixEventNotifier} to use based on order of precedence as defined in {@link HystrixPlugins} class header.
     * <p>
     * Override default by using {@link #registerEventNotifier(HystrixEventNotifier)} or setting property (via Archaius): <code>hystrix.plugin.HystrixEventNotifier.implementation</code> with the full classname to
     * load.
     * 
     * @return {@link HystrixEventNotifier} implementation to use
     */
    public HystrixEventNotifier getEventNotifier() {
        if (notifier.get() == null) {
            // check for an implementation from Archaius first
            Object impl = getPluginImplementation(HystrixEventNotifier.class);
            if (impl == null) {
                // nothing set via Archaius so initialize with default
                notifier.compareAndSet(null, HystrixEventNotifierDefault.getInstance());
                // we don't return from here but call get() again in case of thread-race so the winner will always get returned
            } else {
                // we received an implementation from Archaius so use it
                notifier.compareAndSet(null, (HystrixEventNotifier) impl);
            }
        }
        return notifier.get();
    }

    /**
     * Register a {@link HystrixEventNotifier} implementation as a global override of any injected or default implementations.
     * 
     * @param impl
     *            {@link HystrixEventNotifier} implementation
     * @throws IllegalStateException
     *             if called more than once or after the default was initialized (if usage occurs before trying to register)
     */
    public void registerEventNotifier(HystrixEventNotifier impl) {
        if (!notifier.compareAndSet(null, impl)) {
            throw new IllegalStateException("Another strategy was already registered.");
        }
    }

    /**
     * Retrieve instance of {@link HystrixConcurrencyStrategy} to use based on order of precedence as defined in {@link HystrixPlugins} class header.
     * <p>
     * Override default by using {@link #registerConcurrencyStrategy(HystrixConcurrencyStrategy)} or setting property (via Archaius): <code>hystrix.plugin.HystrixConcurrencyStrategy.implementation</code> with the
     * full classname to load.
     * 
     * @return {@link HystrixConcurrencyStrategy} implementation to use
     */
    public HystrixConcurrencyStrategy getConcurrencyStrategy() {
        if (concurrencyStrategy.get() == null) {
            // check for an implementation from Archaius first
            Object impl = getPluginImplementation(HystrixConcurrencyStrategy.class);
            if (impl == null) {
                // nothing set via Archaius so initialize with default
                concurrencyStrategy.compareAndSet(null, HystrixConcurrencyStrategyDefault.getInstance());
                // we don't return from here but call get() again in case of thread-race so the winner will always get returned
            } else {
                // we received an implementation from Archaius so use it
                concurrencyStrategy.compareAndSet(null, (HystrixConcurrencyStrategy) impl);
            }
        }
        return concurrencyStrategy.get();
    }

    /**
     * Register a {@link HystrixConcurrencyStrategy} implementation as a global override of any injected or default implementations.
     * 
     * @param impl
     *            {@link HystrixConcurrencyStrategy} implementation
     * @throws IllegalStateException
     *             if called more than once or after the default was initialized (if usage occurs before trying to register)
     */
    public void registerConcurrencyStrategy(HystrixConcurrencyStrategy impl) {
        if (!concurrencyStrategy.compareAndSet(null, impl)) {
            throw new IllegalStateException("Another strategy was already registered.");
        }
    }

    /**
     * Retrieve instance of {@link HystrixMetricsPublisher} to use based on order of precedence as defined in {@link HystrixPlugins} class header.
     * <p>
     * Override default by using {@link #registerMetricsPublisher(HystrixMetricsPublisher)} or setting property (via Archaius): <code>hystrix.plugin.HystrixMetricsPublisher.implementation</code> with the full
     * classname to load.
     * 
     * @return {@link HystrixMetricsPublisher} implementation to use
     */
    public HystrixMetricsPublisher getMetricsPublisher() {
        if (metricsPublisher.get() == null) {
            // check for an implementation from Archaius first
            Object impl = getPluginImplementation(HystrixMetricsPublisher.class);
            if (impl == null) {
                // nothing set via Archaius so initialize with default
                metricsPublisher.compareAndSet(null, HystrixMetricsPublisherDefault.getInstance());
                // we don't return from here but call get() again in case of thread-race so the winner will always get returned
            } else {
                // we received an implementation from Archaius so use it
                metricsPublisher.compareAndSet(null, (HystrixMetricsPublisher) impl);
            }
        }
        return metricsPublisher.get();
    }

    /**
     * Register a {@link HystrixMetricsPublisher} implementation as a global override of any injected or default implementations.
     * 
     * @param impl
     *            {@link HystrixMetricsPublisher} implementation
     * @throws IllegalStateException
     *             if called more than once or after the default was initialized (if usage occurs before trying to register)
     */
    public void registerMetricsPublisher(HystrixMetricsPublisher impl) {
        if (!metricsPublisher.compareAndSet(null, impl)) {
            throw new IllegalStateException("Another strategy was already registered.");
        }
    }

    /**
     * Retrieve instance of {@link HystrixPropertiesStrategy} to use based on order of precedence as defined in {@link HystrixPlugins} class header.
     * <p>
     * Override default by using {@link #registerPropertiesStrategy(HystrixPropertiesStrategy)} or setting property (via Archaius): <code>hystrix.plugin.HystrixPropertiesStrategy.implementation</code> with the full
     * classname to load.
     * 
     * @return {@link HystrixPropertiesStrategy} implementation to use
     */
    public HystrixPropertiesStrategy getPropertiesStrategy() {
        if (propertiesFactory.get() == null) {
            // check for an implementation from Archaius first
            Object impl = getPluginImplementation(HystrixPropertiesStrategy.class);
            if (impl == null) {
                // nothing set via Archaius so initialize with default
                propertiesFactory.compareAndSet(null, HystrixPropertiesStrategyDefault.getInstance());
                // we don't return from here but call get() again in case of thread-race so the winner will always get returned
            } else {
                // we received an implementation from Archaius so use it
                propertiesFactory.compareAndSet(null, (HystrixPropertiesStrategy) impl);
            }
        }
        return propertiesFactory.get();
    }
    
    /**
     * Retrieves the instance of {@link HystrixDynamicProperties} to use.
     * <p>
     * Unlike the other plugins this plugin cannot be re-registered and is only loaded at creation 
     * of the {@link HystrixPlugins} singleton.
     * <p>
     * The order of precedence for loading implementations is:
     * <ol>
     * <li>The {@link ServiceLoader}.</li>
     * <li>An implementation based on Archaius if it is found in the classpath it is used.</li>
     * <li>An implementation based on the {@link System#getProperties()}</li>
     * </ol>
     * @return never <code>null</code>
     */
    public HystrixDynamicProperties getDynamicProperties() {
        return dynamicProperties;
    }

    /**
     * Register a {@link HystrixPropertiesStrategy} implementation as a global override of any injected or default implementations.
     * 
     * @param impl
     *            {@link HystrixPropertiesStrategy} implementation
     * @throws IllegalStateException
     *             if called more than once or after the default was initialized (if usage occurs before trying to register)
     */
    public void registerPropertiesStrategy(HystrixPropertiesStrategy impl) {
        if (!propertiesFactory.compareAndSet(null, impl)) {
            throw new IllegalStateException("Another strategy was already registered.");
        }
    }

    /**
     * Retrieve instance of {@link HystrixCommandExecutionHook} to use based on order of precedence as defined in {@link HystrixPlugins} class header.
     * <p>
     * Override default by using {@link #registerCommandExecutionHook(HystrixCommandExecutionHook)} or setting property (via Archaius): <code>hystrix.plugin.HystrixCommandExecutionHook.implementation</code> with the
     * full classname to
     * load.
     * 
     * @return {@link HystrixCommandExecutionHook} implementation to use
     * 
     * @since 1.2
     */
    public HystrixCommandExecutionHook getCommandExecutionHook() {
        if (commandExecutionHook.get() == null) {
            // check for an implementation from Archaius first
            Object impl = getPluginImplementation(HystrixCommandExecutionHook.class);
            if (impl == null) {
                // nothing set via Archaius so initialize with default
                commandExecutionHook.compareAndSet(null, HystrixCommandExecutionHookDefault.getInstance());
                // we don't return from here but call get() again in case of thread-race so the winner will always get returned
            } else {
                // we received an implementation from Archaius so use it
                commandExecutionHook.compareAndSet(null, (HystrixCommandExecutionHook) impl);
            }
        }
        return commandExecutionHook.get();
    }

    /**
     * Register a {@link HystrixCommandExecutionHook} implementation as a global override of any injected or default implementations.
     * 
     * @param impl
     *            {@link HystrixCommandExecutionHook} implementation
     * @throws IllegalStateException
     *             if called more than once or after the default was initialized (if usage occurs before trying to register)
     * 
     * @since 1.2
     */
    public void registerCommandExecutionHook(HystrixCommandExecutionHook impl) {
        if (!commandExecutionHook.compareAndSet(null, impl)) {
            throw new IllegalStateException("Another strategy was already registered.");
        }
    }

    
    private <T> T getPluginImplementation(Class<T> pluginClass) {
        T p = getPluginImplementationViaProperties(pluginClass);
        if (p != null) return p;
        ServiceLoader<T> sl = ServiceLoader.load(pluginClass);
        for(T plugin : sl) {
            if (plugin != null) return plugin;
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getPluginImplementationViaProperties(Class<T> pluginClass) {
        String classSimpleName = pluginClass.getSimpleName();
        // Check Archaius for plugin class.
        String propertyName = "hystrix.plugin." + classSimpleName + ".implementation";
        String implementingClass = dynamicProperties.getString(propertyName, null).get();
        if (implementingClass != null) {
            try {
                Class<?> cls = Class.forName(implementingClass);
                // narrow the scope (cast) to the type we're expecting
                cls = cls.asSubclass(pluginClass);
                return (T) cls.newInstance();
            } catch (ClassCastException e) {
                throw new RuntimeException(classSimpleName + " implementation is not an instance of " + classSimpleName + ": " + implementingClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(classSimpleName + " implementation class not found: " + implementingClass, e);
            } catch (InstantiationException e) {
                throw new RuntimeException(classSimpleName + " implementation not able to be instantiated: " + implementingClass, e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(classSimpleName + " implementation not able to be accessed: " + implementingClass, e);
            }
        } else {
            return null;
        }
    }
    
    
    
    private static HystrixDynamicProperties resolveDynamicProperties() {
        ServiceLoader<HystrixDynamicProperties> sl = ServiceLoader.load(HystrixDynamicProperties.class);
        for(HystrixDynamicProperties hp : sl) {
            if (hp != null) return hp;
        }
        return HystrixArchaiusHelper.createArchaiusDynamicProperties();
    }


}
