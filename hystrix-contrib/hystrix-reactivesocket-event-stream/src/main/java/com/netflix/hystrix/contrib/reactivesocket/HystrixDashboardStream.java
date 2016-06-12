/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix.contrib.reactivesocket;

import com.netflix.hystrix.HystrixCollapserMetrics;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import rx.Observable;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HystrixDashboardStream {
    final int delayInMs;
    final Observable<DashboardData> singleSource;
    final AtomicBoolean isSourceCurrentlySubscribed = new AtomicBoolean(false);

    private HystrixDashboardStream(int delayInMs) {
        this.delayInMs = delayInMs;
        this.singleSource = Observable.interval(delayInMs, TimeUnit.MILLISECONDS)
                .map(timestamp -> new DashboardData(
                        HystrixCommandMetrics.getInstances(),
                        HystrixThreadPoolMetrics.getInstances(),
                        HystrixCollapserMetrics.getInstances()
                ))
                .doOnSubscribe(() -> isSourceCurrentlySubscribed.set(true))
                .doOnUnsubscribe(() -> isSourceCurrentlySubscribed.set(false))
                .share()
                .onBackpressureDrop();
    }

    private static final HystrixDashboardStream INSTANCE = new HystrixDashboardStream(500);

    public static HystrixDashboardStream getInstance() {
        return INSTANCE;
    }

    static HystrixDashboardStream getNonSingletonInstanceOnlyUsedInUnitTests(int delayInMs) {
        return new HystrixDashboardStream(delayInMs);
    }

    /**
     * Return a ref-counted stream that will only do work when at least one subscriber is present
     */
    public Observable<DashboardData> observe() {
        return singleSource;
    }

    public boolean isSourceCurrentlySubscribed() {
        return isSourceCurrentlySubscribed.get();
    }

    public static class DashboardData {
        final Collection<HystrixCommandMetrics> commandMetrics;
        final Collection<HystrixThreadPoolMetrics> threadPoolMetrics;
        final Collection<HystrixCollapserMetrics> collapserMetrics;

        public DashboardData(Collection<HystrixCommandMetrics> commandMetrics, Collection<HystrixThreadPoolMetrics> threadPoolMetrics, Collection<HystrixCollapserMetrics> collapserMetrics) {
            this.commandMetrics = commandMetrics;
            this.threadPoolMetrics = threadPoolMetrics;
            this.collapserMetrics = collapserMetrics;
        }

        public Collection<HystrixCommandMetrics> getCommandMetrics() {
            return commandMetrics;
        }

        public Collection<HystrixThreadPoolMetrics> getThreadPoolMetrics() {
            return threadPoolMetrics;
        }

        public Collection<HystrixCollapserMetrics> getCollapserMetrics() {
            return collapserMetrics;
        }
    }
}


