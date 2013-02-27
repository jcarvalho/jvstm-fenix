/*
 * JVSTM: a Java library for Software Transactional Memory
 * Copyright (C) 2005 INESC-ID Software Engineering Group
 * http://www.esw.inesc-id.pt
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Author's contact:
 * INESC-ID Software Engineering Group
 * Rua Alves Redol 9
 * 1000 - 029 Lisboa
 * Portugal
 */
package jvstm.util;

import jvstm.VBox;
import jvstm.VBoxInt;
import pt.ist.esw.atomicannotation.Atomic;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Queue;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class VQueue<E> extends AbstractCollection<E> implements Queue<E> {
    private final VBox<Cons<E>> front = new VBox<Cons<E>>((Cons<E>)Cons.empty());
    private final VBox<Cons<E>> rear = new VBox<Cons<E>>((Cons<E>)Cons.empty());
    private final VBoxInt size = new VBoxInt(0);

    public VQueue() {
    }

    public VQueue(Collection<? extends E> c) {
        addAll(c);
    }

    public int size() {
        return size.getInt();
    }

    public boolean add(E o) {
        return offer(o);
    }

    // the Queue interface methods

    @Atomic(canFail = false)
    public boolean offer(E o) {
        Cons<E> frontElems = front.get();
        if (frontElems.isEmpty()) {
            front.put(frontElems.cons(o));
        } else {
            rear.put(rear.get().cons(o));
        }

        size.inc();

        return true;
    }

    @Atomic(canFail = false)
    public E poll() {
        Cons<E> frontElems = front.get();
        if (frontElems.isEmpty()) {
            return null;
        } else {
            return removeExisting(frontElems);
        }
    }

    @Atomic
    public E remove() {
        Cons<E> frontElems = front.get();
        if (frontElems.isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return removeExisting(frontElems);
        }
    }

    private E removeExisting(Cons<E> frontElems) {
        E result = frontElems.first();
        frontElems = frontElems.rest();
        if (frontElems.isEmpty()) {
            frontElems = rear.get().reverse();
            if (! frontElems.isEmpty()) {
                // only clear the rear if it had anything
                rear.put((Cons<E>)Cons.empty());
            }
        }
        front.put(frontElems);
        size.dec();
        return result;
    }

    @Atomic(readOnly = true)
    public E peek() {
        Cons<E> frontElems = front.get();
        if (frontElems.isEmpty()) {
            return null;
        } else {
            return frontElems.first();
        }
    }

    @Atomic(readOnly = true)
    public E element() {
        Cons<E> frontElems = front.get();
        if (frontElems.isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return frontElems.first();
        }
    }

    // override some methods with better performing implementations

    @Atomic(readOnly = true)
    public boolean contains(Object o) {
        return front.get().contains(o) || rear.get().contains(o);
    }

    public Iterator<E> iterator() {
        return new VQueueIterator<E>();
    }

    @Atomic(canFail = false)
    public void clear() {
        front.put((Cons<E>)Cons.empty());
        rear.put((Cons<E>)Cons.empty());
        size.putInt(0);
    }

    private class VQueueIterator<T> implements Iterator<T> {
        private Iterator<T> current;
        private Cons<T> rearElems;

        VQueueIterator() {
            this.current = (Iterator<T>)VQueue.this.front.get().iterator();
            this.rearElems = (Cons<T>)VQueue.this.rear.get();
        }
        
        public boolean hasNext() { 
            return current.hasNext() || (! rearElems.isEmpty());
        }
        
        public T next() {
            if (! current.hasNext()) {
                if (! rearElems.isEmpty()) {
                    current = rearElems.reverse().iterator();
                    rearElems = (Cons<T>)Cons.empty();
                }
            }

            if (! current.hasNext()) {
                throw new NoSuchElementException();
            } else {
                return current.next();
            }
        }
            
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
