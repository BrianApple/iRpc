package iRpc.util;

import java.time.LocalDateTime;

/**
 * @Description
 * @Author yangcheng
 * @Date 2021/3/6
 */
public class CommonUtil {
    public static long getSeq(){
        long val = LocalDateTime.now().toLocalTime().toSecondOfDay();
        return val ;
    }



 
}
