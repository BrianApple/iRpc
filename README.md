# iRpc

#### 介绍
iRpc为一款基于netty实现的轻量级高性能rpc框架

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
3.  application.yml配置文件：
    client.serverNode:指定iRpc客户端连接的iRpc服务端节点信息，默认与第一个节点建立连接。
    server.serverPort:iRpc服务端网络端口信息。
    server.heartbeat:iRpc服务端检测iRpc客户端连接状态的最大心跳周期。
    server.ClusterNode:iRpc服务节点集群，可以不指定，基于raft算法选举master节点

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request
