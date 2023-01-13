package com.haruhi.botServer.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 1:异步任务编排调用（链式调用）时（调用方法也是异步） 只有最后一个回调方法相对于caller线程来说是异步（非阻塞）的，在最后一个方法之前的方法，虽然不在caller线程执行，但是是阻塞的
 * 2:whenComplete()之后依然可以调用whenComplete()以及其他方法 不推荐（不规范） 无异常情况还好，有异常的话会产生奇怪的现象
 */
public class CompletableFutureTest {

    public static void main(String[] args) {
        test2();
    }
    public static void test2(){
        for (int i = 0; i < 4; i++) {
            CompletableFuture.runAsync(()->System.out.println(Thread.currentThread().getName()))
                    .thenRunAsync(()->{

                    })
                    .whenCompleteAsync((t,e)->{
                System.out.println(e);
            });

        }
        System.out.println("end ....");
    }

    public static void test1(){
        for (int j = 0;  j< 6; j++) {
            CompletableFuture.supplyAsync(() -> {
                        int i = 10 / 0;
                        Integer a = 1;

                        System.out.println(Thread.currentThread().getName() + " " + a);
                        return a;
                    }).thenAcceptAsync(t->{
                        System.out.println(Thread.currentThread().getName() + " run="+t);
                    })
                    .thenRunAsync(()->{
//            int i = 10 / 0;
                        System.out.println("thenRunAsync");
                    })
                    .whenCompleteAsync((t,e) ->{
                        System.out.println(Thread.currentThread().getName() + " T="+t);
                        System.out.println("e=" + e);
//            int i = 10 / 0;
                    })

//                .whenCompleteAsync((t,e)->{
//                    System.out.println("end t= " +t + " e=" +e);
//                }).thenAcceptAsync(t->{
//            System.out.println("thenAcceptAsync="+t);
//        })
            ;
            CompletableFuture.supplyAsync(()->{
                String t = "123";
                System.out.println(Thread.currentThread().getName() + " t="+t);
                return t;
            }).whenCompleteAsync((t,e)->{
                System.out.println(Thread.currentThread().getName() + "whenCompleteAsync t="+t);
            });
        }


        System.out.println("aaaaaaaaaaaaa");
//        try {
//            System.out.println(integerCompletableFuture.get());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
    }
}
