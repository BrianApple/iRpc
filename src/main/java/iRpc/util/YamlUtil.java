package iRpc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author yangcheng
 * @Date 2021/3/6
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * type.yml 配置文件对应下拉集合显示
 * @author Lenovo
 */
public class YamlUtil {
    private static Map<String,Object> ymalMap = null;
    /**
     * 读取下拉状态配置参数返回map
     * @param pathName
     * @return
     * @throws IOException
     */
    public static Map<String,Object> getTypePropertieMap(String pathName){
        if (ymalMap == null){
            synchronized (YamlUtil.class){
                if (ymalMap == null){
                    ymalMap = new HashMap<>();
                    Yaml yaml = new Yaml();
                    ymalMap = yaml.loadAs(Thread.currentThread().getContextClassLoader().getResourceAsStream(pathName), HashMap.class);
                }
            }
        }
        return ymalMap;

    }

    public static void main(String[] args) {
        Map<String,Object> map =   getTypePropertieMap(null);
    }
}

