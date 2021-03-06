package com.iwhalecloud.retail.goods2b.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 列表拷贝工具类
 * @author z
 * @date 2019/3/5
 */
public class CloneUtils {

    /**
     * 拷贝对象
     * @param source
     * @param classType
     * @return
     */
    public static <T, E> E clone(T source, Class<E> classType) {

        if (source == null) {
            return null;
        }
        E targetInstance = null;
        try {
            targetInstance = classType.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        BeanUtils.copyProperties(source, targetInstance);
        return targetInstance;
    }

    /**
     * 拷贝数组对象
     * @param sourceList
     * @param classType
     * @return
     */
    public static <T, E> List<E> batchClone(List<T> sourceList, Class<E> classType) {
        if (sourceList == null) {
            return null;
        }
        List<E> result = new ArrayList<E>();
        int size = sourceList.size();
        for (int i = 0; i < size; i++) {
            E e = clone(sourceList.get(i), classType);
            if (e != null) {
                result.add(e);
            }
        }
        return result;
    }

}
