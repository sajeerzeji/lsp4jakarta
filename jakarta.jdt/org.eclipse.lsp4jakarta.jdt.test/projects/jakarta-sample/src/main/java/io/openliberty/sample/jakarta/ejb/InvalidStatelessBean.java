package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Stateless;

/**
 * Invalid session bean - lacks the required public no-arg constructor.
 */

@Stateless
public class InvalidStatelessBean {
    
    private String name;

    public InvalidStatelessBean(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
