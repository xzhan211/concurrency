import java.util.function.Function;
import java.util.concurrent.*;

// Cache stores data, Memoizer stores computation. Memoizer avoids cache stampede.
public class Memoizer<K, V> {
    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, Future<V>> cacheWithFuture = new ConcurrentHashMap<>();
    public V get(K key, Function<K, V> loader) {
        return cache.computeIfAbsent(key, loader);
    }

    public V getWithFuture(K key, Callable<V> loader) throws Exception{
        while(true) {
            Future<V> future = cacheWithFuture.get(key);
            if(future == null) {
                FutureTask<V> ft = new FutureTask<>(loader);
                future = cacheWithFuture.putIfAbsent(key, ft);

                if(future == null) {
                    future = ft;
                    ft.run(); // only one thread runs
                }
            }

            try {
                return future.get();
            } catch (CancellationException e) {
                cacheWithFuture.remove(key, future);
            } catch (ExecutionException e) {
                cache.remove(key, future);
                throw new RuntimeException(e.getCause());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Memoizer<String, Integer> cache = new Memoizer<>();
        System.out.println(cache.get("123", k -> {return k.length() + 100;}));
        String key = "456";
        System.out.println(cache.getWithFuture(key, () -> {
            System.out.println("----" + key);
            return 123;}));
    }
}
