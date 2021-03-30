# iRpc

#### 介绍
iRpc为一款基于netty实现的轻量级高性能rpc框架,支持单机及leader-follower部署模式,配置简单，通信效率高，优先适用于物联网应用场景。

#### 软件架构
软件架构说明


#### 消息类型
0.  心跳消息
1.  基本消息类型
2.  选举消息，服务节点选举使用

#### 使用说明

1.  iRpc.base.starter.ClientStarter 为客户端启动模块：当应用服务作为iRpc客户端时需手动实例化该实例，默认配置文件为application.yml；
    配置文件可以通过带参构造方法动态指定；
2.  iRpc.base.starter.ServerStarter 为服务端启动模块：当应用服务作为iRpc服务端时需手动实例化该实例，默认配置文件为application.yml；
    配置文件可以通过带参构造方法动态指定；



#### 配置信息详解

```
#iRpc客户端配置信息
client:
  retryTimes: 3
  serverModCluster: true
  serverNode:
    - ip: 127.0.0.1 # 指定iRpc客户端连接的iRpc服务端节点信息，默认与第一个节点建立连接。
      port: 10916
    - ip: 127.0.0.1
      port: 10917
    - ip: 127.0.0.1
      port: 10918
```


```
## iRpc服务端配置信息
server:
  serverPort: 10916
  heartbeat: 60 # iRpc服务端检测iRpc客户端连接状态的最大心跳周期。
  ClusterNode: #如果使用单机，则不配置该项,node从n1开始，n0属于localhost,基于raft算法选举leader节点
    - node: n0
      ip: 127.0.0.1
      port: 10916
    - node: n1
      ip: 127.0.0.1
      port: 10917
    - node: n2
      ip: 127.0.0.1
      port: 10918

```


#### 启动流程
![流程](https://images.gitee.com/uploads/images/2021/0328/113300_2ae87b28_1038477.png "流程.png")

#### 如何引入
![maven](https://images.gitee.com/uploads/images/2021/0330/212958_4046ad06_1038477.png "maven.png")


```
<dependency>
  <groupId>io.github.brianapple</groupId>
  <artifactId>iRpc</artifactId>
  <version>1.0.1-Release</version>
</dependency>
```


#### 感谢
Netty 项目及作者，项目地址： https://github.com/netty/netty

dledger 项目及作者，项目地址： https://github.com/openmessaging/openmessaging-storage-dledger


#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request

