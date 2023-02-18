package project.janbarry.redislock.service.impl;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import project.janbarry.redislock.service.RedisService;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RedisServiceImpl implements RedisService {

    public static final int TTL_SECOND = 5000;
    public static final int WAIT_SECOND = 3000;

    private final RedissonClient redissonClient;

    @Autowired
    public RedisServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> void setCacheWithLock(String key, Supplier<T> getter, Consumer<T> setter) {
        RLock rLock = redissonClient.getSpinLock(key + "_LOCK");
        try {
            long t1 = System.nanoTime();
            boolean result = rLock.tryLock(WAIT_SECOND, TTL_SECOND, TimeUnit.SECONDS);
            if (result) {
                T t = getter.get();
                long t2 = System.nanoTime();
                long elapsedSecond = TimeUnit.NANOSECONDS.toSeconds(t2 - t1);
                if (elapsedSecond < TTL_SECOND) {
                    setter.accept(t);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            rLock.unlock();
        }
    }

}
