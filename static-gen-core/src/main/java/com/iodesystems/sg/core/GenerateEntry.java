package com.iodesystems.sg.core;

import java.util.Map;

public class GenerateEntry {

    private String name;
    private String path;
    private String view;
    private Object model;
    private Map<String, GenerateEntry> files;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getModel() {
        return model;
    }

    public void setModel(Object model) {
        this.model = model;
    }

    public Map<String, GenerateEntry> getFiles() {
        return files;
    }

    public void setFiles(Map<String, GenerateEntry> files) {
        this.files = files;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }
}
