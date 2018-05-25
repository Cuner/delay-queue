# delay-queue
local delayQueue implemented by JDK &amp; two kinds of distributed delayQueue based redis

## 1. 基本介绍

### RedisSynDelayQueue

1. 基于redis，**并发情况下会加分布式锁**，单线程场景（syn=false）性能较好， 并发场景性能较差
2. **若在并发场景下，设置syn=false，会导致消息重复消费、消息丢失的情况**
3. 支持delay时间的动态调整

### RedisConcurrentDelayQueue⭐️
1. 基于redis，支持在**无分布式锁**的情况下进行并发消费
2. autoAck为true时，吞吐量性能极好，autoAck为false，吞吐量会稍有下降
3. 支持delay时间的动态调整
4. **autoAck为false时，必须在处理完消息后手动调用ack方法，否则会导致应用重启后重新开始消费**

### JDKDelayQueue
1. 基于java.util.syn.DelayQueue，纯内存的消息队列，优点是性能较好，缺陷是没有持久化，应用重启等过程中会有大量消息丢失。

## 2. 性能参考
* RedisConcurrentDelayQueue和RedisSynDelayQueue的简单对比，数据是线下单机环境测试数据

| 队列种类                     |  消费线程数| syn | autoAck |  耗时    |  消息丢失  | 重复消费   |
|:---------------------------:|:----------:|:----------:|:-------:|:--------:|:----------:|:----------:|
| RedisConcurrentDelayQueue         |  1         | -          |  false  | 53936ms  |   无       |     无     |
| RedisConcurrentDelayQueue         |  1         | -          |  true   | 13130ms  | 消费进程关闭，正在处理的消息会丢失 |   无   |
| RedisSynDelayQueue             |  1         | true       |   -     | 55420ms  |   无       |   无   |
| RedisSynDelayQueue             |  1         | false      |   -     | 20012ms  |   无       |   无   |
| RedisConcurrentDelayQueue         |  10        | -          |  false  | 7279ms  |   无       |     无     |
| RedisConcurrentDelayQueue         |  10        | -          |  true   | 1181ms  | 消费进程关闭，正在处理的消息会丢失 |   无   |
| RedisSynDelayQueue             |  10        | true       |   -     | 61532ms  |   无       |   无   |
| RedisSynDelayQueue             |  10        | false      |   -     | -  |   大量消息丢失       |   大量重复消费   |

1. 若能接受系统重启、关闭时的少量消息丢失，推荐RedisConcurrentDelayQueue，并设置autoAck为true：性能最好，且消费线程越多，消费速度（吞吐量）也会相对越好
2. 若不能接受消息丢失，在单机、单线程消费的场景下，可以选择RedisConcurrentDelayQueue（autoAck设置为false）RedisSynDelayQueue（syn设置为false）；
3. 若不能接受消息丢失，且需要在多线程、分布式场景下消费，推荐推荐RedisConcurrentDelayQueue（autoAck设置为false），消费线程越多，消费速度（吞吐量）也会相对越好；
4. RedisSynDelayQueue在并发消费的场景下性能较差，不推荐使用。

## 3. 用法
```java
  // RedisSynDelayQueue
  IDelayQueue queue = DelayQueueFactory.getRedisSynDelayQueue("testdisqueue", 1000, false, "localhost", 6379);// 五个参数分别为队列名、延迟时间ms、是否多线程或分布式消费、redis host、redis port
  queue.put("消息A");
  DelayMessage message = queue.pop();//从队列头部取出就绪的消息，阻塞
  System.out.println(message.getMessage());

  // RedisConcurrentDelayQueue
  IDelayQueue queue = DelayQueueFactory.getRedisConcurrentDelayQueue("testdisqueue", 1000, false, "localhost", 6379);// 三个参数分别为队列名、延迟时间ms、是否自动ack、redis host、redis port
  queue.put("消息A");
  DelayMessage message = queue.pop();//从队列头部取出就绪的消息，阻塞
  System.out.println(message.getMessage());
  queue.ack(message.getTmpKey());//autoAck设置为false的时候，必须主动调用ack方法，参数为消息的tmpKey
```
