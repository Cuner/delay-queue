package org.cuner.delay.queue.cluster;

import com.google.gson.Gson;
import org.apache.commons.collections4.CollectionUtils;
import org.cuner.delay.queue.DelayMessage;
import org.cuner.delay.queue.DelayQueue;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;

/**
 * Created by houan on 18/5/3.
 */
public class RedisSynDelayQueue implements DelayQueue {

    private String queueName;

    private JedisPool jedisPool;

    private long delay;

    private boolean syn;

    private Gson gson = new Gson();

    private RedisDistributedLock lock;

    private static final String QUEUE_PREFIX = "syn_redis_delay_queue_";

    /**
     * @param queueName
     * @param delay      毫秒
     * @param syn
     * @param redisHost
     * @param redisPort
     */
    public RedisSynDelayQueue(String queueName, long delay, boolean syn, String redisHost, int redisPort) {
        this.queueName = QUEUE_PREFIX + queueName;
        this.syn = syn;
        this.delay = delay * 1000000;

        if (this.syn) {
            lock = new RedisDistributedLock(redisHost, redisPort,  10000);
        }

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(5);
        config.setMinIdle(1);
        config.setMaxWaitMillis(2000);
        jedisPool = new JedisPool(config, redisHost, redisPort);

    }

    public String getQueueName() {
        return this.queueName;
    }

    public boolean push(String message) {
        Jedis jedis = jedisPool.getResource();
        try {
            DelayMessage delayMessage = new DelayMessage(delay, null, message);
            Long result = jedis.rpush(this.queueName, gson.toJson(delayMessage));
            return result != null && result != 0;
        } finally {
            jedis.close();
        }
    }

    public DelayMessage pop() {
        while (true) {
            Long waitTime = null;
            Jedis jedis = jedisPool.getResource();
            try {
                if (this.syn) {
                    lock.lock(this.queueName);
                }
                //获取头部的数据
                List<String> dataList = jedis.lrange(this.queueName, 0L, 0L);
                if (CollectionUtils.isNotEmpty(dataList)) {
                    String data = dataList.get(0);
                    DelayMessage message = gson.fromJson(data, DelayMessage.class);
                    if (message != null) {
                        long now = System.nanoTime();
                        if (message.getExpire() > now) {
                            //没有到期
                            waitTime = message.getExpire() - now;
                        } else {
                            jedis.lpop(this.queueName);
                            return message;
                        }
                    }
                }

                if (waitTime != null) {
                    try {
                        Thread.sleep(waitTime / 1000);
                    } catch (InterruptedException e) {
                        //do nothing
                    }
                }

            } finally {
                if (this.syn) {
                    lock.unlock(this.queueName);
                }
                jedis.close();
            }
        }
    }

    public void ack(String tmpKey) {
    }

    public long length() {
        Jedis jedis = jedisPool.getResource();
        try {
            Long length = jedis.llen(this.queueName);
            if (length != null && length > 0) {
                return length;
            } else {
                return 0L;
            }
        } finally {
            jedis.close();
        }
    }

    public boolean clean() {
        Jedis jedis = jedisPool.getResource();
        try {
            Long result = jedis.del(this.queueName);
            return result != null;
        } finally {
            jedis.close();
        }
    }

    public long getDelay() {
        return this.delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }
}
