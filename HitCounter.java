
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ConcurrentHashMap;

public class HitCounter {
    ConcurrentHashMap<String, LongAdder> map = new ConcurrentHashMap<>(); 
    public void hit(String key) {
        map.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    public long get(String key) {
        LongAdder adder = map.get(key);
        return adder == null ? 0 : adder.sum();
    }

    public static void main(String[] args) {
        HitCounter counter = new HitCounter();
        counter.hit("1");
        counter.hit("1");
        counter.hit("1");
        System.out.println(counter.get("1"));
    }
}