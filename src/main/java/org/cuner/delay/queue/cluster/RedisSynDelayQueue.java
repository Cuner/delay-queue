package org.cuner.delay.queue.cluster;

import com.google.gson.Gson;
import org.cuner.delay.queue.DelayMessage;
import org.cuner.delay.queue.DelayQueue;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by houan on 18/5/4.
 */
public class RedisSynDelayQueue implements DelayQueue {

    private String queueName;

    private String exeSetKey;//消息执行队列key

    private long delay;

    private boolean autoAck;

    private JedisPool jedisPool;

    private Gson gson;

    private static final String QUEUE_PREFIX = "syn_redis_delay_queue_";

    private static final String EXECUTION_KEYSET_PREFIX = "exe_key_set_";

    private static final String RANDOM_EXECUTION_PREFIX = "random_exe_";

    private static final String RECOVER_MSG_LOCK_PREFIX = "recover_msg_lock";

    public RedisSynDelayQueue(String queueName, long delay, boolean autoAck, String redisHost, int redisPort) {
        this.queueName = QUEUE_PREFIX + queueName;
        this.exeSetKey = EXECUTION_KEYSET_PREFIX + queueName;
        this.autoAck = autoAck;
        this.delay = delay * 1000000;

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(5);
        config.setMinIdle(1);
        config.setMaxWaitMillis(2000);
        jedisPool = new JedisPool(config, redisHost, redisPort);

        RedisDistributedLock lock = new RedisDistributedLock(redisHost, redisPort, 60000);
        String recoverLockKey = RECOVER_MSG_LOCK_PREFIX + queueName;
        try {
            lock.lock(recoverLockKey);
            recoverExeMsgs();
        } finally {
            lock.unlock(recoverLockKey);
        }
    }

    /**
     * 正在执行状态，但未ack的消息，将重新放入队列头部
     */
    private void recoverExeMsgs() {
        Jedis jedis = jedisPool.getResource();
        try {
            Set<String> exeKeys = jedis.smembers(exeSetKey);
            if (exeKeys != null && exeKeys.size() > 0) {
                for (String exeKey : exeKeys) {
                    String exeRet = jedis.get(exeKey);
                    if (exeRet != null) {
                        jedis.lpush(this.queueName, exeRet);
                    }
                    removeExeMsg(exeKey);
                }
            }
        } finally {
            jedis.close();
        }
    }

    private void removeExeMsg(String tmpKey) {
        Jedis jedis = jedisPool.getResource();
        try {
            jedis.del(tmpKey);
            jedis.srem(exeSetKey, tmpKey);
        } finally {
            jedis.close();
        }
    }

    public String getQueueName() {
        return this.queueName;
    }

    public boolean push(String message) {
        Jedis jedis = jedisPool.getResource();
        try {
            DelayMessage delayMessage = new DelayMessage(delay, null, message);
            Long result = jedis.rpush(queueName, gson.toJson(delayMessage));
            return result != null;
        } finally {
            jedis.close();
        }
    }

    public DelayMessage pop() {
        while (true) {
            Long waitTime;
            Jedis jedis = jedisPool.getResource();
            try {
                // 取队列头部的消息
                String result = jedis.lpop(queueName);
                // 队列非空
                if (result != null) {
                    //暂存此条消息，放入执行集合
                    String tmpKey = null;
                    if (!autoAck) {
                        tmpKey = this.setExeMsg(result);
                    }
                    DelayMessage delayMessage = gson.fromJson(result, DelayMessage.class);
                    if (delayMessage != null) {
                        if (delayMessage.getExpire() > System.nanoTime()) {
                            // 消息未到可执行状态,休眠等待
                            waitTime = delayMessage.getExpire() - System.nanoTime();
                            try {
                                Thread.sleep(waitTime);
                            } catch (InterruptedException e) {
                                // do nothing
                            }
                        }
                        delayMessage.setTmpKey(tmpKey);
                        return delayMessage;
                    }
                }
            } finally {
                jedis.close();
            }
        }
    }

    /**
     * 将正在执行的消息暂存
     * 返回暂存的key
     *
     * @param msg
     * @return
     */
    private String setExeMsg(String msg) {
        int random = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        String tmpKey = RANDOM_EXECUTION_PREFIX + random;
        Jedis jedis = jedisPool.getResource();
        try {
            jedis.sadd(exeSetKey, tmpKey);
            jedis.set(tmpKey, msg);
            return tmpKey;
        } finally {
            jedis.close();
        }
    }

    public void ack(String tmpKey) {
        if (autoAck) {
            return;
        }
        removeExeMsg(tmpKey);
    }

    public long length() {
        Jedis jedis = jedisPool.getResource();
        try {
            Long result = jedis.llen(queueName);
            if (result != null) {
                return result;
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
            Long result = jedis.del(queueName);
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
