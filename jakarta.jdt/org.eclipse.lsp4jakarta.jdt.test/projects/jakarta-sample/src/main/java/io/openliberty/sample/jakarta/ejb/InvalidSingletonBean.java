package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Singleton;

/**
 * Invalid session bean - Lacks the required public no-arg constructor.
 */

@Singleton
public class InvalidSingletonBean {
    
    private String config;
    
    public InvalidSingletonBean(String config) {
        this.config = config;
    }
    
    public String getConfig() {
        return config;
    }
    
    public void setConfig(String config) {
        this.config = config;
    }
}
