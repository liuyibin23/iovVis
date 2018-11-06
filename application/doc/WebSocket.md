# WebSocket

## Handler URL

WebSocketConfiguration.java中./api/ws/plugins

```java
"/api/ws/plugins/"
```

连接之前

```java
public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                            Map<String, Object> attributes) throws Exception
```

check user authorized.

成功连接后

```java
    public WebSocketHandler wsHandler() {
        return new TbWebSocketHandler();
    }
```

在TbWebSocketHandler.java中

```java
public class TbWebSocketHandler extends TextWebSocketHandler implements TelemetryWebSocketMsgEndpoint 
```

处理连接层面。主要是handleTextMessage,afterConnectionEstablished,handleTransportError,afterConnectionClosed,processInWebSocketService.在之中主要处理函数为handleTextMessage.其中具体处理数据的类是

DefaultTelemetryWebSocketService.java

```java
public class DefaultTelemetryWebSocketService implements TelemetryWebSocketService
```

其中主要处理函数中handleWebSocketMsg.具体协议分为三种:AttrSubCmds,TsSubCmds,HistoryCmds.

```java
accessValidator.validate(sessionRef.getSecurityCtx(), entityId,
        on(r -> Futures.addCallback(tsService.findAll(entityId, queries), callback, executor), callback::onFailure));
```

调用tsService.findAll(entityId, queries)，使用executor线程池。完成后调用callback回调。

![1540966757421](C:\Users\ShenJi\AppData\Roaming\Typora\typora-user-images\1540966757421.png)