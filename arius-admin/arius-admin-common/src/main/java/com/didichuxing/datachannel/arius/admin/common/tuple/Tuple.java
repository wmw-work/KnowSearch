package com.didichuxing.datachannel.arius.admin.common.tuple;

import java.util.Map;
import java.util.Objects;

/**
 * 元组
 *
 * @author shizeying
 * @date 2022/06/20
 */
public interface Tuple {
    /**
     * max 最大设置
     */
    int MAX_ARITY = 3;
    
    /**
     * 元组大小
     *
     * @return int
     */
    int tupleSize();
    static Tuple0 empty() {
        return Tuple0.instance();
    }
    static <T1, T2> Tuple2<T1, T2> fromEntry(Map.Entry<? extends T1, ? extends T2> entry) {
        Objects.requireNonNull(entry, "entry is null");
        return new Tuple2<>(entry.getKey(), entry.getValue());
    }
    
    /**
     * @param t1 t1
     * @return {@code Tuple1<T1>}
     */
    static <T1> Tuple1<T1> of(T1 t1) {
        return new Tuple1<>(t1);
    }
    
    /**
     * @param t1 t1
     * @param t2 t2
     * @return {@code Tuple2<T1, T2>}
     */
    static <T1, T2> Tuple2<T1, T2> of(T1 t1, T2 t2) {
        return new Tuple2<>(t1, t2);
    }
    
    /**
     * @param t1 t1
     * @param t2 t2
     * @param t3 t3
     * @return {@code Tuple3<T1, T2, T3>}
     */
    static <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 t1, T2 t2, T3 t3) {
        return new Tuple3<>(t1, t2, t3);
    }
     static int hash(Object o1) {
        return Objects.hashCode(o1);
    }
    static int hash(Object o1, Object o2) {
        int result = 1;
        result = 31 * result + hash(o1);
        result = 31 * result + hash(o2);
        return result;
    }
    static int hash(Object o1, Object o2, Object o3) {
        int result = 1;
        result = 31 * result + hash(o1);
        result = 31 * result + hash(o2);
        result = 31 * result + hash(o3);
        return result;
    }
    
  
}