/**
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.hystrix.strategy.metrics;

import com.netflix.hystrix.HystrixCollapserKey;
import com.netflix.hystrix.HystrixCollapserMetrics;
import com.netflix.hystrix.HystrixCollapserProperties;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Default implementation of {@link HystrixMetricsCollection}.
 * <p>
 * See <a href="https://github.com/Netflix/Hystrix/wiki/Plugins">Wiki docs</a> about plugins for more information.
 * 
 * @ExcludeFromJavadoc
 */
public class HystrixMetricsCollectionDefault extends HystrixMetricsCollection {

    private static HystrixMetricsCollectionDefault INSTANCE = new HystrixMetricsCollectionDefault();

    public static HystrixMetricsCollection getInstance() {
        return INSTANCE;
    }

    private HystrixMetricsCollectionDefault() {
    }

    @Override
    public HystrixCommandMetrics getCommandMetricsInstance(HystrixCommandKey key, HystrixCommandGroupKey commandGroup, HystrixThreadPoolKey threadPoolKey, HystrixCommandProperties properties, HystrixEventNotifier eventNotifier) {
        return new HystrixCommandMetricsSummary(key, commandGroup, threadPoolKey, properties, eventNotifier);
    }

    @Override
    public HystrixThreadPoolMetrics getThreadPoolMetricsInstance(HystrixThreadPoolKey key, ThreadPoolExecutor threadPool, HystrixThreadPoolProperties properties) {
        return new HystrixThreadPoolMetricsSummary(key, threadPool, properties);
    }

    @Override
    public HystrixCollapserMetrics getCollapserMetricsInstance(HystrixCollapserKey key, HystrixCollapserProperties properties) {
        return new HystrixCollapserMetricsSummary(key, properties);
    }
}
