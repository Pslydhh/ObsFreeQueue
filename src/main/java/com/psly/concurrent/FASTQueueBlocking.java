package main.java.com.psly.concurrent;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

//import sun.misc.Contended;
import sun.misc.Unsafe;

public class FASTQueueBlocking<T> {
    public FASTQueueBlocking() {
        Node<T> node = new Node<T>(0);
        this.putNode = new Handle<T>(node);
        this.popNode = new Handle<T>(node);
    }
    
    private final Handle<T> putNode;
    
    private final Handle<T> popNode;
    
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
    
    private void linkNextNode(Handle<T> handle, Node<T> node, long index) {
        if(node.next == null) {
            Node<T> newNode = new Node<T>(index);
            node.casNext(null, newNode);
        }
        if(handle.node == node)
            handle.casNode(node, node.next);
    }
    
    public void enqueue(T item) {
        for(;;) {
            Node<T> node = putNode.node;
            _unsafe.loadFence();
            long index = putNode.index.get();
            long offset = index - node.startIndex;
            
            if(offset < Node.CELLS_SIZE) {
                if(putNode.index.compareAndSet(index, index + 1)) {
                    if(offset + 1 == Node.CELLS_SIZE) {
                        linkNextNode(putNode, node, index + 1);
                    }
                    Object parkedThread;
                    if((parkedThread = node.xchgCells(index & Node.CELLS_BIT, item)) !=null)
                        _unsafe.unpark((Thread) parkedThread);
                    return;
                }
                Thread.yield();
            } else { // must be offset == Node.CELLS_SIZE
                linkNextNode(putNode, node, index);
            }
        }        
    }
    
    public T dequeue() {
        for(;;) {
            Node<T> node = popNode.node;
            _unsafe.loadFence();
            long index = popNode.index.get();
            long offset = index - node.startIndex;
            
            if(offset < Node.CELLS_SIZE) {
                if(popNode.index.compareAndSet(index, index + 1)) {
                    if(offset + 1 == Node.CELLS_SIZE) {
                        linkNextNode(popNode, node, index + 1);
                    }
                    
                    long times = Node.POP_TIMES;
                    Object val = null;
                    do {
                        val = node.getCells(index & Node.CELLS_BIT);
                        if(val != null) { 
                            return (T) val;
                        }
                        --times;
                    } while(times > 0);
                    
                    Thread thread;
                    if((val = node.xchgCells(index & Node.CELLS_BIT, thread = Thread.currentThread())) == null) {
                        while((val = node.getCells(index & Node.CELLS_BIT)) == thread){
                            _unsafe.park(false, 0L);
                        }
                    }
                    return (T) val;                    
                }
                Thread.yield();
            } else { // must be offset == Node.CELLS_SIZE
                linkNextNode(popNode, node, index);
            }
        }
    }
    
    public static class Handle<T>{
        AtomicLong index;
        Node<T> node;

        public Handle(Node<T> node) {
            super();
            this.node = node;
            this.index = new AtomicLong();
        }
        
        public boolean casNode(Node<T> cmp, Node<T> val) {
            return _unsafe.compareAndSwapObject(this, next_node, cmp, val);
        }
        
        private static final long next_node;
        static {
            try {
                next_node = _unsafe.objectFieldOffset(Handle.class.getDeclaredField("node"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }
    
    static class Node<T> {
        private static final int RIght_shift = 10;
        private static final int CELLS_SIZE = 1 << RIght_shift;
        private static final int CELLS_BIT = CELLS_SIZE - 1;
        private static final int POP_TIMES = 1 << 10;
                
        long startIndex;
        
        private final Object[] cells;
        
        private Node<T> next;
        
        public Node(long startIndex) {
            super();
            this.startIndex = startIndex;
            this.cells = new Object[CELLS_SIZE];
            this.next = null;
        }

        private static long rawIndex(final long idx) {
            return cells_entry_base + idx * cells_entry_scale;
        }

        public Object xchgCells(long idx, Object val) {
            return _unsafe.getAndSetObject(cells, rawIndex(idx), val);
            //return _unsafe.compareAndSwapObject(cells, rawIndex(idx), cmp, val);
        }
        
        public Object getCells(long idx) {
            return cells[(int) idx];
        }
        
        public void setCells(long idx, Object val) {
            _unsafe.putObjectVolatile(cells, rawIndex(idx), val);
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
