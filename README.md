# Garbage Lang
A slow and useless Programming Language.</br>
The Language has ***no*** keywords but uses function calls to native functions instead.</br>
This makes the Language even slower, and it also makes compile-time checks for basically anything impossible.<br>
Error messages were a very important design part and I tried to make them as horrible as possible<br>
**Enjoy :3**

### Hello World Program:
```garbageLang
IO.print('Hello World')
```

### Variables:

Because we don't have keywords we use function calls to define or access variables

x = 1:
```garbageLang
Vars.x(1)
```
Access x by using (Vars.x is a function call with 0 arguments which means that we can remove parentheses)
```garbageLang
Vars.x
Vars.x()
```
Or use the alternate method:
```garbageLang
Vars.setVar('x', 1)
Vars.getVar('x')
```

### Scoped-Expression:

Expressions that are wrapped in **{** and **}** will be passed around tokens at runtime which means that it can be used for callbacks at runtime etc.
```garbageLang
Vars.scope({
    IO.print('Hello')
})
```
Now let's execute the scope via an std library function
```garbageLang
Runtime.executeScope(Vars.scope)
```

### Loops:

Loops are also std library function calls.

#### If

```garbageLang
Runtime.If({ Math.equals(1, 1) }, {
    IO.print('1 == 1')
}, {
    IO.print('1 != 1')
})
```
#### While

```garbageLang
Runtime.While({ Math.equals(1, 1) }, {
    IO.print('infinity')
}
```


#### Match
```garbageLang
Runtime.Match(Vars.x)
.when(1, {
    IO.print('x is 1')
})
.when(2, {
    IO.print('x is 2')
})
.when('Hello World', {
    IO.print('x is the string: Hello World')
})
.else({
    IO.print('x is something else')
}).run()
```

### Functions:

Due to the language having no keywords, 
functions can be defined dynamically at runtime using a function call to the std library.
Functions that have no Arguments can be called without parentheses
```garbageLang
Func.define.test('arg1', 'arg2', {
    IO.print(arg1, arg2)
})
```
You can call a function dynamically by using an std library function
```garbageLang
Func.test(1, 2)
```
The output will be<br> **1 <br>2**

Functions have an implicit return which means that the last statement that's executed will be returned
### Function Optimization:
A pure function that doesn't have any side effects can be optimized 
by collecting the results of the function when its executed. 
If the function is called multiple times with the same arguments the already calculated result is read and returned.
You can do this using a built-in function in the std library

For Example:
```garbageLang
Func.define.fibonacci('i', {
    Runtime.Match(Vars.i).when(0, {
        0
    }).when(1, {
        1
    }).else({
        Math.add(Func.fibonacci(Math.sub(Vars.i, 1)), Func.fibonacci(Math.sub(Vars.i, 2)))
    }).run()
})
```
You can optimize this function by calling
```garbageLang
Func.wrapPure('fibonacci')
```
The function is now wrapped by another function that collects the return values of the Fibonacci function. This makes the Fibonacci function way faster

**Note: Functions that produce side effects or use external variables do not work correctly if u apply this optimization function to it**

### Lists

A list can be created by using 
```garbageLang
List.create(1, 2, true, 'sth)
```
or the special list syntax
```garbageLang
[1, 2, true, 'sth']
```

## Std Library
The std library is not documented yet, but you can look at its definition in the **LibFactory.kt** file

## Expandability 
The language can be extended by plugins written in Java, Kotlin, or any other JVM Language.
Here is an example:

Main Class:

```java
public class Main extends Plugin {
    @Override
    public void onEnable() {
        ExtensionLoader.INSTANCE.registerExtension(new WebApi());
    }
}
```

The main class registers all extensions<br>
Extensions Can be called like that:
```garbageLang
ExtensionName.functionName(args)
```

WebApi Class:
```java
public class WebApi implements Extension {
    @Function
    public WebServer createWebServer(Double port) {
        return new WebServer(port.intValue());
    }

}
```

Functions that are callable from GBLang code must be annotated with the @Function annotation.
This tells the interpreter that this function is Callable. 
Functions should only return primitive Data types such as:
- String
- Int
- Double<br>

Or Classes that implement the **Component** or **Extension** interface.

WebServer Class:
```java
public class WebServer implements Component {

    private final HttpServer server;

    private final Map<String, PathHandler> routes;

    private Consumer<WebServer> startupTask = null;

    public WebServer(int port){
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        routes = new HashMap<>();
    }

    @Function
    public void start() {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        routes.forEach(server::createContext);
        server.setExecutor(threadPoolExecutor);
        server.start();
        if (startupTask != null)
            startupTask.accept(this);
    }

    private PathHandler getPath(String path)  {
        if (!routes.containsKey(path))
            routes.put(path, new PathHandler());
        return routes.get(path);
    }

    @Function
    public void get(String path, Consumer<Object> consumer) {
        getPath(path).registerGet(consumer);
    }

    @Function
    public void post(String path, Consumer<Object> consumer) {
        getPath(path).registerPost(consumer);
    }

    @Function
    public void stop() {
        server.stop(0);
    }

    @Function
    public void onReady(Consumer<WebServer> consumer) {
        startupTask = consumer;
    }
}
```
Every function that is annotated with the **@Function** annotation is callable from gb lang.
Here is an example webserver:
```garbageLang
Vars.visits(0)

Vars.server(WebApi.createWebServer(8001))

Vars.server.get('/', Consumer.req({
    Vars.visits(Math.add(1, Vars.visits))
    Vars.req.respond('<h1>Hello World</h1>')
}))

Vars.server.get('/test', Consumer.req({
    Vars.req.respond('<p>Info page</p>')
}))

Vars.server.get('/quit', Consumer.req({
    Vars.req.respond('<p>Shutting down</p>')
    IO.print(Vars.visits, ' visits')
    System.getProgramControl.exit(0)
}))

Vars.server.start()
IO.print('Server Listening on Port: 8001')
```
