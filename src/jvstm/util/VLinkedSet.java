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
import jvstm.Atomic;

import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import java.util.NoSuchElementException;

import java.lang.reflect.Array;

public class VLinkedSet<E> implements Set<E> {
    private final VBox<Cons<E>> entries = new VBox<Cons<E>>((Cons<E>)Cons.empty());
    private final VBoxInt size = new VBoxInt(0);

    public VLinkedSet() {
    }

    public VLinkedSet(Collection<? extends E> c) {
        addAll(c);
    }

    public int size() {
        return size.getInt();
    }

    public boolean isEmpty() {
        return entries.get().isEmpty();
    }

    public boolean contains(Object o) {
        return entries.get().contains(o);
    }

    public Iterator<E> iterator() {
        return new VLinkedSetIterator<E>();
    }

    @Atomic
    public Object[] toArray() {
        int size = size();
        Cons<E> elems = entries.get();
        Object[] result = new Object[size];

        Iterator iter = elems.iterator();
        for (int i = 0; i < size; i++) {
            result[i] = iter.next();
        }

        return result;
    }

    @Atomic
    public <T> T[] toArray(T[] a) {
        int size = size();
        Cons<E> elems = entries.get();

        if (a.length < size) {
            a = (T[])Array.newInstance(a.getClass().getComponentType(), size);
        }

        Iterator<E> iter = elems.iterator();
        Object[] result = a;
        for (int i = 0; i < size; i++) {
            result[i] = iter.next();
        }

        if (size < a.length) {
            a[size] = null;
        }

        return a;
    }

    @Atomic
    public boolean add(E o) {
        Cons<E> oldElems = entries.get();
        Cons<E> newElems = adjoin(oldElems, o);

        if (oldElems == newElems) {
            return false;
        } else {
            entries.put(newElems);
            size.inc();
            return true;
        }
    }

    @Atomic
    public boolean remove(Object o) {
        Cons<E> oldElems = entries.get();
        Cons<E> newElems = oldElems.removeFirst(o);

        if (oldElems == newElems) {
            return false;
        } else {
            entries.put(newElems);
            size.dec();
            return true;
        }
    }

    public boolean containsAll(Collection<?> c) {
        Cons<E> elems = entries.get();

        for (Object o : c) {
            if (! elems.contains(o)) {
                return false;
            }
        }

        return true;
    }

    @Atomic
    public boolean addAll(Collection<? extends E> c) {
        Cons<E> prev = entries.get();
        int added = 0;

        for (E o : c) {
            Cons<E> next = adjoin(prev, o);
            if (prev != next) {
                added++;
                prev = next;
            }
        }

        if (added > 0) {
            entries.put(prev);
            size.inc(added);
        }

        return added > 0;
    }

    @Atomic
    public boolean retainAll(Collection<?> c) {
        Cons<E> result = Cons.empty();
        Cons<E> elems = entries.get();

        int removed = 0;

        for (E o : elems) {
            if (c.contains(o)) {
                result = result.cons(o);
            } else {
                removed++;
            }
        }

        if (removed > 0) {
            entries.put(result.reverse());
            size.dec(removed);
        }

        return removed > 0;
    }

    @Atomic
    public boolean removeAll(Collection<?> c) {
        Cons<E> prev = entries.get();
        int removed = 0;

        for (Object o : c) {
            Cons<E> next = prev.removeFirst(o);
            if (prev != next) {
                removed++;
                prev = next;
            }
        }

        if (removed > 0) {
            entries.put(prev);
            size.dec(removed);
        }

        return removed > 0;
    }

    @Atomic
    public void clear() {
        entries.put((Cons<E>)Cons.empty());
        size.putInt(0);
    }

    @Atomic
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (! (o instanceof Set)) {
            return false;
        }

        Set otherSet = (Set)o;
        if (this.size() != otherSet.size()) {
            return false;
        }

        return containsAll(otherSet);
    }
   
    @Atomic
    public int hashCode() {
        int value = 0;
        for (E o : entries.get()) {
            if (o != null) {
                value += o.hashCode();
            }
        }
        return value;
    }

    private Cons<E> adjoin(Cons<E> list, E elem) {
        return (list.contains(elem)) ? list : list.cons(elem);
    }

    protected void removeCons(Cons cons) {
        Cons<E> oldElems = entries.get();
        Cons<E> newElems = oldElems.removeCons(cons);

        if (oldElems != newElems) {
            entries.put(newElems);
            size.dec();
        }        
    }

    private class VLinkedSetIterator<T> implements Iterator<T> {
        private Cons<T> current;
        private Cons<T> previous = null;

        VLinkedSetIterator() {
            this.current = (Cons<T>)VLinkedSet.this.entries.get();
        }
        
        public boolean hasNext() { 
            return (! current.isEmpty());
        }
        
        public T next() { 
            if (current.isEmpty()) {
                throw new NoSuchElementException();
            } else {
                T result = current.first();
                previous = current;
                current = current.rest();
                return result;
            }
        }
            
        public void remove() {
            if (previous == null) {
                throw new IllegalStateException();
            } else {
                VLinkedSet.this.removeCons(previous);
                previous = null;
            }
        }
    }
}
