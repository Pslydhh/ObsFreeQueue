# ObsFreeQueue
An simple high performance Obstruction-Free MPMC unbounded Queue Based on FAA(FetchAndAdd) and Linked Arrays.

and this queue is Obviously faster than ConcurrentLinkedQueue and LinkedTransferQueue in JUC.
# benchmarks
Benchmarks for ObsFreeQueue/ConcurrentLinkedQueue/LinkedTransferQueue, run in 4 threads:
- test/benchmark1.java for one enqueue/one dequeue, and until 10,000,000 pairs;
- test/benchmark2.java for one enqueue/one dequeue(in different cycles), and until 5,000,000 pairs;
- test/benchmark3.java for one enqueue until 10,000,000 times and then one dequeue until 10,000,000 times;
# command
-XX:-RestrictContended -Xmx1024m -Xmn256m -XX:+UseG1GC -XX:+TieredCompilation
# test_
<pre><code>
package main.java.com.psly.test;

import main.java.com.psly.concurrent.ObsFreeQueue;
import main.java.com.psly.concurrent.ObsFreeQueue.Handle;

public class benchmark3 {
	private static final int rounds = 16;
	public static void main(String[] args) throws InterruptedException {
			testObsFreeQueue();
	}
	
	private static void testObsFreeQueue() throws InterruptedException {
		final ObsFreeQueue<Integer> queues = new ObsFreeQueue<Integer>();
		System.out.println("\ntestObsFreeQueue");
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
							queues.enqueue(i_ * counts + j, h);
						}
						queues.unregister(h);
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i)
				threads[i].join();
			
			long now = System.currentTimeMillis();
			System.out.println(MetaDATAs.counts + " puts times(seconds): " + (now - start) / 1000.0);
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
			
			System.out.println(MetaDATAs.counts + " pops times(seconds): " + (System.currentTimeMillis() - now) / 1000.0);
			Thread.sleep(2000);
		}
	}
}
</pre></code>
# output

testObsFreeQueue

0 20000000 Ops Hello world! main

10000000 puts times(seconds): 0.786

ints[0-9999999] has been Verify through

10000000 pops times(seconds): 0.279
