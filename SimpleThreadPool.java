import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class SimpleThreadPool {
    private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(10);
    private final List<Worker> workers = new ArrayList<>();
    public SimpleThreadPool(int nThreads) {
        for(int i=0; i<nThreads; i++) {
            Worker worker = new Worker("work-" + i);
            worker.start();
            workers.add(worker);
        }
    }

    public void submit(Runnable task) throws InterruptedException {
        queue.put(task);
    }

    public void stop() {
        for(Worker w : workers) {
            w.terminate();
        }
    }

    public void join() throws InterruptedException {
        for(Worker w : workers) {
            w.join();
        }
    }

    private final class Worker extends Thread {
        private volatile boolean run;

        Worker(String name) {
            super(name);
            this.run = true;
        }

        public void terminate() {
            run = false;
            // 唤醒可能因 take() 阻塞的 worker
            for (Worker w : workers) {
                w.interrupt();
            }
        }

        @Override
        public void run() {
            while(run || !queue.isEmpty()) {
                try {
                    Runnable task = queue.take();
                    task.run();
                } catch(InterruptedException e) {
                    // Thread.currentThread().interrupt();
                    if(!run && queue.isEmpty()) {
                        break;
                    } 
                    continue;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SimpleThreadPool pool = new SimpleThreadPool(3);
        for(int i=0; i<20; i++) {
            int idx = i;
            pool.submit(()-> {
                System.out.println("[Execute]   " + Thread.currentThread().getName() + " --- " + idx);
                try {
                    Thread.sleep(1000);
                }catch(Exception e) {
                    // skip
                }
            });
            System.out.println("[Submit] " + i);
        }
        
        pool.stop();
        pool.join();
        System.out.println("Done");
    }
}