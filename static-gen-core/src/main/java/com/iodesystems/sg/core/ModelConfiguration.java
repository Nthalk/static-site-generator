package com.iodesystems.sg.core;

import java.util.HashMap;
import java.util.Map;

public class ModelConfiguration {
    private Map<String, Object> model = new HashMap<>();

    public Map<String, Object> getModel() {
        return model;
    }

    public void setModel(Map<String, Object> model) {
        this.model = model;
    }

}
