package main.java.com.psly.concurrent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedTransferQueue;

import main.java.com.psly.concurrent.ObsFreeQueue.Handle;

public class benchmark1 {
	private static final int rounds = 8;
	public static void main(String[] args) throws InterruptedException {
		if(args.length == 0 || args[0].trim().equals("ObsFreeQueue"))
			testObsFreeQueue();
		else if(args[0].trim().equals("ConcurrentLinkedQueue")) {
			testConcurrentLinkedQueue();
		}
		else if(args[0].trim().equals("LinkedTransferQueue")) {
			testLinkedTransferQueue();
		}
	}
	
	private static void testObsFreeQueue() throws InterruptedException {
		final ObsFreeQueue<Integer> queues = new ObsFreeQueue<Integer>();
		System.out.println("testObsFreeQueue");
		for(int t = 0; t < rounds; ++t) {
			int thread_num = MetaDATAs.thread_num;
			final int counts = MetaDATAs.counts / thread_num;
			System.out.println("\n" + t + " " + (MetaDATAs.counts * 2) + " Ops Hello world! " + Thread.currentThread().getName());
			Thread.sleep(2000);
			long start = System.currentTimeMillis();
			Thread[] threads = new Thread[thread_num];
			final int[] ints = new int[MetaDATAs.counts];
			for(int i = 0; i < thread_num; ++i) {
				final int i_ = i;
				(threads[i] = new Thread() {
					public void run() {
						Handle<Integer> h = queues.register();
						for(int j = 0; j < counts; ++j) {
							queues.enqueue(new Integer(i_ * counts + j), h);
							Integer value;
							if((value = queues.dequeue(h)) == null)
								return ;
							ints[value] = 1;
						}
						queues.unregister(h);
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i)
				threads[i].join();
			
			boolean verify = true;
			for(int i = 0; i < MetaDATAs.counts; ++i) {
				if(ints[i] != 1) {
					System.out.println("Error: ints[" + i + "]");
					verify = false;
				}
			}
			if(verify)
				System.out.println("ints[0-" + (MetaDATAs.counts-1) + "] has been Verify through");
			
			System.out.println("cost times(seconds): " + (System.currentTimeMillis() - start) / 1000.0);
			Thread.sleep(2000);
		}
	}
	
	private static void testConcurrentLinkedQueue() throws InterruptedException {
		final Queue<Integer> queues = new ConcurrentLinkedQueue<Integer>();
		System.out.println("testConcurrentLinkedQueue");
		for(int t = 0; t < rounds; ++t) {
			int thread_num = MetaDATAs.thread_num;
			final int counts = MetaDATAs.counts / thread_num;
			System.out.println("\n" + t + " " + (MetaDATAs.counts * 2) + " Ops Hello world! " + Thread.currentThread().getName());
			Thread.sleep(2000);
			long start = System.currentTimeMillis();
			Thread[] threads = new Thread[thread_num];
			final int[] ints = new int[MetaDATAs.counts];
			for(int i = 0; i < thread_num; ++i) {
				final int i_ = i;
				(threads[i] = new Thread() {
					public void run() {
						for(int j = 0; j < counts; ++j) {
							queues.add(new Integer(i_ * counts + j));
							Integer value;
							if((value = queues.poll()) == null)
								return ;
							ints[value] = 1;
						}
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i)
				threads[i].join();
			
			boolean verify = true;
			for(int i = 0; i < MetaDATAs.counts; ++i) {
				if(ints[i] != 1) {
					System.out.println("Error: ints[" + i + "]");
					verify = false;
				}
			}
			if(verify)
				System.out.println("ints[0-" + (MetaDATAs.counts-1) + "] has been Verify through");
			
			System.out.println("cost times(seconds): " + (System.currentTimeMillis() - start) / 1000.0);
			Thread.sleep(2000);
		}
	}
	
	private static void testLinkedTransferQueue() throws InterruptedException {
		final Queue<Integer> queues = new LinkedTransferQueue<Integer>();
		System.out.println("testLinkedTransferQueue");
		for(int t = 0; t < rounds; ++t) {
			int thread_num = MetaDATAs.thread_num;
			final int counts = MetaDATAs.counts / thread_num;
			System.out.println("\n" + t + " " + (MetaDATAs.counts * 2) + " Ops Hello world! " + Thread.currentThread().getName());
			Thread.sleep(2000);
			long start = System.currentTimeMillis();
			Thread[] threads = new Thread[thread_num];
			final int[] ints = new int[MetaDATAs.counts];
			for(int i = 0; i < thread_num; ++i) {
				final int i_ = i;
				(threads[i] = new Thread() {
					public void run() {
						for(int j = 0; j < counts; ++j) {
							queues.add(new Integer(i_ * counts + j));
							Integer value;
							if((value = queues.poll()) == null)
								return ;
							ints[value] = 1;
						}
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i)
				threads[i].join();
			
			boolean verify = true;
			for(int i = 0; i < MetaDATAs.counts; ++i) {
				if(ints[i] != 1) {
					System.out.println("Error: ints[" + i + "]");
					verify = false;
				}
			}
			if(verify)
				System.out.println("ints[0-" + (MetaDATAs.counts-1) + "] has been Verify through");
			
			System.out.println("cost times(seconds): " + (System.currentTimeMillis() - start) / 1000.0);
			Thread.sleep(2000);
		}
	}
}
