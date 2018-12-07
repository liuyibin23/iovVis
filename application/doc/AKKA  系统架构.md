# AKKA  系统架构

本系统中只有一个AKKA根节点，ACTOR_SYSTEM_NAME。同时创建4个顶级actor处理分支节点：appActor/sessionManagerActor/rpcManagerActor/statsActor。

创建系统AKKA时，时按照./conf/actor-system.conf中的配置创建。

```java
actorContext.setActorService(this);
system = ActorSystem.create(ACTOR_SYSTEM_NAME, actorContext.getConfig());
actorContext.setActorSystem(system);

appActor = system.actorOf(Props.create(new AppActor.ActorCreator(actorContext)).withDispatcher(APP_DISPATCHER_NAME), "appActor");
actorContext.setAppActor(appActor);

sessionManagerActor = system.actorOf(Props.create(new SessionManagerActor.ActorCreator(actorContext)).withDispatcher(CORE_DISPATCHER_NAME),
                                     "sessionManagerActor");
actorContext.setSessionManagerActor(sessionManagerActor);

rpcManagerActor = system.actorOf(Props.create(new RpcManagerActor.ActorCreator(actorContext)).withDispatcher(CORE_DISPATCHER_NAME),
                                 "rpcManagerActor");

ActorRef statsActor = system.actorOf(Props.create(new StatsActor.ActorCreator(actorContext)).withDispatcher(CORE_DISPATCHER_NAME), "statsActor");
actorContext.setStatsActor(statsActor);

```

