package tune.the.code.v2;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @author Fabian Ohler <fabian.ohler1@rwth-aachen.de>
 * @since 27.11.2018
 */
public class MessageQueue<T> {

    private static final Map<Class<?>, MessageQueue<?>> queueMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> MessageQueue<T> getInstance(Class<T> clazz) {
        return (MessageQueue<T>) queueMap.computeIfAbsent(clazz, k -> new MessageQueue<T>(new ConcurrentLinkedQueue<>()));
    }

    private final Queue<T> queue;

    private MessageQueue(Queue<T> queue) {
        this.queue = queue;
    }

    public T pop() {
        return queue.poll();
    }

    public void push(T obj) {
        queue.add(obj);
    }

    public boolean hasElements() {
        return !queue.isEmpty();
    }
}
