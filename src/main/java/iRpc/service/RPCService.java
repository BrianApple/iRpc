package iRpc.service;

import java.lang.annotation.*;

/**
 * Rpc服务注解
 * @Description: 
 * @author  yangcheng
 * @date:   2019年3月18日
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD})
@Documented
public @interface RPCService {
	String value() default "";

}
