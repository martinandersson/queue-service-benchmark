package com.martinandersson.mqb.benchmark;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * Fixed-cost looping iterator.
 * 
 * @param <E>  element type
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public final class FixedCostLoopingIterator<E> implements Iterator<E>
{
    private final E[] elements;
    
    private int pos;
    
    
    
    public FixedCostLoopingIterator(Stream<E> elements) {
        @SuppressWarnings("unchecked")
        E[] e0 = (E[]) elements.toArray();
        
        this.elements = e0;
        
        if (this.elements.length == 0) {
            throw new NoSuchElementException();
        }
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public E next() {
        final E e = elements[pos++];
                
        if (pos == elements.length) {
            pos = 0;
        }
        
        return e;
    }
}