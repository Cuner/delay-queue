package org.cuner.delay.queue.test;

import org.cuner.delay.queue.local.JDKDelayMessage;
import org.cuner.delay.queue.local.JDKDelayQueue;
import org.junit.Test;

/**
 * Created by houan on 18/4/25.
 */
public class JDKDelayQueueTest {

    @Test
    public void JDKDelayQueueTest() throws Exception {
        JDKDelayQueue jdkDelayQueue = new JDKDelayQueue(10);
        jdkDelayQueue.push("first message");
        System.out.println("first push time: " + System.currentTimeMillis() / 1000);
        jdkDelayQueue.push("second message");
        System.out.println("second push time: " + System.currentTimeMillis() / 1000);

        JDKDelayMessage jdkDelayMessage = (JDKDelayMessage)jdkDelayQueue.pop();
        System.out.println("first pop time: " + System.currentTimeMillis() / 1000 + " message :" + jdkDelayMessage.getMessage());
        jdkDelayMessage = (JDKDelayMessage)jdkDelayQueue.pop();
        System.out.println("second pop time: " + System.currentTimeMillis() / 1000 + " message :" + jdkDelayMessage.getMessage());
    }
}