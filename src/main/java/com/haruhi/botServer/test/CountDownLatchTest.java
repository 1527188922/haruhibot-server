package com.haruhi.botServer.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CountDownLatchTest {


    public static void main(String[] args) {
        int count = 10;
        CountDownLatch downLatch = new CountDownLatch(count);
        long l = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            new Thread(()->{
                try {
                    Thread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    System.out.println(Thread.currentThread().getName());
                    downLatch.countDown();
                }
            }).start();
        }
        try {
            downLatch.await(999,TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("耗时："+(System.currentTimeMillis() - l));
        System.out.println("out downLatch.getCount()="+downLatch.getCount());//未执行完的线程数量
        System.out.println("end...");
    }

}
