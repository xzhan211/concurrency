import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class RateLimiter {
    private final BlockingQueue<Long> queue = new LinkedBlockingQueue<>();
    private final int limit;
    private final long window;

    public RateLimiter(int limit, long window) {
        this.limit = limit;
        this.window = window;
    }

    public synchronized boolean allow() {
        long now = System.currentTimeMillis();
        while (!queue.isEmpty() && now - queue.peek() > window) {
            queue.poll();
        }
        if (queue.size() < limit) {
            queue.offer(now);
            return true;
        }
        return false;
    }
}