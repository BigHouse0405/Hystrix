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


import com.netflix.hystrix.HystrixCollapser;
import com.netflix.hystrix.contrib.javanica.exception.FallbackInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This command is used in collapser.
 */
@ThreadSafe
public class BatchHystrixCommand extends AbstractHystrixCommand<List<Object>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericCommand.class);



    protected BatchHystrixCommand(HystrixCommandBuilder builder) {
        super(builder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Object> run() throws Exception {
        Object[] args = toArgs(getCollapsedRequests());
        return (List) process(args);
    }

    private Object process(final Object[] args) throws Exception {
        return process(new Action() {
            @Override
            Object execute() {
                return getCommandAction().executeWithArgs(getExecutionType(), args);
            }
        });
    }


    @Override
    protected List<Object> getFallback() {
        if (getFallbackAction() != null) {
            final CommandAction commandAction = getFallbackAction();
            final Object[] args = toArgs(getCollapsedRequests());
            try {
                return (List<Object>) process(new Action() {
                    @Override
                    Object execute() {
                        return commandAction.executeWithArgs(ExecutionType.SYNCHRONOUS, args);
                    }
                });
            } catch (Throwable e) {
                LOGGER.error(FallbackErrorMessageBuilder.create()
                        .append(commandAction, e).build());
                throw new FallbackInvocationException(e.getCause());
            }
        } else {
            return super.getFallback();
        }
    }

    Object[] toArgs(Collection<HystrixCollapser.CollapsedRequest<Object, Object>> requests) {
        return new Object[]{collect(getCollapsedRequests())};
    }

    List<Object> collect(Collection<HystrixCollapser.CollapsedRequest<Object, Object>> requests) {
        List<Object> commandArgs = new ArrayList<Object>();
        for (HystrixCollapser.CollapsedRequest<Object, Object> request : requests) {
            final Object[] args = (Object[]) request.getArgument();
            commandArgs.add(args[0]);
        }
        return commandArgs;
    }

}
