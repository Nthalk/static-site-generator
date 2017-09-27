package com.iodesystems.sg.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Manifest {

    private Configuration config = new Configuration();
    private List<String> copy;
    private List<String> filter;
    private Map<String, GenerateEntry> generate;
    private Map<String, Object> model = new HashMap<>();

    public Configuration getConfig() {
        return config;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public void setModel(Map<String, Object> model) {
        this.model = model;
    }

    public List<String> getCopy() {
        return copy;
    }

    public void setCopy(List<String> copy) {
        this.copy = copy;
    }

    public Map<String, GenerateEntry> getGenerate() {
        return generate;
    }

    public void setGenerate(Map<String, GenerateEntry> generate) {
        this.generate = generate;
    }

    public List<String> getFilter() {
        return filter;
    }

    public void setFilter(List<String> filter) {
        this.filter = filter;
    }
}
