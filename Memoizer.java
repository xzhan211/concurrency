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
        String key = "tom";

        ExecutorService executor = Executors.newFixedThreadPool(3);

        Runnable task = () -> {
            try {
                Integer result = cache.getWithFuture(key, () -> {
                    System.out.println("loader running by " + Thread.currentThread().getName());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return key.length() + 100000;
                });

                System.out.println(result + " ----- " + Thread.currentThread().getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        executor.submit(task);
        executor.submit(task);
        executor.submit(task);

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);  
    }
}
