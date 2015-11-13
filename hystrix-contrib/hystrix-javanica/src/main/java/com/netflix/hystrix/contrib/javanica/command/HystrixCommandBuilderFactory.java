/**
 * Copyright 2015 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix.contrib.javanica.command;

import com.netflix.hystrix.HystrixCollapser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.utils.FallbackMethod;
import com.netflix.hystrix.contrib.javanica.utils.MethodProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import static com.netflix.hystrix.contrib.javanica.cache.CacheInvocationContextFactory.createCacheRemoveInvocationContext;
import static com.netflix.hystrix.contrib.javanica.cache.CacheInvocationContextFactory.createCacheResultInvocationContext;
import static com.netflix.hystrix.contrib.javanica.utils.EnvUtils.isCompileWeaving;
import static com.netflix.hystrix.contrib.javanica.utils.ajc.AjcUtils.getAjcMethodAroundAdvice;

/**
 * Created by dmgcodevil.
 */
public class HystrixCommandBuilderFactory {

    // todo Add Cache

    private static final HystrixCommandBuilderFactory INSTANCE = new HystrixCommandBuilderFactory();

    public static HystrixCommandBuilderFactory getInstance() {
        return INSTANCE;
    }

    private HystrixCommandBuilderFactory() {

    }

    public HystrixCommandBuilder create(MetaHolder metaHolder) {
        return create(metaHolder, Collections.<HystrixCollapser.CollapsedRequest<Object, Object>>emptyList());
    }

    public HystrixCommandBuilder create(MetaHolder metaHolder, Collection<HystrixCollapser.CollapsedRequest<Object, Object>> collapsedRequests) {
        validateMetaHolder(metaHolder);

        return HystrixCommandBuilder.builder()
                .setterBuilder(createGenericSetterBuilder(metaHolder))
                .commandActions(createCommandActions(metaHolder))
                .collapsedRequests(collapsedRequests)
                .cacheResultInvocationContext(createCacheResultInvocationContext(metaHolder))
                .cacheRemoveInvocationContext(createCacheRemoveInvocationContext(metaHolder))
                .ignoreExceptions(metaHolder.getHystrixCommand().ignoreExceptions())
                .executionType(metaHolder.getExecutionType())
                .build();
    }

    private void validateMetaHolder(MetaHolder metaHolder) {
        Validate.notNull(metaHolder, "metaHolder is required parameter and cannot be null");
        Validate.isTrue(metaHolder.isCommandAnnotationPresent(), "hystrixCommand annotation is absent");
    }

    private GenericSetterBuilder createGenericSetterBuilder(MetaHolder metaHolder) {
        GenericSetterBuilder.Builder setterBuilder = GenericSetterBuilder.builder()
                .groupKey(createGroupKey(metaHolder))
                .threadPoolKey(createThreadPoolKey(metaHolder))
                .commandKey(createCommandKey(metaHolder))
                .collapserKey(createCollapserKey(metaHolder))
                .commandProperties(metaHolder.getCommandProperties())
                .threadPoolProperties(metaHolder.getThreadPoolProperties())
                .collapserProperties(metaHolder.getCollapserProperties());
        if (metaHolder.isCollapserAnnotationPresent()) {
            setterBuilder.scope(metaHolder.getHystrixCollapser().scope());
        }
        return setterBuilder.build();
    }

    private String createGroupKey(MetaHolder metaHolder) {
        return createKey(metaHolder.getHystrixCommand().groupKey(), metaHolder.getDefaultGroupKey());
    }

    private String createThreadPoolKey(MetaHolder metaHolder) {
        // this key is created without default value because intrinsically Hystrix knows how to derive this key properly if it's absent
        return metaHolder.getHystrixCommand().threadPoolKey();
    }

    private String createCommandKey(MetaHolder metaHolder) {
        return createKey(metaHolder.getHystrixCommand().commandKey(), metaHolder.getDefaultCommandKey());
    }

    private String createCollapserKey(MetaHolder metaHolder) {
        if (metaHolder.isCollapserAnnotationPresent()) {
            return createKey(metaHolder.getHystrixCollapser().collapserKey(), metaHolder.getDefaultCollapserKey());
        }
        return null;
    }

    private String createKey(String key, String defKey) {
        return StringUtils.isNotBlank(key) ? key : defKey;
    }


    private CommandActions createCommandActions(MetaHolder metaHolder) {
        CommandAction commandAction = createCommandAction(metaHolder);
        CommandAction fallbackAction = createFallbackAction(metaHolder);
        return CommandActions.builder().commandAction(commandAction)
                .fallbackAction(fallbackAction).build();
    }

    private CommandAction createCommandAction(MetaHolder metaHolder) {
        return new MethodExecutionAction(metaHolder.getObj(), metaHolder.getMethod(), metaHolder.getArgs(), metaHolder);
    }

    private CommandAction createFallbackAction(MetaHolder metaHolder) {

        FallbackMethod fallbackMethod = MethodProvider.getInstance().getFallbackMethod(metaHolder.getObj().getClass(),
                metaHolder.getMethod(), metaHolder.isExtendedFallback());
        fallbackMethod.validateReturnType(metaHolder.getMethod());
        CommandAction fallbackAction = null;
        if (fallbackMethod.isPresent()) {

            Method fMethod = fallbackMethod.getMethod();
            if (fallbackMethod.isCommand()) {
                fMethod.setAccessible(true);
                MetaHolder fmMetaHolder = MetaHolder.builder()
                        .obj(metaHolder.getObj())
                        .method(fMethod)
                        .ajcMethod(getAjcMethod(metaHolder.getObj(), fMethod))
                        .args(metaHolder.getArgs())
                        .fallback(true)
                        .defaultCollapserKey(metaHolder.getDefaultCollapserKey())
                        .fallbackMethod(fMethod)
                        .extendedFallback(fallbackMethod.isExtended())
                        .fallbackExecutionType(fallbackMethod.getExecutionType())
                        .extendedParentFallback(metaHolder.isExtendedFallback())
                        .observable(ExecutionType.OBSERVABLE == fallbackMethod.getExecutionType())
                        .defaultCommandKey(fMethod.getName())
                        .defaultGroupKey(metaHolder.getDefaultGroupKey())
                        .hystrixCollapser(metaHolder.getHystrixCollapser())
                        .hystrixCommand(fMethod.getAnnotation(HystrixCommand.class)).build();
                fallbackAction = new LazyCommandExecutionAction(fmMetaHolder);
            } else {
                MetaHolder fmMetaHolder = MetaHolder.builder()
                        .obj(metaHolder.getObj())
                        .method(fMethod)
                        .fallbackExecutionType(ExecutionType.SYNCHRONOUS)
                        .extendedFallback(fallbackMethod.isExtended())
                        .extendedParentFallback(metaHolder.isExtendedFallback())
                        .ajcMethod(null) // if fallback method isn't annotated with command annotation then we don't need to get ajc method for this
                        .args(metaHolder.getArgs()).build();

                fallbackAction = new MethodExecutionAction(fmMetaHolder.getObj(), fMethod, fmMetaHolder.getArgs(), fmMetaHolder);
            }

        }
        return fallbackAction;
    }

    private Method getAjcMethod(Object target, Method fallback) {
        if (isCompileWeaving()) {
            return getAjcMethodAroundAdvice(target.getClass(), fallback);
        }
        return null;
    }

}
