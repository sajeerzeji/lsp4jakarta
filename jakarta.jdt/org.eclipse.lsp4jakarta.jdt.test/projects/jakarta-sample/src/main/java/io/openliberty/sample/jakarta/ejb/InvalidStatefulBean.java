package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Stateful;

/**
 * Invalid session bean - Lacks the required public no-arg constructor.
 */

@Stateful
public class InvalidStatefulBean {
    
    private int counter;
    
    public InvalidStatefulBean(int initialValue) {
        this.counter = initialValue;
    }
    
    public void increment() {
        counter++;
    }
    
    public int getCounter() {
        return counter;
    }
}
