package com.iwhalecloud.retail.oms.cache.redis;

import com.iwhalecloud.retail.oms.cache.ICache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisCluster;

import javax.annotation.Resource;
import java.io.*;

/**
 * 操作redis缓存实现类
 *
 * @author Z
 * @date 2018/11/23
 */
@Service
public class RedisCache implements ICache {


    @Autowired
    private JedisCluster jedisCluster;


    @Override
    public Serializable get(int nameSpace, String key) {
        final byte[] cacheKey = getKey(nameSpace,key).getBytes();

        byte[] cacheValue = jedisCluster.get(cacheKey);
        return (Serializable)byteToObject(cacheValue);
    }

    @Override
    public void set(int nameSpace, String key, Serializable value) {
        final byte[] cacheKey = getKey(nameSpace,key).getBytes();
        final byte[] cacheValue = objectToByte(value);
        this.jedisCluster.set(cacheKey,cacheValue);
    }

    @Override
    public void set(int nameSpace, String key, Serializable value, int expireTime) {
        final byte[] cacheKey = getKey(nameSpace,key).getBytes();
        this.set(nameSpace,key,value);

        this.jedisCluster.expire(cacheKey,expireTime);
    }

    @Override
    public void delete(int nameSpace, String key) {
        final byte[] cacheKey = getKey(nameSpace,key).getBytes();
        this.jedisCluster.del(cacheKey);
    }

    @Override
    public void clear(int nameSpace) {
        final byte[] cacheKey = getKey(nameSpace,"*").getBytes();
        this.jedisCluster.del(cacheKey);
    }


    private String getKey(int nameSpace, String key) {
        key =nameSpace+"_"+key;
        return key;
    }

    private byte[] objectToByte(java.lang.Object obj) {
        byte[] bytes =null;
        ByteArrayOutputStream bo = null;
        ObjectOutputStream oo = null;
        try {
            bo = new ByteArrayOutputStream();
            oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);
            bytes = bo.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != bo) {
                    bo.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (null != oo) {
                    oo.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return (bytes);
    }

    private static Object byteToObject(byte[] bytes){
        java.lang.Object obj=null;
        ByteArrayInputStream bi = null;
        ObjectInputStream oi = null;
        try {
            bi = new ByteArrayInputStream(bytes);
            oi = new ObjectInputStream(bi);
            obj = oi.readObject();
        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if (null != bi) {
                    bi.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (null != oi) {
                    oi.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return obj;
    }
}
