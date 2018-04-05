package main.java.com.psly.concurrent;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import sun.misc.Unsafe;

/**
 *
 * @since 1.6
 * @author Pslydhh 2018.04.03
 * @param <E> the type of elements held in this collection
 */
public class ObsFreeQueue<T> {
	public ObsFreeQueue() {
		this.putNode = new Node<T>();
		this.popNode = this.putNode;
		this.putIndex = new AtomicLong();
		this.popIndex = new AtomicLong();
	}
	
	private Node<T> putNode;
	private Node<T> popNode;
	private AtomicLong putIndex;
	private AtomicLong popIndex;
	
	public long getPutIndex() {
		return putIndex.get();
	}
	
	public long getPopIndex() {
		return popIndex.get();
	}
	
	private boolean casPutNode(Node<T> cmp, Node<T> val) {
		return _unsafe.compareAndSwapObject(this, putNode_offset, cmp, val);
	}
	
	private boolean casPopNode(Node<T> cmp, Node<T> val) {
		return _unsafe.compareAndSwapObject(this, popNode_offset, cmp, val);
	}
	
	private static final long putNode_offset;
	private static final long popNode_offset;
	private static final Unsafe _unsafe = UtilUnsafe.getUnsafe();
	
	private static class UtilUnsafe {
		private UtilUnsafe() {
		}

		public static Unsafe getUnsafe() {
			if (UtilUnsafe.class.getClassLoader() == null)
				return Unsafe.getUnsafe();
			try {
				final Field fld = Unsafe.class.getDeclaredField("theUnsafe");
				fld.setAccessible(true);
				return (Unsafe) fld.get(UtilUnsafe.class);
			} catch (Exception e) {
				throw new RuntimeException("Could not obtain access to sun.misc.Unsafe", e);
			}
		}
	}
	
	static {
		try {
			putNode_offset = _unsafe.objectFieldOffset(ObsFreeQueue.class.getDeclaredField("putNode"));
			popNode_offset = _unsafe.objectFieldOffset(ObsFreeQueue.class.getDeclaredField("popNode"));
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
    /**
     * Throws NullPointerException if argument is null.
     *
     * @param v the element
     */
    private static void checkNotNull(Object v) {
        if (v == null)
            throw new NullPointerException();
    }
	
    /**
     * Inserts the specified element to this queue.
     */
	public void enqueue(T item, Handle<T> handle) {
		checkNotNull(item);
		long index;
		Node<T> node;
		for(;;) {
			index = putIndex.getAndIncrement();
			node = findCell(handle.putNode, index, handle);
			if(handle.putNode != node) {
				handle.putNode = node;
				chasePut(node);
			}
			// if the cell is null and the compare-and-swap return true, we successful!!
			if(node.getCells(index & Node.CELLS_BIT) == null
				&& node.casCells(index & Node.CELLS_BIT, null, item)) {
				return ;
			}
		}
	}
	
    /**
     * try to chase the newest putNode. As the unused Memory can be reclaimed
     */
	private void chasePut(Node<T> node) {
		Node<T> putNode = this.putNode;
		while(putNode.id < node.id && !this.casPutNode(putNode, node))
			putNode = this.putNode;
	}
	
    /**
     * get a element from this queue.
     */
	public T dequeue(Handle<T> handle) {
		long index, times = Node.POP_TIMES;
		Node<T> node;
		for(;;) {
			index = popIndex.getAndIncrement();
			node = findCell(handle.popNode, index, handle);
			if(handle.popNode != node) {
				handle.popNode = node;
				chasePop(node);
			}
			
			T val = null;
			do {
				val = node.getCells(index & Node.CELLS_BIT);
				// If the cell is not empty, then we return it as ending
				if(val != null) { 
					return val;
				}
				--times;
			} while(times > 0);
			
			if(node.casCells(index & Node.CELLS_BIT, null, Node.TOMBSTONE)) {
				// if we surpassed putIndex, so we should return null as ending...
				if(index >= this.putIndex.get()) {
					long putPrev;
					// and if our index greater than putIndex, we should raise it
					while(index > (putPrev = this.putIndex.get())) {
						if(this.putIndex.compareAndSet(putPrev, index))
							return null;
					}
					return null;
				}
				times = Node.POP_TIMES;
				continue;
			}
			// here the cell mustn't be empty! then we return it as ending
			val = node.getCells(index & Node.CELLS_BIT);
			return val;
		}
	}
	
	/*
	 * try to chase the newest popNode. As the unused Memory can be reclaimed
	 */
	private void chasePop(Node<T> node) {
		Node<T> popNode = this.popNode;
		while(popNode.id < node.id && !this.casPopNode(popNode, node))
			popNode = this.popNode;
	}
	
	/*
	 * find the node as index:i, that is the node such that:
	 *       node.id == (i >>>Node.RIght_shift)
	 * return node.      
	 */
	public Node<T> findCell(Node<T> node, long i, Handle<T> handle) {
		Node<T> curr = node;
		long j, index = (i >>> Node.RIght_shift);
		for(j = curr.id; j < index; ++j) {
			Node<T> next = curr.next;
			// next is null
			if(next == null) {
				// construct a new Node
				Node<T> temp = handle.spare;
				if(temp == null) {
					temp = new Node<T>();
					handle.spare = temp;
				}
				temp.id = j + 1;
				// link to the prev's next;
				if( curr.casNext(null, temp)) {
					next = temp;
					handle.spare = null;
				} else {
					next = curr.next;
				}
			}
			curr = next;
		}
		return curr;
	}
	
	/*
	 * Threads must first registered for the queue as get the useful Handle.
	 */
	public Handle<T> register() {
		Handle<T> handle = new Handle<T>(this.putNode, this.popNode);
		return handle;
	}
	
	/*
	 * Threads never use the queue again.
	 */
	public void unregister(Handle<T> handle) {
		handle.popNode = handle.putNode = handle.spare = null;
	}
	
	public static class Handle<T>{
		Node<T> putNode;
		Node<T> popNode;
		Node<T> spare;
		public Handle(Node<T> putNode, Node<T> popNode) {
			super();
			this.putNode = putNode;
			this.popNode = popNode;
			this.spare = null;
		}
	}

	static class Node<T> {
		private static final int RIght_shift = 10;
		private static final int CELLS_SIZE = 1 << RIght_shift;
		private static final int CELLS_BIT = CELLS_SIZE - 1;
		private static final int POP_TIMES = 256;
		private static final Object TOMBSTONE = new Object();
		
		private long id;
		private final Object[] cells;
		private Node<T> next;
		
		public Node() {
			super();
			this.cells = new Object[CELLS_SIZE];
			this.next = null;
		}

		private static long rawIndex(final long idx) {
			return cells_entry_base + idx * cells_entry_scale;
		}
		  
		public boolean casCells(long idx, Object cmp, Object val) {
			return _unsafe.compareAndSwapObject(cells, rawIndex(idx), cmp, val);
		}
		
		public T getCells(long idx) {
			return (T) cells[(int) idx];
		}
		
		public boolean casNext(Node<T> cmp, Node<T> val) {
			return _unsafe.compareAndSwapObject(this, next_offset, cmp, val);
		}
		
		private static final long cells_entry_base;
		private static final long cells_entry_scale;
		private static final long next_offset;
		static {
			try {
				cells_entry_base = _unsafe.arrayBaseOffset(Object[].class);
				cells_entry_scale = _unsafe.arrayIndexScale(Object[].class);
				next_offset = _unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
			} catch (Exception e) {
				throw new Error(e);
			}
		}
	}
}
	
