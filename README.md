# iRpc

### 介绍
iRpc为一款基于Nio通信实现的轻量级高性能rpc框架,支持单机及leader-follower部署模式,配置简单，通信效率高。
提供同步和异步(callBack模式)等多种消息发送方式,不依赖于第三方注册中心即可实现服务端自主选举（选举模块基于dleger源码改造），

### 启动类

#### ServerStarter 启动类

- 构造方法

|      构造方法      | 作用 |
|------------- |----------|
| 		ServerStarter()	   |	默认加载配置文件名为"application.yml"的配置文件，加载位置默认为resources目录下  |
| 		ServerStarter(String pathName)	   |	指定配置yml文件名称,格式为xxx.yml  | 
| 		ServerStarter(IRpcServerProperty property)	   |	通过javaBean方式配置服务端信息  | 
| 		...	   |	带扩展  | 

- server端yml配置信息详解
  
  |      参数名称      | 含义 |
  |------------- |----------|
  | 		iRpcServer   | iRpc服务端配置信息	  |
  | 		serverPort	   |	服务端监听端口  |
  | 		heartbeat	   |	服务端监测客户端心跳周期-暂未使用  |
  | 		nodeName	   |	当前服务节点名称，为ClusterNode-node值  |
  | 		ClusterNode   |	 leader-flower集群模式节点信息，集群模式下多个节点的ClusterNode信息一样 |
  | 		node   |	节点名称(建议使用：n0-nx，沿用的是dleger的模式)，  |
  | 		ip   |	iRpcServer节点ip地址（或域名）  |
  | 		port   |	iRpcServer节点服务端口  |

- yml配置信息模版

```
## iRpc服务端配置信息
iRpcServer:
  serverPort: 10916
  heartbeat: 60 # iRpc服务端检测iRpc客户端连接状态的最大心跳周期。
  nodeName: n0
  ClusterNode: #如果使用单机，则不配置该项,node从n1开始，n0属于localhost
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


#### ClientStarter 启动类

- 构造方法

|      构造方法      | 作用 |
|------------- |----------|
| 		ClientStarter()	   |	默认加载配置文件名为"application.yml"的配置文件，加载位置默认为resources目录下  |
| 		ClientStarter(String pathName)	   |	指定配置yml文件名称,格式为xxx.yml  | 
| 		 ClientStarter(IRpcClientProperty property)	   |	通过javaBean实例化配置信息启动客户端  | 
| 		...	   |	带扩展  | 

- 配置参数详解

  |      参数名称      | 含义 |
    |------------- |----------|
  | 		iRpcClient   | iRpc客户端配置信息	  |
  | 		retryTimes	   |	serverNode节点网络连接失败重试次数，默认为3次  |
  | 		serverModCluster	   | true/false	服务端是否为leader-flower模式  |
  | 		serverNode   |	 iRpc server端节点信息 |
  | 		ip   |	iRpcServer节点ip地址（或域名）  |
  | 		port   |	iRpcServer节点服务端口  |

- yml配置信息模版

```
#iRpc客户端配置信息
iRpcClient:
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



### 消息发送
消息发送核心类为iRpc.base.messageDeal.MessageSender

|     方法名称      | 发送模式 |返回结果 |
|------------- |----------|----------|
| 		synBaseMsgSend   | 同步	  |	 ResponseData对象，returncode != 200 发送失败 |
| 		asynBaseMsgSend	   |	异步  | boolean,发布成功或失败，通过回调方式异步处理方法执行结果  |


![客户端消息发送流程图](https://images.gitee.com/uploads/images/2021/0410/123030_25b15048_1038477.png "客户端消息发送流程图.png")


### 启动流程
![流程](https://images.gitee.com/uploads/images/2021/0328/113300_2ae87b28_1038477.png "流程.png")

### 如何引入

iRpc发行版本已同步到maven中央仓库，因此根据项目实际情况选择合适版本引入即可

```
<dependency>
  <groupId>io.github.brianapple</groupId>
  <artifactId>iRpc</artifactId>
  <version>2.0.1-Release</version>
</dependency>
```
### 版本
- 1.0.2.Release 修复了1.0.1及之前版本，客户端无法正常启动的异常。
- 2.0.1 支持集群动态扩展，支持配置信息通过javaBean方式配置
- 2.0.3 完善集群扩展功能,支持"AB + C(A) -> ABC"、"AB + C(AB) -> ABC"等动态扩容模式。测试方法见iRpc.serverTest包中测试类 
### 客户端demo

```
public class Test {
	public static void main(String[] args) {
		ServerRpc();
	}
	/**
	 * rpc服务端
	 */
	public static void ServerRpc(){
		ClientStarter clientStarter = new ClientStarter();
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		while(true) {
			/**
			 * 同步消息发送
			 */
			Class<? >[] classType = new Class[]{String.class};
			Object[] argsData = new Object[]{"world"};
			ResponseData ret = MessageSender.synBaseMsgSend(false,
					"iRpc.rpcService.RPCExportServiceImpl",
					"test",
					classType,
					argsData,
					5000);

			System.out.println("客户端同步收到数据："+ret.getData());
			//控制台输出：客户端同步收到数据：hello world
		}
	}

}
```

### 集群选举及扩容功能

#### raft选举
基本集群基于raft选举算法实现节点自举，当前选举模式下，节点包含当前集群下所有节点信息

![raft选举](https://images.gitee.com/uploads/images/2021/0410/101957_1cf5ac1f_1038477.png "raft选举.png")
#### 集群扩容-选举过程依然基于raft
扩容节点和原集群groupName必须一致，且iRpc不支持两个存在leader节点的集群合并扩容

##### AB + C(A) 或  AB + C(B) -> ABC
AB基于raft算法自举选出leader，C节点只携带AB集群中的一部分节点信息参与扩容，最终达成选举一致。
![AB + C(A) 或  AB + C(B) -> ABC](https://images.gitee.com/uploads/images/2021/0410/102039_9b06539c_1038477.png "扩容.png")

##### AB + C(AB) -> ABC
AB基于raft算法自举选出leader，C节点携带原集群所有节点信息且groupName一致，参与集群扩容
![AB + C(AB) -> ABC](https://images.gitee.com/uploads/images/2021/0410/103413_87b10f09_1038477.png "AB + C(AB) -> ABC.png")


### 教程
於之博客 :100: https://www.xianglong.work/tag/iRpc
於之CSDN :100: https://blog.csdn.net/sinat_28771747/category_10967790.html
### 感谢
Netty 项目及作者，项目地址： https://github.com/netty/netty
dledger 项目及作者，项目地址： https://github.com/openmessaging/openmessaging-storage-dledger
IOTGate 项目及作者，项目地址：https://gitee.com/willbeahero/IOTGate

### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request

