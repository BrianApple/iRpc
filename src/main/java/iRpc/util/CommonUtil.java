package iRpc.util;

import io.netty.util.HashedWheelTimer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Random;

/**
 * @Description
 * @Author yangcheng
 * @Date 2021/3/6
 */
public class CommonUtil {
    public static HashedWheelTimer timer = new HashedWheelTimer();
    public static Random random = new Random();
    public static long getSeq(){
        long val =  LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli()+random.nextInt(200);;
        return val ;
    }




 
}
