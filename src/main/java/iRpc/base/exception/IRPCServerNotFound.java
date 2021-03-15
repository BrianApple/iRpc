package iRpc.base.exception;

/**
 * @Description
 * @Author yangcheng
 * @Date 2021/3/14
 */
public class IRPCServerNotFound extends RuntimeException{
    public IRPCServerNotFound(String message, Throwable cause) {
        super(message, cause);
    }

    public IRPCServerNotFound(String message) {
        super(message);
    }
}
