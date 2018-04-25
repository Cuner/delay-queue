package org.cuner.delay.queue.local;

import org.cuner.delay.queue.DelayMessage;

import java.util.concurrent.DelayQueue;

/**
 * Created by houan on 18/4/25.
 */
public class JDKDelayQueue implements org.cuner.delay.queue.DelayQueue {

    //纳秒
    private long delay;

    private DelayQueue<JDKDelayMessage> delayQueue = new DelayQueue<JDKDelayMessage>();

    public JDKDelayQueue(long delay) {
        this.delay = delay * 1000000000;
    }

    public String getQueueName() {
        return "jdkDelayQueue";
    }

    public boolean push(String message) throws Exception {
        JDKDelayMessage jdkDelayMessage = new JDKDelayMessage(delay, null, message);
        return delayQueue.offer(jdkDelayMessage);
    }

    public DelayMessage pop() throws Exception {
        return delayQueue.take();
    }

    public void ack(String tmpKey) {

    }

    public long length() throws Exception {
        return delayQueue.size();
    }

    public boolean clean() throws Exception {
        delayQueue.clear();
        return true;
    }

    public long getDelay() {
        return this.delay / 1000000000;
    }

    public void setDelay(long delay) {
        this.delay = delay * 1000000000;
    }
}
