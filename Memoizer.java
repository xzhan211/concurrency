import java.util.function.Function;
import java.util.concurrent.*;
import java.util.*;

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
                return future.get(); // 多线程之间同步
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
        CountDownLatch startLatch = new CountDownLatch(1); 

        Callable<Void> task = () -> {
            startLatch.await();
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
            return null;
        };

        List<Future<Void>> futures = new ArrayList<>();
        futures.add(executor.submit(task));
        futures.add(executor.submit(task));
        futures.add(executor.submit(task));
        
        startLatch.countDown();
    
        for(Future<Void> future : futures) {
            future.get(); // 主线程等待 + 异常传播
        }
        executor.shutdown();
    }
}


/*

➜  concurrency git:(main) ✗ java Memoizer                            
loader running by pool-1-thread-3
100003 ----- pool-1-thread-2
100003 ----- pool-1-thread-3
100003 ----- pool-1-thread-1
➜  concurrency git:(main) ✗ java Memoizer
loader running by pool-1-thread-3
100003 ----- pool-1-thread-1
100003 ----- pool-1-thread-3
100003 ----- pool-1-thread-2


*/
