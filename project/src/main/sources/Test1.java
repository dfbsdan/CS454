import java.lang.Thread;
import java.util.*;

class Test1 {
    public static void main(String[] args) {
        int testCnt = 50;
        for (int i = 0; i < testCnt; i++) {
            int threadCnt = 10;
            List<TestThread> threads = new ArrayList<>(threadCnt);
            List<Integer> list = new ArrayList<>();
            for (int j = 0; j < threadCnt; j++) {
                TestThread thread = new TestThread(list, j);
                threads.add(thread);
            }
            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (list.size() != threadCnt)
                System.exit(1);
        }
    }
}

class TestThread extends Thread {
    List<Integer> list;
    int i;

    TestThread(List<Integer> list, int i) {
        this.list = list;
        this.i = i;
    }

    public void run(){
        this.list.add(i);
    }
}