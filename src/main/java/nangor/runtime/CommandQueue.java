package nangor.runtime;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Predicate;

/** A bounded queue provides backpressure instead of dropping pause or detonate. */
public final class CommandQueue<T> {
    private final ArrayBlockingQueue<T> queue;
    public CommandQueue(int capacity) { queue = new ArrayBlockingQueue<>(capacity); }
    public void put(T value) throws InterruptedException { queue.put(value); }
    public T poll() { return queue.poll(); }
    public void discardUnless(Predicate<T> keep) { queue.removeIf(value -> !keep.test(value)); }
}
