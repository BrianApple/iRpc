package iRpc.dataBridge;

import java.io.Serializable;
/**
 * 封装请求参数
 * @Description: 
 * @author  yangcheng
 * @date:   2019年3月17日
 */
public class RequestData implements  IDataSend , Serializable{
	private static final long serialVersionUID = -497072374733332517L;
	private boolean isBroadcast;//是否广播到所有网关节点
	private String requestNum;//请求编号
	/**
	 * 全类名
	 */
	private String className;
	private String methodName;
	/**
	 * 可选参数，服务端可自动识别参数的简单Class类型
	 */
	private Class<?>[] paramTyps;
	private Object[] args;
	
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	public Class<?>[] getParamTyps() {
		return paramTyps;
	}
	public void setParamTyps(Class<?>[] paramTyps) {
		this.paramTyps = paramTyps;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	public Object[] getArgs() {
		return args;
	}
	public void setArgs(Object[] args) {
		this.args = args;
	}
	public String getRequestNum() {
		return requestNum;
	}
	public void setRequestNum(String requestNum) {
		this.requestNum = requestNum;
	}
	public boolean isBroadcast() {
		return isBroadcast;
	}
	public void setBroadcast(boolean isBroadcast) {
		this.isBroadcast = isBroadcast;
	}
}
