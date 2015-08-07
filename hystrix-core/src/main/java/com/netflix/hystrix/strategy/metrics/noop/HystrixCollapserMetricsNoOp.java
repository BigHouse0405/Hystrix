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
package com.netflix.hystrix.strategy.metrics.noop;

import com.netflix.hystrix.HystrixCollapserKey;
import com.netflix.hystrix.HystrixCollapserMetrics;
import com.netflix.hystrix.HystrixCollapserProperties;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;

/**
 * Not needed for health check - so it's fine to just drop all collapser metrics on the floor if you're not interested in them
 */
public class HystrixCollapserMetricsNoOp extends HystrixCollapserMetrics {

    HystrixCollapserMetricsNoOp(HystrixCollapserKey key, HystrixCollapserProperties properties) {
        super(key, properties);
    }

    @Override
    public int getBatchSizePercentile(double percentile) {
        return 0;
    }

    @Override
    public int getBatchSizeMean() {
        return 0;
    }

    @Override
    protected void addBatchSize(int batchSize) {

    }

    @Override
    public int getShardSizePercentile(double percentile) {
        return 0;
    }

    @Override
    public int getShardSizeMean() {
        return 0;
    }

    @Override
    protected void addShardSize(int shardSize) {

    }

    @Override
    public long getCumulativeCount(HystrixRollingNumberEvent event) {
        return 0;
    }

    @Override
    public long getRollingCount(HystrixRollingNumberEvent event) {
        return 0;
    }

    @Override
    public long getRollingMax(HystrixRollingNumberEvent event) {
        return 0;
    }

    @Override
    protected void addEvent(HystrixRollingNumberEvent event) {

    }

    @Override
    protected void addEventWithValue(HystrixRollingNumberEvent event, long value) {

    }

    @Override
    protected void updateRollingMax(HystrixRollingNumberEvent event, long value) {

    }
}
