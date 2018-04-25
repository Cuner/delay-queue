package org.cuner.delay.queue.local;

import org.cuner.delay.queue.DelayMessage;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Created by houan on 18/4/25.
 */
public class JDKDelayMessage extends DelayMessage implements Delayed {

    private final long delay; //延迟时间
    private final long expire;  //到期时间
    private final long now; //创建时间

    //纳秒级别
    public JDKDelayMessage(long delay, String tmpKey, String message) {
        super(tmpKey, message);
        this.delay = delay;
        this.now = System.nanoTime();
        this.expire = this.delay + this.now;
    }

    public long getDelay(TimeUnit unit) {
        return unit.convert(this.expire - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    public int compareTo(Delayed o) {
        return (int) (this.getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS));
    }

    @Override
    public String toString() {
        return "JDKDelayMessage{" +
                "delay=" + delay +
                ", expire=" + expire +
                ", now=" + now +
                '}';
    }
}
