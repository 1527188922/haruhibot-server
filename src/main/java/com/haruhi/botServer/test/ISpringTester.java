package com.haruhi.botServer.test;

public interface ISpringTester {

    /**
     * 测试代码执行入口方法
     */
    void test();

    /**
     *
     * @return 为false则不会执行test()方法
     */
    boolean enable();
}
