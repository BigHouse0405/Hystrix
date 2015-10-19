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
package com.netflix.hystrix.contrib.yammermetricspublisher;

import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCommand;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import rx.functions.Func0;

/**
 * Implementation of {@link HystrixMetricsPublisherCommand} using Yammer Metrics (https://github.com/codahale/metrics)
 */
public class HystrixYammerMetricsPublisherCommand implements HystrixMetricsPublisherCommand {
    private final HystrixCommandKey key;
    private final HystrixCommandGroupKey commandGroupKey;
    private final HystrixCommandMetrics metrics;
    private final HystrixCircuitBreaker circuitBreaker;
    private final HystrixCommandProperties properties;
    private final MetricsRegistry metricsRegistry;
    private final String metricGroup;
    private final String metricType;

    public HystrixYammerMetricsPublisherCommand(HystrixCommandKey commandKey, HystrixCommandGroupKey commandGroupKey, HystrixCommandMetrics metrics, HystrixCircuitBreaker circuitBreaker, HystrixCommandProperties properties, MetricsRegistry metricsRegistry) {
        this.key = commandKey;
        this.commandGroupKey = commandGroupKey;
        this.metrics = metrics;
        this.circuitBreaker = circuitBreaker;
        this.properties = properties;
        this.metricsRegistry = metricsRegistry;
        this.metricGroup = "HystrixCommand";
        this.metricType = key.name();
    }

    @Override
    public void initialize() {
        metricsRegistry.newGauge(createMetricName("isCircuitBreakerOpen"), new Gauge<Boolean>() {
            @Override
            public Boolean value() {
                return circuitBreaker.isOpen();
            }
        });

        // allow monitor to know exactly at what point in time these stats are for so they can be plotted accurately
        metricsRegistry.newGauge(createMetricName("currentTime"), new Gauge<Long>() {
            @Override
            public Long value() {
                return System.currentTimeMillis();
            }
        });

        // cumulative counts
        createCumulativeGauge("countBadRequests", HystrixEventType.BAD_REQUEST);
        createCumulativeGauge("countCollapsedRequests", HystrixEventType.COLLAPSED);
        createCumulativeGauge("countEmit", HystrixEventType.EMIT);
        createCumulativeGauge("countExceptionsThrown", HystrixEventType.EXCEPTION_THROWN);
        createCumulativeGauge("countFailure", HystrixEventType.FAILURE);
        createCumulativeGauge("countFallbackEmit", HystrixEventType.FALLBACK_EMIT);
        createCumulativeGauge("countFallbackFailure", HystrixEventType.FALLBACK_FAILURE);
        createCumulativeGauge("countFallbackMissing", HystrixEventType.FALLBACK_MISSING);
        createCumulativeGauge("countFallbackRejection", HystrixEventType.FALLBACK_REJECTION);
        createCumulativeGauge("countFallbackSuccess", HystrixEventType.FALLBACK_SUCCESS);
        createCumulativeGauge("countResponsesFromCache", HystrixEventType.RESPONSE_FROM_CACHE);
        createCumulativeGauge("countSemaphoreRejected", HystrixEventType.SEMAPHORE_REJECTED);
        createCumulativeGauge("countShortCircuited", HystrixEventType.SHORT_CIRCUITED);
        createCumulativeGauge("countSuccess", HystrixEventType.SUCCESS);
        createCumulativeGauge("countThreadPoolRejected", HystrixEventType.THREAD_POOL_REJECTED);
        createCumulativeGauge("countTimeout", HystrixEventType.TIMEOUT);

        // rolling counts
        createRollingGauge("rollingCountBadRequests", HystrixEventType.BAD_REQUEST);
        createRollingGauge("rollingCountCollapsedRequests", HystrixEventType.COLLAPSED);
        createRollingGauge("rollingCountExceptionsThrown", HystrixEventType.EXCEPTION_THROWN);
        createRollingGauge("rollingCountFailure", HystrixEventType.FAILURE);
        createRollingGauge("rollingCountFallbackFailure", HystrixEventType.FALLBACK_FAILURE);
        createRollingGauge("rollingCountFallbackMissing", HystrixEventType.FALLBACK_MISSING);
        createRollingGauge("rollingCountFallbackRejection", HystrixEventType.FALLBACK_REJECTION);
        createRollingGauge("rollingCountFallbackSuccess", HystrixEventType.FALLBACK_SUCCESS);
        createRollingGauge("rollingCountResponsesFromCache", HystrixEventType.RESPONSE_FROM_CACHE);
        createRollingGauge("rollingCountSemaphoreRejected", HystrixEventType.SEMAPHORE_REJECTED);
        createRollingGauge("rollingCountShortCircuited", HystrixEventType.SHORT_CIRCUITED);
        createRollingGauge("rollingCountSuccess", HystrixEventType.SUCCESS);
        createRollingGauge("rollingCountThreadPoolRejected", HystrixEventType.THREAD_POOL_REJECTED);
        createRollingGauge("rollingCountTimeout", HystrixEventType.TIMEOUT);

        // the number of executionSemaphorePermits in use right now 
        createCurrentValueGauge("executionSemaphorePermitsInUse", currentConcurrentExecutionCountThunk);

        // error percentage derived from current metrics 
        createCurrentValueGauge("errorPercentage", errorPercentageThunk);

        // latency metrics
        createExecutionLatencyMeanGauge("latencyExecute_mean");

        createExecutionLatencyPercentileGauge("latencyExecute_percentile_5", 5);
        createExecutionLatencyPercentileGauge("latencyExecute_percentile_25", 25);
        createExecutionLatencyPercentileGauge("latencyExecute_percentile_50", 50);
        createExecutionLatencyPercentileGauge("latencyExecute_percentile_75", 75);
        createExecutionLatencyPercentileGauge("latencyExecute_percentile_90", 90);
        createExecutionLatencyPercentileGauge("latencyExecute_percentile_99", 99);
        createExecutionLatencyPercentileGauge("latencyExecute_percentile_995", 99.5);

        createTotalLatencyMeanGauge("latencyTotal_mean");

        createTotalLatencyPercentileGauge("latencyTotal_percentile_5", 5);
        createTotalLatencyPercentileGauge("latencyTotal_percentile_25", 25);
        createTotalLatencyPercentileGauge("latencyTotal_percentile_50", 50);
        createTotalLatencyPercentileGauge("latencyTotal_percentile_75", 75);
        createTotalLatencyPercentileGauge("latencyTotal_percentile_90", 90);
        createTotalLatencyPercentileGauge("latencyTotal_percentile_99", 99);
        createTotalLatencyPercentileGauge("latencyTotal_percentile_995", 99.5);

        // group
        metricsRegistry.newGauge(createMetricName("commandGroup"), new Gauge<String>() {
            @Override
            public String value() {
                return commandGroupKey != null ? commandGroupKey.name() : null;
            }
        });

        // properties (so the values can be inspected and monitored)
        metricsRegistry.newGauge(createMetricName("propertyValue_rollingStatisticalWindowInMilliseconds"), new Gauge<Number>() {
            @Override
            public Number value() {
                return properties.metricsRollingStatisticalWindowInMilliseconds().get();
            }
        });
        metricsRegistry.newGauge(createMetricName("propertyValue_circuitBreakerRequestVolumeThreshold"), new Gauge<Number>() {
            @Override
            public Number value() {
                return properties.circuitBreakerRequestVolumeThreshold().get();
            }
        });
        metricsRegistry.newGauge(createMetricName("propertyValue_circuitBreakerSleepWindowInMilliseconds"), new Gauge<Number>() {
            @Override
            public Number value() {
                return properties.circuitBreakerSleepWindowInMilliseconds().get();
            }
        });
        metricsRegistry.newGauge(createMetricName("propertyValue_circuitBreakerErrorThresholdPercentage"), new Gauge<Number>() {
            @Override
            public Number value() {
                return properties.circuitBreakerErrorThresholdPercentage().get();
            }
        });
        metricsRegistry.newGauge(createMetricName("propertyValue_circuitBreakerForceOpen"), new Gauge<Boolean>() {
            @Override
            public Boolean value() {
                return properties.circuitBreakerForceOpen().get();
            }
        });
        metricsRegistry.newGauge(createMetricName("propertyValue_circuitBreakerForceClosed"), new Gauge<Boolean>() {
            @Override
            public Boolean value() {
                return properties.circuitBreakerForceClosed().get();
            }
        });
        metricsRegistry.newGauge(createMetricName("propertyValue_executionTimeoutInMilliseconds"), new Gauge<Number>() {
            @Override
            public Number value() {
                return properties.executionTimeoutInMilliseconds().get();
            }
        });
        metricsRegistry.newGauge(createMetricName("propertyValue_executionIsolationThreadTimeoutInMilliseconds"), new Gauge<Number>() {
            @Override
            public Number value() {
                return properties.executionTimeoutInMilliseconds().get();
            }
        });
        metricsRegistry.newGauge(createMetricName("propertyValue_executionIsolationStrategy"), new Gauge<String>() {
            @Override
            public String value() {
                return properties.executionIsolationStrategy().get().name();
            }
        });
        metricsRegistry.newGauge(createMetricName("propertyValue_metricsRollingPercentileEnabled"), new Gauge<Boolean>() {
            @Override
            public Boolean value() {
                return properties.metricsRollingPercentileEnabled().get();
            }
        });
        metricsRegistry.newGauge(createMetricName("propertyValue_requestCacheEnabled"), new Gauge<Boolean>() {
            @Override
            public Boolean value() {
                return properties.requestCacheEnabled().get();
            }
        });
        metricsRegistry.newGauge(createMetricName("propertyValue_requestLogEnabled"), new Gauge<Boolean>() {
            @Override
            public Boolean value() {
                return properties.requestLogEnabled().get();
            }
        });
        metricsRegistry.newGauge(createMetricName("propertyValue_executionIsolationSemaphoreMaxConcurrentRequests"), new Gauge<Number>() {
            @Override
            public Number value() {
                return properties.executionIsolationSemaphoreMaxConcurrentRequests().get();
            }
        });
        metricsRegistry.newGauge(createMetricName("propertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests"), new Gauge<Number>() {
            @Override
            public Number value() {
                return properties.fallbackIsolationSemaphoreMaxConcurrentRequests().get();
            }
        });
    }

    protected MetricName createMetricName(String name) {
        return new MetricName(metricGroup, metricType, name);
    }

    @Deprecated
    protected void createCumulativeCountForEvent(String name, final HystrixRollingNumberEvent event) {
        metricsRegistry.newGauge(createMetricName(name), new Gauge<Long>() {
            @Override
            public Long value() {
                return metrics.getCumulativeCount(event);
            }
        });
    }

    protected void createCumulativeGauge(final String name, final HystrixEventType eventType) {
        metricsRegistry.newGauge(createMetricName(name), new Gauge<Long>() {
            @Override
            public Long value() {
                return metrics.getCumulativeCount(HystrixRollingNumberEvent.from(eventType));
            }
        });
    }

    @Deprecated
    protected void createRollingGauge(String name, final HystrixRollingNumberEvent event) {
        metricsRegistry.newGauge(createMetricName(name), new Gauge<Long>() {
            @Override
            public Long value() {
                return metrics.getRollingCount(event);
            }
        });
    }

    protected void createRollingGauge(final String name, final HystrixEventType eventType) {
        metricsRegistry.newGauge(createMetricName(name), new Gauge<Long>() {
            @Override
            public Long value() {
                return metrics.getRollingCount(HystrixRollingNumberEvent.from(eventType));
            }
        });
    }

    protected void createExecutionLatencyMeanGauge(final String name) {
        metricsRegistry.newGauge(createMetricName(name), new Gauge<Integer>() {
            @Override
            public Integer value() {
                return metrics.getExecutionTimeMean();
            }
        });
    }

    protected void createExecutionLatencyPercentileGauge(final String name, final double percentile) {
        metricsRegistry.newGauge(createMetricName(name), new Gauge<Integer>() {
            @Override
            public Integer value() {
                return metrics.getExecutionTimePercentile(percentile);
            }
        });
    }

    protected void createTotalLatencyMeanGauge(final String name) {
        metricsRegistry.newGauge(createMetricName(name), new Gauge<Integer>() {
            @Override
            public Integer value() {
                return metrics.getTotalTimeMean();
            }
        });
    }

    protected void createTotalLatencyPercentileGauge(final String name, final double percentile) {
        metricsRegistry.newGauge(createMetricName(name), new Gauge<Integer>() {
            @Override
            public Integer value() {
                return metrics.getTotalTimePercentile(percentile);
            }
        });
    }

    protected final Func0<Integer> currentConcurrentExecutionCountThunk = new Func0<Integer>() {
        @Override
        public Integer call() {
            return metrics.getCurrentConcurrentExecutionCount();
        }
    };

    protected final Func0<Long> rollingMaxConcurrentExecutionCountThunk = new Func0<Long>() {
        @Override
        public Long call() {
            return metrics.getRollingMaxConcurrentExecutions();
        }
    };

    protected final Func0<Integer> errorPercentageThunk = new Func0<Integer>() {
        @Override
        public Integer call() {
            return metrics.getHealthCounts().getErrorPercentage();
        }
    };

    protected void createCurrentValueGauge(final String name, final Func0<Integer> metricToEvaluate) {
        metricsRegistry.newGauge(createMetricName(name), new Gauge<Integer>() {
            @Override
            public Integer value() {
                return metricToEvaluate.call();
            }
        });
    }
}
