package tune.the.code.v2;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @author Fabian Ohler <fabian.ohler1@rwth-aachen.de>
 * @since 27.11.2018
 */
public class MessageQueue<T> {

    private static final Map<Class<?>, MessageQueue<?>> queueMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> MessageQueue<T> getInstance(Class<T> clazz) {
        return (MessageQueue<T>) queueMap.computeIfAbsent(clazz, k -> new MessageQueue<T>(new LinkedBlockingQueue<>()));
    }

    private final BlockingQueue<T> queue;

    private MessageQueue(BlockingQueue<T> queue) {
        this.queue = queue;
    }

    public T pop(int waitForMs) throws InterruptedException {
        return queue.poll(waitForMs, TimeUnit.MILLISECONDS);
    }

    public void push(T obj) throws InterruptedException {
        queue.put(obj);
    }
}
