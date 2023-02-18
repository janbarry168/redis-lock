package project.janbarry.redislock.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface RedisService {

    <T> void setCacheWithLock(String key, Supplier<T> getter, Consumer<T> setter);

}
