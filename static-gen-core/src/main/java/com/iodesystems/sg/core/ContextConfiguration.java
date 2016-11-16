package com.iodesystems.sg.core;

import java.util.HashMap;
import java.util.Map;

public class ContextConfiguration {
    private Map<String, String> importContexts = new HashMap<>();
    private Map<String, Object> context = new HashMap<>();

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }


    public Map<String, String> getImportContexts() {
        return importContexts;
    }

    public void setImportContexts(Map<String, String> importContexts) {
        this.importContexts = importContexts;
    }
}
