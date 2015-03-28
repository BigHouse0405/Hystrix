# hystrix-javanica

Java language has a great advantages over other languages such as reflection and annotations.
All modern frameworks such as Spring, Hibernate, myBatis and etc. seek to use this advantages to the maximum.
The idea of introduction annotations in Hystrix is obvious solution for improvement. Currently using Hystrix involves writing a lot of code that is a barrier to rapid development. You likely be spending a lot of time on writing a Hystrix commands. Idea of the Javanica project is make easier using of Hystrix by the introduction of support annotations.

First of all in order to use hystrix-javanica you need to add hystrix-javanica dependency in your project.

Example for Maven:
```xml
<dependency>
    <groupId>com.netflix.hystrix</groupId>
    <artifactId>hystrix-javanica</artifactId>
    <version>x.y.z</version>
</dependency>
```

To implement AOP functionality in the project was used AspectJ library. If in your project already used AspectJ then you need to add hystrix aspect in aop.xml as below:
```xml
<aspects>
        ...
        <aspect name="com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect"/>
        ...
</aspects>
```
More about AspectJ configuration read [here] (http://www.eclipse.org/aspectj/doc/next/devguide/ltw-configuration.html)


If you use Spring AOP in your project then you need to add specific configuration using Spring AOP namespace in order to make Spring capable to manage aspects which were written using AspectJ and declare `HystrixCommandAspect` as Spring bean like below:

```xml
    <aop:aspectj-autoproxy/>
    <bean id="hystrixAspect" class="com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect"></bean>
```

Or if you are using Spring code configuration:

```java
@Configuration
public class HystrixConfiguration {

  @Bean
  public HystrixCommandAspect hystrixAspect() {
    return new HystrixCommandAspect();
  }

}
```

It doesn't matter which approach you use to create proxies in Spring, javanica works fine with JDK and CGLIB proxies. If you use another framework for aop which supports AspectJ and uses other libs (Javassist for instance) to create proxies then let us know what lib you use to create proxies and we'll try to add support for this library in near future.

More about Spring AOP + AspectJ read [here] (http://docs.spring.io/spring/docs/current/spring-framework-reference/html/aop.html)

# How to use

## Hystrix command
### Synchronous Execution

To run method as Hystrix command synchronously you need to annotate method with `@HystrixCommand` annotation, for example
```java
public class UserService {
...
    @HystrixCommand
    public User getUserById(String id) {
        return userResource.getUserById(id);
    }
}
...
```
In example above the `getUserById` method will be processed [synchronously](https://github.com/Netflix/Hystrix/wiki/How-To-Use#wiki-Synchronous-Execution) within new Hystrix command. 
By default the name of **command key** is command method name: `getUserById`, default **group key** name is class name of annotated method: `UserService`. You can change it using necessary `@HystrixCommand` properties:

```java
    @HystrixCommand(groupKey="UserGroup", commandKey = "GetUserByIdCommand")
    public User getUserById(String id) {
        return userResource.getUserById(id);
    }
```
To set threadPoolKey use ```@HystrixCommand#threadPoolKey()```

### Asynchronous Execution

To process Hystrix command asynchronously you should return an instance of `AsyncResult` in your command method as in the exapmple below:
```java
    @HystrixCommand
    public Future<User> getUserByIdAsync(final String id) {
        return new AsyncResult<User>() {
            @Override
            public User invoke() {
                return userResource.getUserById(id);
            }
        };
    }
```

The return type of command method should be Future that indicates that a command should be executed [asynchronously] (https://github.com/Netflix/Hystrix/wiki/How-To-Use#wiki-Asynchronous-Execution).

## Reactive Execution

To performe "Reactive Execution" you should return an instance of `ObservableResult` in your command method as in the exapmple below:

```java
    @HystrixCommand
    public Observable<User> getUserById(final String id) {
        return new ObservableResult<User>() {
            @Override
            public User invoke() {
                return userResource.getUserById(id);
            }
        };
    }
```

The return type of command method should be `Observable`.

## Fallback

Graceful degradation can be achieved by declaring name of fallback method in `@HystrixCommand` like below:

```java
    @HystrixCommand(fallbackMethod = "defaultUser")
    public User getUserById(String id) {
        return userResource.getUserById(id);
    }

    private User defaultUser(String id) {
        return new User("def", "def");
    }
```

**_Its important to remember that Hystrix command and fallback should be placed in the same class and have same method signature_**.

Fallback method can have any access modifier. Method `defaultUser` will be used to process fallback logic in a case of any errors. If you need to run fallback method `defaultUser` as separate Hystrix command then you need to annotate it with `HystrixCommand` annotation as below:
```java
    @HystrixCommand(fallbackMethod = "defaultUser")
    public User getUserById(String id) {
        return userResource.getUserById(id);
    }

    @HystrixCommand
    private User defaultUser(String id) {
        return new User();
    }
```

If fallback method was marked with `@HystrixCommand` then this fallback method (_defaultUser_) also can has own fallback method, as in the example below:
```java
    @HystrixCommand(fallbackMethod = "defaultUser")
    public User getUserById(String id) {
        return userResource.getUserById(id);
    }

    @HystrixCommand(fallbackMethod = "defaultUserSecond")
    private User defaultUser(String id) {
        return new User();
    }
    
    @HystrixCommand
    private User defaultUserSecond(String id) {
        return new User("def", "def");
    }
```

## Error Propagation
Based on [this](https://github.com/Netflix/Hystrix/wiki/How-To-Use#ErrorPropagation) description, `@HystrixCommand` has an ability to specify exceptions types which should be ignored and wrapped to throw in `HystrixBadRequestException`.

```java
    @HystrixCommand(ignoreExceptions = {BadRequestException.class})
    public User getUserById(String id) {
        return userResource.getUserById(id);
    }
```

If `userResource.getUserById(id);` throws an exception which type is _BadRequestException_ then this exception will be wrapped in `HystrixBadRequestException` and will not affect metrics and will not trigger fallback logic.

## Request Cache

Javanica provides specific annotations in order to enable and manage request caching. This annotations look very similar to [JSR107](https://github.com/jsr107/jsr107spec) but less extensive than those, by other hand Hystrix doesn't provide independent and complex caching system therefore  there is no need to have such diversity of annotations as in JSR107.
Javanica has only three annotations dedicated for request caching.


| Annotation        | Description           | Properties  |
| ------------- |-------------| -----|
| @CacheResult     | Marks a methods that results should be cached for a Hystrix command.This annotation must be used in conjunction with HystrixCommand annotation. | cacheKeyMethod |
| @CacheRemove     | Marks methods used to invalidate cache of a command. Generated cache key must be same as key generated within link CacheResult context      |   commandKey, cacheKeyMethod |
| @CacheKey | Marks a method argument as part of the cache key. If no arguments are marked all arguments are used. If _@CacheResult_ or _@CacheRemove_ annotation has specified _cacheKeyMethod_ then a method arguments will not be used to build cache key even if they annotated with _@CacheKey_    |    value |

**cacheKeyMethod** - a method name to be used to get a key for request caching. The command and cache key method should be placed in the same class and have same method signature except cache key method return type that should be _String_.
_cacheKeyMethod_ has higher priority than an arguments of a method, that means what actual arguments
of a method that annotated with _@CacheResult_ will not be used to generate cache key, instead specified
_cacheKeyMethod_ fully assigns to itself responsibility for cache key generation.
By default this returns empty string which means "do not use cache method.
You can consider _cacheKeyMethod_ as a replacement for common key generators (for example [JSR170-CacheKeyGenerator](https://github.com/jsr107/jsr107spec/blob/master/src/main/java/javax/cache/annotation/CacheKeyGenerator.java)) but with _cacheKeyMethod_ cache key generation becomes more convenient and simple. Not to be unfounded let's compare the two approaches:
JSR107
```java

    @CacheRemove(cacheName = "getUserById", cacheKeyGenerator = UserCacheKeyGenerator.class)
    @HystrixCommand
    public void update(@CacheKey User user) {
         storage.put(user.getId(), user);
    }
        
    public static class UserCacheKeyGenerator implements HystrixCacheKeyGenerator {
        @Override
        public HystrixGeneratedCacheKey generateCacheKey(CacheKeyInvocationContext<? extends Annotation>  cacheKeyInvocationContext) {
            CacheInvocationParameter cacheInvocationParameter = cacheKeyInvocationContext.getKeyParameters()[0];
            User user = (User) cacheInvocationParameter.getValue();
            return new DefaultHystrixGeneratedCacheKey(user.getId());
        }
    }
```

Javanica cacheKeyMethod

```java
        @CacheRemove(commandKey = "getUserById", cacheKeyMethod=)
        @HystrixCommand
        public void update(User user) {
            storage.put(user.getId(), user);
        }
        private String cacheKeyMethod(User user) {
            return user.getId();
        }

```
or even just
```java
        @CacheRemove(commandKey = "getUserById")
        @HystrixCommand
        public void update(@CacheKey("id") User user) {
            storage.put(user.getId(), user);
        }
```
You don't need to create new classes, also approach with cacheKeyMethod helps during refactoring if you will give correct names for cache key methods. It is recommended to append prefix "cacheKeyMethod" to the real method name, for example:
```java
public User getUserById(@CacheKey String id);
```
```java
private User getUserByIdCacheKeyMethod(String id);
```
**Cache key generator**

Jacanica has only one cache key generator **HystrixCacheKeyGenerator** that generates a _HystrixGeneratedCacheKey_ based on _CacheInvocationContext_. Implementation is thread-safe.
Parameters of an annotated method are selected by the following rules:
- If no parameters are annotated with _@CacheKey_ then all parameters are included
- If one or more _@CacheKey_ annotations exist only those parameters with the _@CacheKey_ annotation are included

**Note**:  If _CacheResult_ or _CacheRemove_ annotation has specified **cacheKeyMethod** then a method arguments **will not be used to build cache key** even if they annotated with _CacheKey_.

**@CacheKey and value property**
This annotation has one property by default that allows specify name of a certain argument property. for example: ```@CacheKey("id") User user```, or in case composite property: ```@CacheKey("profile.name") User user```. Null properties are ignored, i.e. if ```profile``` is ```null```  then result of ```@CacheKey("profile.name") User user``` will be empty string.

Examples:

```java
        @CacheResult
        @HystrixCommand
        public User getUserById(@CacheKey String id) {
            return storage.get(id);
        }
        
        // --------------------------------------------------
        @CacheResult(cacheKeyMethod = "getUserByNameCacheKey")
        @HystrixCommand
        public User getUserByName(String name) {
            return storage.getByName(name);
        }
        private Long getUserByNameCacheKey(String name) {
            return name;
        }
        // --------------------------------------------------
        @CacheResult
        @HystrixCommand
        public void getUserByProfileName(@CacheKey("profile.email") User user) {
            storage.getUserByProfileName(user.getProfile().getName());
        }
        
```

**Get-Set-Get pattern**
To get more about this pattern you can read [this](https://github.com/Netflix/Hystrix/wiki/How-To-Use#get-set-get-with-request-cache-invalidation) chapter
Example:
```java
    public class UserService {    
        @CacheResult
        @HystrixCommand
        public User getUserById(@CacheKey String id) { // GET
            return storage.get(id);
        }

        @CacheRemove(commandKey = "getUserById")
        @HystrixCommand
        public void update(@CacheKey("id") User user) { // SET
            storage.put(user.getId(), user);
        }
    }    
        
        // test app
        
        public void test(){
        User user = userService.getUserById("1");
        HystrixInvokableInfo<?> getUserByIdCommand = getLastExecutedCommand();
        // this is the first time we've executed this command with
        // the value of "1" so it should not be from cache
        assertFalse(getUserByIdCommand.isResponseFromCache());
        user = userService.getUserById("1");
        getUserByIdCommand = getLastExecutedCommand();
        // this is the second time we've executed this command with
        // the same value so it should return from cache
        assertTrue(getUserByIdCommand.isResponseFromCache());
        
        user = new User("1", "new_name");
        userService.update(user); // update the user
        user = userService.getUserById("1");
        getUserByIdCommand = getLastExecutedCommand();
        // this is the first time we've executed this command after "update"
        // method was invoked and a cache for "getUserById" command was flushed
        // so the response shouldn't be from cache
        assertFalse(getUserByIdCommand.isResponseFromCache());
        }
```

**Note**: You can use @CacheRemove annotation in conjunction with  @HystrixCommand or without. If you want annotate not command method with @CacheRemove annotation then you need to add HystrixCacheAspect aspect to your configuration:

```xml
<aspects>
        ...
        <aspect name="com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCacheAspect"/>
        ...
</aspects>

<!-- or Spring conf -->

    <aop:aspectj-autoproxy/>
    <bean id="hystrixAspect" class="com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCacheAspect"></bean>

```

## Configuration
### Command Properties

Command properties can be set using @HystrixCommand's 'commandProperties' like below:

```java
    @HystrixCommand(commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "500")
        })
    public User getUserById(String id) {
        return userResource.getUserById(id);
    }
```

Javanica dynamically sets properties using Hystrix ConfigurationManager.
For the example above Javanica behind the scenes performs next action:
```java
ConfigurationManager.getConfigInstance().setProperty("hystrix.command.getUserById.execution.isolation.thread.timeoutInMilliseconds", "500");
```
More about Hystrix command properties [command](https://github.com/Netflix/Hystrix/wiki/Configuration#wiki-CommandExecution) and [fallback](https://github.com/Netflix/Hystrix/wiki/Configuration#wiki-CommandFallback)

ThreadPoolProperties can be set using @HystrixCommand's 'threadPoolProperties' like below:

```java
    @HystrixCommand(commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "500")
        },
                threadPoolProperties = {
                        @HystrixProperty(name = "coreSize", value = "30"),
                        @HystrixProperty(name = "maxQueueSize", value = "101"),
                        @HystrixProperty(name = "keepAliveTimeMinutes", value = "2"),
                        @HystrixProperty(name = "queueSizeRejectionThreshold", value = "15"),
                        @HystrixProperty(name = "metrics.rollingStats.numBuckets", value = "12"),
                        @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "1440")
        })
    public User getUserById(String id) {
        return userResource.getUserById(id);
    }
```

## Hystrix collapser

Suppose you have some command which calls should be collapsed in one backend call. For this goal you can use ```@HystrixCollapser``` annotation.

Example:
```java
    @HystrixCollapser(batchMethod = "getUserByIds")
    public Future<User> getUserById(String id) {
        return null;
    }
        
    @HystrixCommand
    public List<User> getUserByIds(List<String> ids) {
        List<User> users = new ArrayList<User>();
        for (String id : ids) {
            users.add(new User(id, "name: " + id));
        }
        return users;
    }
        

    Future<User> f1 = userService.getUserById("1");
    Future<User> f2 = userService.getUserById("2");
    Future<User> f3 = userService.getUserById("3");
    Future<User> f4 = userService.getUserById("4");
    Future<User> f5 = userService.getUserById("5");
```
A method annotated with ```@HystrixCollapser``` annotation can return any value with compatible type, it does not affect the result of collapser execution, collapser method can even return ```null``` or another stub.
There are several rules applied for methods signatures.

1. Collapser method must have one argument of any type, desired a wrapper of a primitive type like Integer, Long, String and etc. 
2. A batch method must have one argument with type java.util.List parameterized with corresponding type, that's if a type of collapser argument is ```Integer``` then type of batch method argument must be ```List<Integer>```.
3. Return type of batch method must be java.util.List parameterized with corresponding type, that's if a return type of collapser method is ```User``` then a return type of batch command must be ```List<User>```.

**Convention for batch method behavior**

The size of response collection must be equal to the size of request collection.

```java
  @HystrixCommand
  public List<User> getUserByIds(List<String> ids); // batch method
  
  List<String> ids = List("1", "2", "3");
  getUserByIds(ids).size() == ids.size();
```
Order of elements in response collection must be same as in request collection.

```
 @HystrixCommand
  public List<User> getUserByIds(List<String> ids); // batch method
  
  List<String> ids = List("1", "2", "3");
  List<User> users = getUserByIds(ids);
  System.out.println(users);
  // output
  User: id=1
  User: id=2
  User: id=3
```

**Why order of elements of request and response collections is important?**

The reason of this is in reducing logic, basically request elements are mapped one-to-one to response elements. Thus if order of elements of request collection is different then the result of execution can be unpredictable.

**Deduplication batch command request parameters**.

In some cases your batch method can depend on behavior of third-party service or library that skips duplicates in a request. It can be a rest service that expects unique values and ignores duplicates. In this case the size of elements in request collection can be different from size of elements in response collection. It violates one of the behavior principle. To fix it you need manually map request to response, for example:

```java
// hava 8
@HystrixCommand
List<User> batchMethod(List<String> ids){
// ids = [1, 2, 2, 3]
List<User> users = restClient.getUsersByIds(ids);
// users = [User{id='1', name='user1'}, User{id='2', name='user2'}, User{id='3', name='user3'}]
List<User> response = ids.stream().map(it -> users.stream()
                .filter(u -> u.getId().equals(it)).findFirst().get())
                .collect(Collectors.toList());
// response = [User{id='1', name='user1'}, User{id='2', name='user2'}, User{id='2', name='user2'}, User{id='3', name='user3'}]
return response;
```

Same case if you want to remove duplicate elements from request collection before a service call.
Example:
```java
// hava 8
@HystrixCommand
List<User> batchMethod(List<String> ids){
// ids = [1, 2, 2, 3]
List<String> uniqueIds = ids.stream().distinct().collect(Collectors.toList());
// uniqueIds = [1, 2, 3]
List<User> users = restClient.getUsersByIds(uniqueIds);
// users = [User{id='1', name='user1'}, User{id='2', name='user2'}, User{id='3', name='user3'}]
List<User> response = ids.stream().map(it -> users.stream()
                .filter(u -> u.getId().equals(it)).findFirst().get())
                .collect(Collectors.toList());
// response = [User{id='1', name='user1'}, User{id='2', name='user2'}, User{id='2', name='user2'}, User{id='3', name='user3'}]
return response;
```
To set collapser [properties](https://github.com/Netflix/Hystrix/wiki/Configuration#Collapser) use `@HystrixCollapser#collapserProperties`

Read more about Hystrix request collapsing [here] (https://github.com/Netflix/Hystrix/wiki/How-it-Works#wiki-RequestCollapsing)

**Collapser error processing**
Bath command can have a fallback method.
Example:

```java
    @HystrixCollapser(batchMethod = "getUserByIdsWithFallback")
    public Future<User> getUserByIdWithFallback(String id) {
        return null;
    }
        
    @HystrixCommand(fallbackMethod = "getUserByIdsFallback")
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
```


#Development Status and Future
Please create an issue if you need a feature or you detected some bugs. Thanks

**Note**: Javaniva 1.4.+ is updated more frequently than 1.3.+ hence 1.4+ is more stable. 

**It's recommended to use Javaniva 1.4.+** 
