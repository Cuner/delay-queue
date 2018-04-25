package org.cuner.delay.queue;

/**
 * Created by houan on 18/4/25.
 */
public class DelayMessage {

    /**
     * 正执行消息的token
     */
    private String tmpKey;

    /**
     * 消息内容
     */
    private String message;

    public DelayMessage(String tmpKey, String message){
        this.tmpKey = tmpKey;
        this.message = message;
    }

    public String getTmpKey() {
        return tmpKey;
    }

    public void setTmpKey(String tmpKey) {
        this.tmpKey = tmpKey;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
