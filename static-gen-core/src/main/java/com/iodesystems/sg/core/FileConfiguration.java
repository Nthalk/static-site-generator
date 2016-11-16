package com.iodesystems.sg.core;

import java.util.Map;

public class FileConfiguration extends ModelConfiguration {

    private String path;
    private String view;
    private String fileModels;
    private Map<String, FileConfiguration> files;

    public String getFileModels() {
        return fileModels;
    }

    public void setFileModels(String fileModels) {
        this.fileModels = fileModels;
    }

    public Map<String, FileConfiguration> getFiles() {
        return files;
    }

    public void setFiles(Map<String, FileConfiguration> files) {
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
