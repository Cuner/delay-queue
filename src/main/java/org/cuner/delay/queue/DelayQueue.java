package org.cuner.delay.queue;

/**
 * Created by houan on 18/4/25.
 */
public interface DelayQueue {

    String getQueueName();

    /**
     * 将消息放入队列尾部
     *
     * @param message
     * @return
     */
    boolean push(String message) throws Exception;

    /**
     * 从队列头部取出数据（阻塞式）
     *
     * @return
     */
    DelayMessage pop() throws Exception;

    /**
     * 消息执行处理完毕，进行ack
     * 只有autoAck=false才需要调用
     *
     * @param tmpKey
     */
    void ack(String tmpKey);

    /**
     * 获取队列长度
     * @return
     */
    long length() throws Exception;

    /**
     * 清空队列
     * @return
     */
    boolean clean() throws Exception;

    /**
     * 获取队列延迟时间(ms)
     * @return
     */
    long getDelay();

    /**
     * 重新设置延迟时间
     */
    void setDelay(long delay);
}
