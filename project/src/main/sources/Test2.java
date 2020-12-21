import java.lang.Thread;
import java.util.concurrent.locks.ReentrantLock;

class Test2 {
    public static void main(String[] args) {
        int testCnt = 200;
        for (int i = 0; i < testCnt; i++) {
            ReentrantLock lock1 = new ReentrantLock();
            ReentrantLock lock2 = new ReentrantLock();
            TestThread t1 = new TestThread(lock1, lock2);
            TestThread t2 = new TestThread(lock2, lock1);
            t1.start();
            t2.start();
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class TestThread extends Thread {
    ReentrantLock lock1;
    ReentrantLock lock2;

    TestThread(ReentrantLock lock1, ReentrantLock lock2) {
        this.lock1 = lock1;
        this.lock2 = lock2;
    }

    public void run() {
        this.lock1.lock();
        this.lock2.lock();
        
        this.lock2.unlock();
        this.lock1.unlock();
    }
}