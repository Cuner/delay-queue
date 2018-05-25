package org.cuner.delay.queue;

import org.cuner.delay.queue.cluster.RedisSynDelayQueue;
import org.cuner.delay.queue.cluster.RedisConcurrentDelayQueue;
import org.cuner.delay.queue.local.JDKDelayQueue;

/**
 * Created by houan on 18/5/4.
 */
public class DelayQueueFactory {

    /**
     * 基于redis, 并发情况下会加分布式锁, 单线程场景（concurrent=false）性能较好, 并发场景性能较差
     * 若在并发场景下, 设置concurrent=false, 会导致消息重复消费、消息丢失的情况
     * 支持delay时间的动态调整
     *
     * @param queueName
     * @param delay
     * @param concurrent
     * @param redisHost
     * @param redisPort
     * @return
     * @throws Exception
     */
    public static RedisSynDelayQueue getRedisSynDelayQueue(String queueName, long delay, boolean concurrent, String redisHost, int redisPort) {
        return new RedisSynDelayQueue(queueName, delay, concurrent, redisHost, redisPort);
    }

    /**
     * 基于redis, 支持在无分布式锁的情况下进行并发消费
     * autoAck为true时, 吞吐量性能极好, autoAck为false, 吞吐量会稍有下降
     * 支持delay时间的动态调整
     *
     * @param queueName
     * @param delay
     * @param autoAck
     * @param redisHost
     * @param redisPort
     * @return
     * @throws Exception
     */
    public static RedisConcurrentDelayQueue getRedisConcurrentDelayQueue(String queueName, long delay, boolean autoAck, String redisHost, int redisPort) {
        return new RedisConcurrentDelayQueue(queueName, delay, autoAck, redisHost, redisPort);
    }

    /**
     * 基于JDK DelayQueue
     * delay单位为毫秒
     */
    public static JDKDelayQueue getJDKDelayQueue(long delay) {
        return new JDKDelayQueue(delay);
    }
}
