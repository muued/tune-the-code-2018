package tune.the.code.v2;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @author Fabian Ohler <fabian.ohler1@rwth-aachen.de>
 * @since 27.11.2018
 */
public class MessageQueue<T> {

    private static final Map<Class<?>, MessageQueue<?>> queue = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> MessageQueue<T> getInstance(Class<T> clazz) {
        return (MessageQueue<T>) queue.computeIfAbsent(clazz, k -> new MessageQueue<T>(new ConcurrentLinkedDeque<>()));
    }

    private final Deque<T> deque;

    private MessageQueue(Deque<T> deque) {
        this.deque = deque;
    }

    /**
     * The method name is chosen this way to be identical with the original API. Even though {@link Deque#pop()} exists,
     * it throws Exception if the Deque is empty, which is good but does not align with the original API. It should
     * return null. Therefore, we use {@link Deque#poll()}.
     */
    public T pop() {
        return deque.poll();
    }

    public void push(T obj) {
        deque.push(obj);
    }

    public boolean hasElements() {
        return !deque.isEmpty();
    }
}
