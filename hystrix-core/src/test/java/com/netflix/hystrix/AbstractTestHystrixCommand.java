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
package com.netflix.hystrix;

import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

public interface AbstractTestHystrixCommand<R> extends HystrixObservable<R>, InspectableBuilder {

    public static enum ExecutionResult {
        SUCCESS, FAILURE, ASYNC_FAILURE, HYSTRIX_FAILURE, ASYNC_HYSTRIX_FAILURE, RECOVERABLE_ERROR, ASYNC_RECOVERABLE_ERROR, UNRECOVERABLE_ERROR, ASYNC_UNRECOVERABLE_ERROR, BAD_REQUEST, ASYNC_BAD_REQUEST, MULTIPLE_EMITS_THEN_SUCCESS, MULTIPLE_EMITS_THEN_FAILURE, NO_EMITS_THEN_SUCCESS
    }

    public static enum FallbackResult {
        UNIMPLEMENTED, SUCCESS, FAILURE, ASYNC_FAILURE, MULTIPLE_EMITS_THEN_SUCCESS, MULTIPLE_EMITS_THEN_FAILURE, NO_EMITS_THEN_SUCCESS
    }

    public static enum CacheEnabled {
        YES, NO
    }

    static HystrixPropertiesStrategy TEST_PROPERTIES_FACTORY = new TestPropertiesFactory();

    static class TestPropertiesFactory extends HystrixPropertiesStrategy {

        @Override
        public HystrixCommandProperties getCommandProperties(HystrixCommandKey commandKey, HystrixCommandProperties.Setter builder) {
            if (builder == null) {
                builder = HystrixCommandPropertiesTest.getUnitTestPropertiesSetter();
            }
            return HystrixCommandPropertiesTest.asMock(builder);
        }

        @Override
        public HystrixThreadPoolProperties getThreadPoolProperties(HystrixThreadPoolKey threadPoolKey, HystrixThreadPoolProperties.Setter builder) {
            if (builder == null) {
                builder = HystrixThreadPoolProperties.Setter.getUnitTestPropertiesBuilder();
            }
            return HystrixThreadPoolProperties.Setter.asMock(builder);
        }

        @Override
        public HystrixCollapserProperties getCollapserProperties(HystrixCollapserKey collapserKey, HystrixCollapserProperties.Setter builder) {
            throw new IllegalStateException("not expecting collapser properties");
        }

        @Override
        public String getCommandPropertiesCacheKey(HystrixCommandKey commandKey, HystrixCommandProperties.Setter builder) {
            return null;
        }

        @Override
        public String getThreadPoolPropertiesCacheKey(HystrixThreadPoolKey threadPoolKey, com.netflix.hystrix.HystrixThreadPoolProperties.Setter builder) {
            return null;
        }

        @Override
        public String getCollapserPropertiesCacheKey(HystrixCollapserKey collapserKey, com.netflix.hystrix.HystrixCollapserProperties.Setter builder) {
            return null;
        }

    }

}
