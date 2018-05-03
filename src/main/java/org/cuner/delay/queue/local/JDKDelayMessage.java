package org.cuner.delay.queue.local;

import org.cuner.delay.queue.DelayMessage;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Created by houan on 18/4/25.
 */
public class JDKDelayMessage extends DelayMessage implements Delayed {

    //纳秒级别
    public JDKDelayMessage(long delay, String tmpKey, String message) {
        super(delay, tmpKey, message);
    }

    public long getDelay(TimeUnit unit) {
        return unit.convert(this.getExpire() - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    public int compareTo(Delayed o) {
        return (int) (this.getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS));
    }

}
