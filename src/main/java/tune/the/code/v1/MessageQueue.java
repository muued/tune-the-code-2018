package tune.the.code.v1;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

public class MessageQueue<T> {

    private static final Map<Class<?>, MessageQueue<?>> printSystemMessageQueue = new HashMap<>();

    public static <S> MessageQueue<S> getInstance(Class<S> clazz) {
        MessageQueue<S> queue = (MessageQueue<S>) printSystemMessageQueue.get(clazz);
        if (queue == null) {
            queue = new MessageQueue<>();
            printSystemMessageQueue.put(clazz, queue);
        }
        return queue;
    }

    private T[] buffer = null;
    private int size = 0;

    @SuppressWarnings("unchecked")
    public synchronized T pop() {
        if (size == 0) {
            return null;
        }
        final T obj = buffer[0];

        redim(--size, (Class<T>) obj.getClass());
        return obj;
    }

    @SuppressWarnings("unchecked")
    public synchronized void push(final T obj) {
        assert (obj != null);

        final int bufferLength = buffer == null ? 0 : buffer.length;
        if (size + 1 > bufferLength) {
            redim(size + 1, (Class<T>) obj.getClass());
        }
        buffer[size++] = obj;
    }

    /**
     * Re-Dimension of buffer-array to the given size and of given type. Data of
     * current buffer is being preserved. <br/>
     * In case of increased buffer-size the data is left-aligned.<br/>
     * In case of decreased buffer-size the data is right-aligned.
     *
     * <pre>
     * buffer	-> method-call			-> buffer
     * ["A"]	 -> redim(2, String.class) -> ["A", null]
     * ["A","B"] -> redim(1, String.class) -> ["B"]
     * </pre>
     *
     * @param newSize
     * @param genericClass
     */
    @SuppressWarnings("unchecked")
    private void redim(final int newSize, final Class<T> genericClass) {
        // create new buffer
        final T[] newBuffer = (T[]) Array.newInstance(genericClass, newSize);
        // copy data of old to new buffer if needed
        if (buffer != null && newSize > 0) {
            final int copyLength = Math.min(newSize, buffer.length);
            final int copyStartIndex = buffer.length - copyLength;
            System.arraycopy(buffer, copyStartIndex, newBuffer, 0, copyLength);
        }
        buffer = newBuffer;
    }
}
