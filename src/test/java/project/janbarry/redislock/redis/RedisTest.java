package project.janbarry.redislock.redis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class RedisTest {

    private static final int WAIT_SECOND = 5;
    private static final int TTL_SECOND = 3;
    private static final String CACHE_KEY = "CacheKey";
    private static final String CACHE_VALUE = "CacheValue";

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    private RLock rLock;

    @BeforeEach
    void before() {
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        rLock = redissonClient.getSpinLock(CACHE_KEY);
    }

    @Test
    void testLock() throws InterruptedException {
        int timeout = TTL_SECOND;
        redisTemplate.opsForValue().set(CACHE_KEY, CACHE_VALUE, timeout, TimeUnit.SECONDS);
        watchLock(CACHE_KEY, () -> redisTemplate.opsForValue().get(CACHE_KEY) != null);
    }

    @Test
    void testRedissonLock() throws InterruptedException {
        rLock.lock(TTL_SECOND, TimeUnit.SECONDS);
        assertTrue(rLock.isHeldByCurrentThread());
        watchLock(CACHE_KEY, () -> rLock.isHeldByCurrentThread());
        assertFalse(rLock.isLocked());
    }

    @Test
    void testRedissonDoubleLock() {
        Arrays.asList("1", "2").stream().parallel().forEach(s -> {
            try {
                rLock.lock(TTL_SECOND, TimeUnit.SECONDS);
                watchLock(CACHE_KEY, () -> rLock.isHeldByCurrentThread());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testRedissonUnLock() {
        rLock.lock(TTL_SECOND, TimeUnit.SECONDS);
        assertTrue(rLock.isHeldByCurrentThread());
        rLock.unlock();
        assertFalse(rLock.isHeldByCurrentThread());
        assertFalse(rLock.isLocked());
    }

    @Test
    void testRedissonUnLockFail() throws InterruptedException {
        rLock.lock(TTL_SECOND, TimeUnit.SECONDS);
        assertTrue(rLock.isHeldByCurrentThread());
        Thread.sleep((TTL_SECOND + 1) * 1000);
        Assertions.assertThrows(IllegalMonitorStateException.class, () -> rLock.unlock());
    }

    @Test
    void testRedissonTryLock() throws InterruptedException {
        boolean result = rLock.tryLock(WAIT_SECOND, TTL_SECOND, TimeUnit.SECONDS);
        assertTrue(result);
        watchLock(CACHE_KEY, () -> rLock.isHeldByCurrentThread());
    }

    @Test
    void testRedissonDoubleTryLock() {
        Arrays.asList("1", "2").stream().parallel().forEach(s -> {
            try {
                boolean result = rLock.tryLock(WAIT_SECOND, TTL_SECOND, TimeUnit.SECONDS);
                assertTrue(result);
                watchLock(CACHE_KEY, () -> rLock.isHeldByCurrentThread());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testRedissonDoubleTryLockTimeout() {
        Arrays.asList("1", "2").stream().parallel().forEach(s -> {
            try {
                System.out.println("thread" + Thread.currentThread().getId() + " start");
                rLock.tryLock(TTL_SECOND - 1, TTL_SECOND, TimeUnit.SECONDS);
                watchLock(CACHE_KEY, () -> rLock.isHeldByCurrentThread());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void watchLock(String key, Supplier<Boolean> isLock) throws InterruptedException {
        int count = 1;
        long threadId = Thread.currentThread().getId();
        while (isLock.get()) {
            System.out.println("thread" + threadId + ": time=" + (count++));
            Thread.sleep(1000);
        }
        System.out.println("thread" + threadId + " done");
    }

}
