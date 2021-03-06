package iRpc.dataBridge;

import java.io.Serializable;
import java.util.List;
/**
 * 封装响应参数
 * @Description: 
 * @author  yangcheng
 * @date:   2019年3月17日
 */
public class ResponseData implements Serializable{
	private static final long serialVersionUID = 1341548752135718024L;
	private String responseNum;
	/**
	 * 返回状态码
	 * 200 成功
	 * 500 失败
	 */
	private int returnCode = 200;
	private Object data;
	private Throwable erroInfo;

	public ResponseData(String responseNum, int returnCode) {
		super();
		this.responseNum = responseNum;
		this.returnCode = returnCode;
	}

	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}

	public Throwable getErroInfo() {
		return erroInfo;
	}

	public void setErroInfo(Throwable erroInfo) {
		this.erroInfo = erroInfo;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public String getResponseNum() {
		return responseNum;
	}

	public void setResponseNum(String responseNum) {
		this.responseNum = responseNum;
	}
	
}
