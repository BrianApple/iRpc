package iRpc.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @Description
 * @Author yangcheng
 * @Date 2021/3/6
 */
public class CommonUtil {
    public static long getSeq(){
        long val =  LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli();
        return val ;
    }



 
}
