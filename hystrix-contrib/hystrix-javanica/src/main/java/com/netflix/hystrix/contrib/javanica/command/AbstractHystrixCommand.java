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
package com.netflix.hystrix.contrib.javanica.command;


import com.google.common.base.Throwables;
import com.netflix.hystrix.HystrixCollapser;
import com.netflix.hystrix.contrib.javanica.conf.HystrixPropertiesManager;
import com.netflix.hystrix.contrib.javanica.exception.CommandActionExecutionException;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Map;

/**
 * Base class for hystrix commands.
 *
 * @param <T> the return type
 */
@ThreadSafe
public abstract class AbstractHystrixCommand<T> extends com.netflix.hystrix.HystrixCommand<T> {

    private CommandActions commandActions;
    private final Map<String, Object> commandProperties;
    private final Collection<HystrixCollapser.CollapsedRequest<Object, Object>> collapsedRequests;
    private final Class<? extends Throwable>[] ignoreExceptions;
    private final ExecutionType executionType;

    /**
     * Constructor with parameters.
     *
     * @param setterBuilder     the builder to build {@link com.netflix.hystrix.HystrixCommand.Setter}
     * @param commandActions    the command actions {@link CommandActions}
     * @param commandProperties the command properties
     * @param collapsedRequests the collapsed requests
     * @param ignoreExceptions  the exceptions which should be ignored and wrapped to throw in {@link HystrixBadRequestException}
     * @param executionType     the execution type {@link ExecutionType}
     */
    protected AbstractHystrixCommand(CommandSetterBuilder setterBuilder,
                                     CommandActions commandActions,
                                     Map<String, Object> commandProperties,
                                     Collection<HystrixCollapser.CollapsedRequest<Object, Object>> collapsedRequests,
                                     final Class<? extends Throwable>[] ignoreExceptions,
                                     ExecutionType executionType) {
        super(setterBuilder.build());
        this.commandActions = commandActions;
        this.commandProperties = commandProperties;
        this.collapsedRequests = collapsedRequests;
        this.ignoreExceptions = ignoreExceptions;
        this.executionType = executionType;
        HystrixPropertiesManager.setCommandProperties(commandProperties, getCommandKey().name());
    }

    /**
     * Gets command action.
     *
     * @return command action
     */
    CommandAction getCommandAction() {
        return commandActions.getCommandAction();
    }

    /**
     * Gets fallback action.
     *
     * @return fallback action
     */
    CommandAction getFallbackAction() {
        return commandActions.getFallbackAction();
    }

    /**
     * Gets key action.
     *
     * @return key action
     */
    CommandAction getCacheKeyAction() {
        return commandActions.getCacheKeyAction();
    }

    /**
     * Gets command properties.
     *
     * @return command properties
     */
    Map<String, Object> getCommandProperties() {
        return commandProperties;
    }

    /**
     * Gets collapsed requests.
     *
     * @return collapsed requests
     */
    Collection<HystrixCollapser.CollapsedRequest<Object, Object>> getCollapsedRequests() {
        return collapsedRequests;
    }

    /**
     * Gets exceptions types which should be ignored.
     *
     * @return exceptions types
     */
    Class<? extends Throwable>[] getIgnoreExceptions() {
        return ignoreExceptions;
    }

    public ExecutionType getExecutionType() {
        return executionType;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected String getCacheKey() {
        String key;
        if (commandActions.getCacheKeyAction() != null) {
            key = String.valueOf(commandActions.getCacheKeyAction().execute(executionType));
        } else {
            key = super.getCacheKey();
        }
        return key;
    }

    /**
     * Check whether triggered exception is ignorable.
     *
     * @param throwable the exception occurred during a command execution
     * @return true if exception is ignorable, otherwise - false
     */
    boolean isIgnorable(Throwable throwable) {
        if (ignoreExceptions == null || ignoreExceptions.length == 0) {
            return false;
        }
        for (Class<? extends Throwable> ignoreException : ignoreExceptions) {
            if (ignoreException.isAssignableFrom(throwable.getClass())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes an action. If an action has failed and an exception is ignorable then propagate it as HystrixBadRequestException
     * otherwise propagate original exception to trigger fallback method.
     * Note: If an exception occurred in a command directly extends {@link java.lang.Throwable} then this exception cannot be re-thrown
     * as original exception because HystrixCommand.run() allows throw subclasses of {@link java.lang.Exception}.
     * Thus we need to wrap cause in RuntimeException, anyway in this case the fallback logic will be triggered.
     *
     * @param action the action
     * @return result of command action execution
     */
    Object process(Action action) throws Exception {
        Object result;
        try {
            result = action.execute();
        } catch (CommandActionExecutionException throwable) {
            Throwable cause = throwable.getCause();
            if (isIgnorable(cause)) {
                throw new HystrixBadRequestException(cause.getMessage(), cause);
            }
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw Throwables.propagate(cause);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected abstract T run() throws Exception;

    /**
     * {@inheritDoc}.
     */
    @Override
    protected T getFallback() {
        throw new RuntimeException("No fallback available.", getFailedExecutionException());
    }

    /**
     * Common action.
     */
    abstract class Action {
        /**
         * Each implementation of this method should wrap any exceptions in CommandActionExecutionException.
         *
         * @return execution result
         * @throws CommandActionExecutionException
         */
        abstract Object execute() throws CommandActionExecutionException;
    }


    /**
     * Builder to create error message for failed fallback operation.
     */
    static class FallbackErrorMessageBuilder {
        private StringBuilder builder = new StringBuilder("failed to processed fallback");

        static FallbackErrorMessageBuilder create() {
            return new FallbackErrorMessageBuilder();
        }

        public FallbackErrorMessageBuilder append(CommandAction action, Throwable throwable) {
            return commandAction(action).exception(throwable);
        }

        private FallbackErrorMessageBuilder commandAction(CommandAction action) {
            if (action instanceof CommandExecutionAction || action instanceof LazyCommandExecutionAction) {
                builder.append(": '").append(action.getActionName()).append("'. ")
                        .append(action.getActionName()).append(" fallback is a hystrix command. ");
            } else if (action instanceof MethodExecutionAction) {
                builder.append(" is the method: '").append(action.getActionName()).append("'. ");
            }
            return this;
        }

        private FallbackErrorMessageBuilder exception(Throwable throwable) {
            if (throwable instanceof HystrixBadRequestException) {
                builder.append("exception: '").append(throwable.getCause().getClass())
                        .append("' occurred in fallback was ignored and wrapped to HystrixBadRequestException.\n");
            } else if (throwable instanceof HystrixRuntimeException) {
                builder.append("exception: '").append(throwable.getCause().getClass())
                        .append("' occurred in fallback wasn't ignored.\n");
            }
            return this;
        }

        public String build() {
            return builder.toString();
        }
    }

}
