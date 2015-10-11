package com.netflix.hystrix.contrib.javanica.test.common.collapser;

import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.HystrixInvokableInfo;
import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCollapser;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.contrib.javanica.test.common.domain.User;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.netflix.hystrix.contrib.javanica.test.common.CommonUtils.getHystrixCommandByKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by dmgcodevil
 */
public abstract class BasicCollapserTest {

    protected abstract UserService createUserService();

    private UserService userService;
    private HystrixRequestContext context;

    @Before
    public void setUp() throws Exception {
        userService = createUserService();
        context = HystrixRequestContext.initializeContext();
    }

    @After
    public void tearDown() throws Exception {
        context.shutdown();
    }

    @Test
    public void testGetUserById() throws ExecutionException, InterruptedException {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
            Future<User> f1 = userService.getUserById("1");
            Future<User> f2 = userService.getUserById("2");
            Future<User> f3 = userService.getUserById("3");
            Future<User> f4 = userService.getUserById("4");
            Future<User> f5 = userService.getUserById("5");

            assertEquals("name: 1", f1.get().getName());
            assertEquals("name: 2", f2.get().getName());
            assertEquals("name: 3", f3.get().getName());
            assertEquals("name: 4", f4.get().getName());
            assertEquals("name: 5", f5.get().getName());
            // assert that the batch command 'getUserByIds' was in fact
            // executed and that it executed only once
            assertEquals(1, HystrixRequestLog.getCurrentRequest().getAllExecutedCommands().size());
            HystrixInvokableInfo<?> command = HystrixRequestLog.getCurrentRequest()
                    .getAllExecutedCommands().iterator().next();
            // assert the command is the one we're expecting
            assertEquals("getUserByIds", command.getCommandKey().name());
            // confirm that it was a COLLAPSED command execution
            assertTrue(command.getExecutionEvents().contains(HystrixEventType.COLLAPSED));
            // and that it was successful
            assertTrue(command.getExecutionEvents().contains(HystrixEventType.SUCCESS));
        } finally {
            context.shutdown();
        }
    }

    @Test
    public void testGetUserByIdWithFallback() throws ExecutionException, InterruptedException {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        try {
            Future<User> f1 = userService.getUserByIdWithFallback("1");
            Future<User> f2 = userService.getUserByIdWithFallback("2");
            Future<User> f3 = userService.getUserByIdWithFallback("3");
            Future<User> f4 = userService.getUserByIdWithFallback("4");
            Future<User> f5 = userService.getUserByIdWithFallback("5");

            assertEquals("name: 1", f1.get().getName());
            assertEquals("name: 2", f2.get().getName());
            assertEquals("name: 3", f3.get().getName());
            assertEquals("name: 4", f4.get().getName());
            assertEquals("name: 5", f5.get().getName());
            // two command should be executed: "getUserByIdWithFallback" and "getUserByIdsWithFallback"
            assertEquals(2, HystrixRequestLog.getCurrentRequest().getAllExecutedCommands().size());
            HystrixInvokableInfo<?> getUserByIdsWithFallback = getHystrixCommandByKey("getUserByIdsWithFallback");
            com.netflix.hystrix.HystrixInvokableInfo getUserByIdsFallback = getHystrixCommandByKey("getUserByIdsFallback");
            // confirm that command has failed
            assertTrue(getUserByIdsWithFallback.getExecutionEvents().contains(HystrixEventType.FAILURE));
            assertTrue(getUserByIdsWithFallback.getExecutionEvents().contains(HystrixEventType.FALLBACK_SUCCESS));
            // and that fallback was successful
            assertTrue(getUserByIdsFallback.getExecutionEvents().contains(HystrixEventType.SUCCESS));
        } finally {
            context.shutdown();
        }
    }


    public static class UserService {

        public static final User DEFAULT_USER = new User("def", "def");


        @HystrixCollapser(batchMethod = "getUserByIds",
                collapserProperties = {@HystrixProperty(name = "timerDelayInMilliseconds", value = "200")})
        public Future<User> getUserById(String id) {
            return null;
        }

        @HystrixCollapser(batchMethod = "getUserByIdsWithFallback",
                collapserProperties = {@HystrixProperty(name = "timerDelayInMilliseconds", value = "200")})
        public Future<User> getUserByIdWithFallback(String id) {
            return null;
        }


        @HystrixCommand(commandProperties = {
                @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "10000")// for debug
        })
        public List<User> getUserByIds(List<String> ids) {
            List<User> users = new ArrayList<User>();
            for (String id : ids) {
                users.add(new User(id, "name: " + id));
            }
            return users;
        }

        @HystrixCommand(fallbackMethod = "getUserByIdsFallback",
                commandProperties = {
                        @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "10000")// for debug
                })

        public List<User> getUserByIdsWithFallback(List<String> ids) {
            throw new RuntimeException("not found");
        }


        @HystrixCommand
        private List<User> getUserByIdsFallback(List<String> ids) {
            List<User> users = new ArrayList<User>();
            for (String id : ids) {
                users.add(new User(id, "name: " + id));
            }
            return users;
        }

    }
}
