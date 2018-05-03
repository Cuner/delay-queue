package org.cuner.delay.queue.cluster;

import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by houan on 18/5/2.
 */
public class RedisDistributedLock {

    private JedisPool jedisPool;

    private static final String LOCK_PREFIX = "lock_";

    private long lockTimeout;

    /**
     * @param host
     * @param port
     * @param lockTimeout 毫秒
     */
    public RedisDistributedLock(String host, int port, long lockTimeout) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(5);
        config.setMaxIdle(1);
        config.setMaxWaitMillis(2000);
        jedisPool = new JedisPool(config, host, port);
        this.lockTimeout = lockTimeout;
    }

    /**
     * 阻塞式分布式锁
     */
    public void lock(String key) {
        String lockKey = LOCK_PREFIX + key;

        while (true) {
            //防止 jedis链接断开导致消费失败
            Jedis jedis = jedisPool.getResource();
            try {
                Long locked = jedis.setnx(lockKey, String.valueOf(System.currentTimeMillis()));
                if (locked != null && locked == 1) {
                    //获取到了锁 跳出循环
                    return;
                }

                //没获取到锁 自动判断是否超时
                String lockTime = jedis.get(lockKey);
                long now = System.currentTimeMillis();
                if (StringUtils.isNotBlank(lockTime) && now - Long.parseLong(lockTime) > lockTimeout) {
                    //原有的key已经超时失效了
                    String oldLockTime = jedis.getSet(lockKey, String.valueOf(System.currentTimeMillis()));
                    now = System.currentTimeMillis();
                    if (StringUtils.isNotBlank(oldLockTime) && now - Long.parseLong(oldLockTime) > lockTimeout) {
                        //表示之前的锁已经过期 且 已经获取到了锁(设置了新的value)
                        return;
                    }

                }
            } finally {
                jedis.close();
            }
        }
    }

    public void unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        Jedis jedis = jedisPool.getResource();
        try {
            jedis.del(lockKey);
        } finally {
            jedis.close();
        }
    }

}
