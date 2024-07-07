package org.example;

import java.util.concurrent.locks.ReentrantLock;

public class UnboundedQueue<T> {
    ReentrantLock enqLock, deqLock;
    volatile Node<T> head, tail;

    public UnboundedQueue() {
        head = new Node<>(null);
        tail = head;
        enqLock = new ReentrantLock();
        deqLock = new ReentrantLock();
    }

    public void enq(T item) {
        Node<T> e = new Node<>(item);
        enqLock.lock();
        try {
            tail.next = e;
            tail = e;
        } finally {
            enqLock.unlock();
        }
    }

    public T deq() {
        T result;
        deqLock.lock();
        try {
            if (head.next==null) {
                return null;
            }
            result = head.next.value;
            head = head.next;
        } finally {
            deqLock.unlock();
        }
        return result;
    }

    private static class Node<T> {
        T value;
        volatile Node<T> next;

        public Node(T value) {
            this.value = value;
            this.next = null;
        }
    }
}
