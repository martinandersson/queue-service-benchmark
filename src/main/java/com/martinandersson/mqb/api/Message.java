package com.martinandersson.mqb.api;

import java.util.function.Supplier;

/**
 * A message.<p>
 * 
 * Use {@link #get()} to get message content.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public interface Message extends Supplier<String>
{
    /**
     * Returns the message Id.
     * 
     * The returned Id may be {@code -1}, in which case all messages from the
     * same queue service return {@code -1}. It is assumed that the queue
     * service implementation has no use for a value-based message identity.
     * 
     * @implSpec
     * The default implementation return {@code -1}.
     * 
     * @return the message Id
     */
    default long getId() {
        return -1;
    }
    
    /**
     * Returns the queue [name] this message belongs to.
     * 
     * @return the queue [name] (never {@code null})
     */
    String getQueue();
    
    /**
     * Returns the message content.
     * 
     * @return the message content (never {@code null})
     */
    @Override
    public String get();
}