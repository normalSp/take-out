package com.sky.context;

public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static ThreadLocal<Long> threadLocalShop = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void setCurrentShopId(Long shopId) {
        threadLocalShop.set(shopId);
    }

    public static Long getCurrentShopId() {
        return threadLocalShop.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
