package iRpc.base;



import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import iRpc.dataBridge.RequestData;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * 
 * @Description: 
 * @author  Internet
 * @date:   2019年3月20日
 */
public class SerializationUtil {

    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<Class<?>, Schema<?>>();

    private SerializationUtil() {
    }
    /**
     * 获取类的schema
     * @param cls
     * @return
     */
    @SuppressWarnings("unchecked")
	private static <T> Schema<T> getSchema(Class<T> cls) {
    	if(!cachedSchema.containsKey(cls)){
    		Schema<T> schema = RuntimeSchema.getSchema(cls);
    		cachedSchema.put(cls, schema);
    		return schema;
    	}
        return (Schema<T>) cachedSchema.get(cls);
    }

    /**
     * 序列化（对象 -> 字节数组）
     */
    @SuppressWarnings("unchecked")
    public static <T> byte[] serialize(T obj) {
        Class<T> cls = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(cls);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);//序列化
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 反序列化（字节数组 -> 对象）
     */
    public static <T> T deserialize(byte[] data, Class<T> cls) {
        try {
        	Schema<T> schema = getSchema(cls);//获取类的schema
//            T message = (T) objenesis.newInstance(cls);//实例化
            T message = schema.newMessage();//实例化
            ProtostuffIOUtil.mergeFrom(data, message, schema);
            return message;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
    
    public static void main(String[] args) {
    	RequestData data = new RequestData();
		Object[] arg = new Object[2];
		arg[0] = "arg1";
		arg[1] = 2;
		data.setArgs(arg);
		
		data.setRequestNum("12121");
		
		Class<?>[] clazzs = new Class<?>[2];
		clazzs[0] = String.class;
		clazzs[1] = Integer.class;
		data.setParamTyps(clazzs);
		
		byte[] sData = serialize(data);
		
		RequestData DRequestData = deserialize(sData,RequestData.class);
		
	}
}
