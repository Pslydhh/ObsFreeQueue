# ObsFreeQueue
An igh performance Obstruction-Free mpmc unbounded queues Based on FAA(FetchAndADD) and Linked Arrays
# benchmarks
Benchmarks for ObsFreeQueue/ConcurrentLinkedQueue/LinkedTransferQueue, run in 4 threads:
- test/benchmark1.java for one enqueue/one dequeue, and until 10,000,000 pairs;
- test/benchmark2.java for one enqueue/one dequeue(in different cycles), and until 5,000,000 pairs;
- test/benchmark3.java for one enqueue until 10,000,000 times and then one dequeue until 10,000,000 times;
