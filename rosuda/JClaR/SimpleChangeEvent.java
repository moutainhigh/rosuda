/*
 * SimpleChangeEvent.java
 *
 * Created on 12. Juli 2005, 17:13
 *
 */

package org.rosuda.JClaR;

import java.util.EventObject;

/**
 *
 * @author tobias
 */
public final class SimpleChangeEvent extends EventObject {
    
    private int message;
    
    /** Creates a new instance of SimpleChangeEvent */
    SimpleChangeEvent(final Object source, final int message) {
        super(source);
        this.message = message;
    }
    
    int getMessage(){
        return message;
    }
    
    static final int HARD_CHANGE = 1;
    private static final int UPDATE = 2;
}
