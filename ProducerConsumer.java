import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class ProducerConsumer {
    private final BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);

    public void produce(int value) throws InterruptedException {
        queue.put(value);
    }

    public int consume() throws InterruptedException {
        return queue.take();
    }

    public static void main(String[] args) throws InterruptedException {
        ProducerConsumer pc = new ProducerConsumer();
        Thread producer = new Thread(()->{
            for(int i=0; i<20; i++) {
                try{
                    pc.produce(i);
                    System.out.println("[P] - " + i + "   - " + System.currentTimeMillis());
                    Thread.sleep(200);

                }catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "producer");
        Thread consumer = new Thread(()->{
            for(int i=0; i<20; i++) {
                try{
                    pc.consume();
                    System.out.println("[C] ---- " + i + "   - " + System.currentTimeMillis());
                    Thread.sleep(1000);

                }catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "consumer");

        consumer.start();
        producer.start();
        consumer.join();
        producer.join();

        System.out.println("DONE");
    }
}