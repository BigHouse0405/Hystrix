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
package com.netflix.hystrix.contrib.codahalemetricspublisher;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCommand;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Func0;

/**
 * Implementation of {@link HystrixMetricsPublisherCommand} using Coda Hale Metrics (https://github.com/codahale/metrics)
 */
public class HystrixCodaHaleMetricsPublisherCommand implements HystrixMetricsPublisherCommand {
    private final HystrixCommandKey key;
    private final HystrixCommandGroupKey commandGroupKey;
    private final HystrixCommandMetrics metrics;
    private final HystrixCircuitBreaker circuitBreaker;
    private final HystrixCommandProperties properties;
    private final MetricRegistry metricRegistry;
    private final String metricGroup;
    private final String metricType;

    static final Logger logger = LoggerFactory.getLogger(HystrixCodaHaleMetricsPublisherCommand.class);

    public HystrixCodaHaleMetricsPublisherCommand(HystrixCommandKey commandKey, HystrixCommandGroupKey commandGroupKey, HystrixCommandMetrics metrics, HystrixCircuitBreaker circuitBreaker, HystrixCommandProperties properties, MetricRegistry metricRegistry) {
        this.key = commandKey;
        this.commandGroupKey = commandGroupKey;
        this.metrics = metrics;
        this.circuitBreaker = circuitBreaker;
        this.properties = properties;
        this.metricRegistry = metricRegistry;
        this.metricGroup = commandGroupKey.name();
        this.metricType = key.name();
    }

    /**
     * An implementation note.  If there's a version mismatch between hystrix-core and hystrix-codahale-metrics-publisher,
     * the code below may reference a HystrixRollingNumberEvent that does not exist in hystrix-core.  If this happens,
     * a j.l.NoSuchFieldError occurs.  Since this data is not being generated by hystrix-core, it's safe to count it as 0
     * and we should log an error to get users to update their dependency set.
     */
    @Override
    public void initialize() {
        metricRegistry.register(createMetricName("isCircuitBreakerOpen"), new Gauge<Boolean>() {
            @Override
            public Boolean getValue() {
                return circuitBreaker.isOpen();
            }
        });

        // allow monitor to know exactly at what point in time these stats are for so they can be plotted accurately
        metricRegistry.register(createMetricName("currentTime"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return System.currentTimeMillis();
            }
        });

        // cumulative counts
        safelyCreateCumulativeCountForEvent("countBadRequests", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.BAD_REQUEST;
            }
        });
        safelyCreateCumulativeCountForEvent("countCollapsedRequests", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.COLLAPSED;
            }
        });
        safelyCreateCumulativeCountForEvent("countEmit", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.EMIT;
            }
        });
        safelyCreateCumulativeCountForEvent("countExceptionsThrown", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.EXCEPTION_THROWN;
            }
        });
        safelyCreateCumulativeCountForEvent("countFailure", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FAILURE;
            }
        });
        safelyCreateCumulativeCountForEvent("countFallbackEmit", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_EMIT;
            }
        });
        safelyCreateCumulativeCountForEvent("countFallbackFailure", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_FAILURE;
            }
        });
        safelyCreateCumulativeCountForEvent("countFallbackMissing", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_MISSING;
            }
        });
        safelyCreateCumulativeCountForEvent("countFallbackRejection", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_REJECTION;
            }
        });
        safelyCreateCumulativeCountForEvent("countFallbackSuccess", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_SUCCESS;
            }
        });
        safelyCreateCumulativeCountForEvent("countResponsesFromCache", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.RESPONSE_FROM_CACHE;
            }
        });
        safelyCreateCumulativeCountForEvent("countSemaphoreRejected", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.SEMAPHORE_REJECTED;
            }
        });
        safelyCreateCumulativeCountForEvent("countShortCircuited", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.SHORT_CIRCUITED;
            }
        });
        safelyCreateCumulativeCountForEvent("countSuccess", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.SUCCESS;
            }
        });
        safelyCreateCumulativeCountForEvent("countThreadPoolRejected", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.THREAD_POOL_REJECTED;
            }
        });
        safelyCreateCumulativeCountForEvent("countTimeout", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.TIMEOUT;
            }
        });

        // rolling counts
        safelyCreateRollingCountForEvent("rollingCountBadRequests", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.BAD_REQUEST;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountCollapsedRequests", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.COLLAPSED;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountEmit", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.EMIT;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountExceptionsThrown", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.EXCEPTION_THROWN;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountFailure", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FAILURE;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountFallbackEmit", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_EMIT;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountFallbackFailure", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_FAILURE;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountFallbackMissing", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_MISSING;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountFallbackRejection", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_REJECTION;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountFallbackSuccess", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.FALLBACK_SUCCESS;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountResponsesFromCache", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.RESPONSE_FROM_CACHE;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountSemaphoreRejected", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.SEMAPHORE_REJECTED;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountShortCircuited", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.SHORT_CIRCUITED;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountSuccess", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.SUCCESS;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountThreadPoolRejected", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.THREAD_POOL_REJECTED;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountTimeout", new Func0<HystrixRollingNumberEvent>() {
            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.TIMEOUT;
            }
        });

        // the number of executionSemaphorePermits in use right now
        metricRegistry.register(createMetricName("executionSemaphorePermitsInUse"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getCurrentConcurrentExecutionCount();
            }
        });

        // error percentage derived from current metrics
        metricRegistry.register(createMetricName("errorPercentage"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getHealthCounts().getErrorPercentage();
            }
        });

        // latency metrics
        metricRegistry.register(createMetricName("latencyExecute_mean"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getExecutionTimeMean();
            }
        });
        metricRegistry.register(createMetricName("latencyExecute_percentile_5"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getExecutionTimePercentile(5);
            }
        });
        metricRegistry.register(createMetricName("latencyExecute_percentile_25"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getExecutionTimePercentile(25);
            }
        });
        metricRegistry.register(createMetricName("latencyExecute_percentile_50"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getExecutionTimePercentile(50);
            }
        });
        metricRegistry.register(createMetricName("latencyExecute_percentile_75"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getExecutionTimePercentile(75);
            }
        });
        metricRegistry.register(createMetricName("latencyExecute_percentile_90"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getExecutionTimePercentile(90);
            }
        });
        metricRegistry.register(createMetricName("latencyExecute_percentile_99"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getExecutionTimePercentile(99);
            }
        });
        metricRegistry.register(createMetricName("latencyExecute_percentile_995"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getExecutionTimePercentile(99.5);
            }
        });

        metricRegistry.register(createMetricName("latencyTotal_mean"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getTotalTimeMean();
            }
        });
        metricRegistry.register(createMetricName("latencyTotal_percentile_5"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getTotalTimePercentile(5);
            }
        });
        metricRegistry.register(createMetricName("latencyTotal_percentile_25"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getTotalTimePercentile(25);
            }
        });
        metricRegistry.register(createMetricName("latencyTotal_percentile_50"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getTotalTimePercentile(50);
            }
        });
        metricRegistry.register(createMetricName("latencyTotal_percentile_75"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getTotalTimePercentile(75);
            }
        });
        metricRegistry.register(createMetricName("latencyTotal_percentile_90"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getTotalTimePercentile(90);
            }
        });
        metricRegistry.register(createMetricName("latencyTotal_percentile_99"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getTotalTimePercentile(99);
            }
        });
        metricRegistry.register(createMetricName("latencyTotal_percentile_995"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getTotalTimePercentile(99.5);
            }
        });

        // group
        metricRegistry.register(createMetricName("commandGroup"), new Gauge<String>() {
            @Override
            public String getValue() {
                return commandGroupKey != null ? commandGroupKey.name() : null;
            }
        });

        // properties (so the values can be inspected and monitored)
        metricRegistry.register(createMetricName("propertyValue_rollingStatisticalWindowInMilliseconds"), new Gauge<Number>() {
            @Override
            public Number getValue() {
                return properties.metricsRollingStatisticalWindowInMilliseconds().get();
            }
        });
        metricRegistry.register(createMetricName("propertyValue_circuitBreakerRequestVolumeThreshold"), new Gauge<Number>() {
            @Override
            public Number getValue() {
                return properties.circuitBreakerRequestVolumeThreshold().get();
            }
        });
        metricRegistry.register(createMetricName("propertyValue_circuitBreakerSleepWindowInMilliseconds"), new Gauge<Number>() {
            @Override
            public Number getValue() {
                return properties.circuitBreakerSleepWindowInMilliseconds().get();
            }
        });
        metricRegistry.register(createMetricName("propertyValue_circuitBreakerErrorThresholdPercentage"), new Gauge<Number>() {
            @Override
            public Number getValue() {
                return properties.circuitBreakerErrorThresholdPercentage().get();
            }
        });
        metricRegistry.register(createMetricName("propertyValue_circuitBreakerForceOpen"), new Gauge<Boolean>() {
            @Override
            public Boolean getValue() {
                return properties.circuitBreakerForceOpen().get();
            }
        });
        metricRegistry.register(createMetricName("propertyValue_circuitBreakerForceClosed"), new Gauge<Boolean>() {
            @Override
            public Boolean getValue() {
                return properties.circuitBreakerForceClosed().get();
            }
        });
        metricRegistry.register(createMetricName("propertyValue_executionIsolationThreadTimeoutInMilliseconds"), new Gauge<Number>() {
            @Override
            public Number getValue() {
                return properties.executionTimeoutInMilliseconds().get();
            }
        });
        metricRegistry.register(createMetricName("propertyValue_executionTimeoutInMilliseconds"), new Gauge<Number>() {
            @Override
            public Number getValue() {
                return properties.executionTimeoutInMilliseconds().get();
            }
        });
        metricRegistry.register(createMetricName("propertyValue_executionIsolationStrategy"), new Gauge<String>() {
            @Override
            public String getValue() {
                return properties.executionIsolationStrategy().get().name();
            }
        });
        metricRegistry.register(createMetricName("propertyValue_metricsRollingPercentileEnabled"), new Gauge<Boolean>() {
            @Override
            public Boolean getValue() {
                return properties.metricsRollingPercentileEnabled().get();
            }
        });
        metricRegistry.register(createMetricName("propertyValue_requestCacheEnabled"), new Gauge<Boolean>() {
            @Override
            public Boolean getValue() {
                return properties.requestCacheEnabled().get();
            }
        });
        metricRegistry.register(createMetricName("propertyValue_requestLogEnabled"), new Gauge<Boolean>() {
            @Override
            public Boolean getValue() {
                return properties.requestLogEnabled().get();
            }
        });
        metricRegistry.register(createMetricName("propertyValue_executionIsolationSemaphoreMaxConcurrentRequests"), new Gauge<Number>() {
            @Override
            public Number getValue() {
                return properties.executionIsolationSemaphoreMaxConcurrentRequests().get();
            }
        });
        metricRegistry.register(createMetricName("propertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests"), new Gauge<Number>() {
            @Override
            public Number getValue() {
                return properties.fallbackIsolationSemaphoreMaxConcurrentRequests().get();
            }
        });
    }

    protected String createMetricName(String name) {
        return MetricRegistry.name(metricGroup, metricType, name);
    }

    protected void createCumulativeCountForEvent(final String name, final HystrixRollingNumberEvent event) {
        metricRegistry.register(createMetricName(name), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return metrics.getCumulativeCount(event);
            }
        });
    }

    protected void safelyCreateCumulativeCountForEvent(final String name, final Func0<HystrixRollingNumberEvent> eventThunk) {
        metricRegistry.register(createMetricName(name), new Gauge<Long>() {
            @Override
            public Long getValue() {
                try {
                    return metrics.getCumulativeCount(eventThunk.call());
                } catch (NoSuchFieldError error) {
                    logger.error("While publishing CodaHale metrics, error looking up eventType for : " + name + ".  Please check that all Hystrix versions are the same!");
                    return 0L;
                }
            }
        });
    }

    protected void createRollingCountForEvent(final String name, final HystrixRollingNumberEvent event) {
        metricRegistry.register(createMetricName(name), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return metrics.getRollingCount(event);
            }
        });
    }

    protected void safelyCreateRollingCountForEvent(final String name, final Func0<HystrixRollingNumberEvent> eventThunk) {
        metricRegistry.register(createMetricName(name), new Gauge<Long>() {
            @Override
            public Long getValue() {
                try {
                    return metrics.getRollingCount(eventThunk.call());
                } catch (NoSuchFieldError error) {
                    logger.error("While publishing CodaHale metrics, error looking up eventType for : " + name + ".  Please check that all Hystrix versions are the same!");
                    return 0L;
                }
            }
        });
    }
}
