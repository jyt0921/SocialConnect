package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {

    /**
     * 测试生成order：id*/
    @Resource
    private RedisIdWorker redisIdWorker;

    private ExecutorService es= Executors.newFixedThreadPool(50);
    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 10; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            System.out.println("***********");
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 30; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }
}
