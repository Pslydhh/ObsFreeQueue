package main.java.com.psly.concurrent;

import main.java.com.psly.concurrent.FAAQueueBlocking.Handle;
import main.java.com.psly.test.MetaDATAs;

public class TestFAAQueueForBlocking {	
	public static void main(String[] args) throws InterruptedException {
		final FAAQueueBlocking<Integer> queues = new FAAQueueBlocking<Integer>();
	
		for(int t = 0; t < 256; ++t) {
			int thread_num = MetaDATAs.thread_num;
			final int counts = MetaDATAs.counts / thread_num;
			System.out.println("\n" + t + " " + (MetaDATAs.counts * 2) + " Ops Hello world! " + Thread.currentThread().getName());
			Thread.sleep(2000);
			
			long start = System.currentTimeMillis();
			Thread[] threads = new Thread[thread_num];
			final int[] ints = new int[MetaDATAs.counts];
			for(int i = 0; i < thread_num; ++i) {
				(threads[i] = new Thread() {
					public void run() {
						Handle<Integer> h = queues.register();
						for(int i = 0; i < counts; ++i) {
							int val = queues.dequeue(h);
							ints[val] = 1;
						}
						queues.unregister(h);
					}
				}).start();
			}
			
			Thread[] threadss = new Thread[thread_num];
			for(int i = 0; i < thread_num; ++i) {
				final int i_ = i;
				(threadss[i] = new Thread() {
					public void run() {
						Handle<Integer> h = queues.register();
						for(int j = 0; j < counts; ++j) {
							queues.enqueue(i_ * counts + j, h);
						}
						queues.unregister(h);
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i) {
				threads[i].join();
				threadss[i].join();
			}
			
			boolean verify = true;
			for(int i = 0; i < MetaDATAs.counts; ++i) {
				if(ints[i] != 1) {
					System.out.println("Error: ints[" + i + "]");
					verify = false;
				}
			}
			if(verify)
				System.out.println("ints[0-" + (MetaDATAs.counts-1) + "] has been Verify through");
			
			System.out.println(MetaDATAs.counts + " pops times(seconds): " + (System.currentTimeMillis() - start) / 1000.0);
			Thread.sleep(2000);
		}
	}
}
