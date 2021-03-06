package iRpc.future;


import iRpc.dataBridge.ResponseData;

/**
 * @description: 自定义实现Future
 */
public class DefaultFuture {
    private ResponseData rpcResponse;
    private volatile boolean isSucceed = false;
    private final Object object = new Object();

    public ResponseData getRpcResponseBySyn(int timeout) {
        synchronized (object) {
            while (!isSucceed) {
                try {
                    object.wait(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return rpcResponse;
        }
    }

    public void setResponse(ResponseData response) {
        if (isSucceed) {
            return;
        }
        synchronized (object) {
            this.rpcResponse = response;
            this.isSucceed = true;
            object.notify();
        }
    }
}
