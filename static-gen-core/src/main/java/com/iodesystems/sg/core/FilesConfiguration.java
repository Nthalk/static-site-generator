package com.iodesystems.sg.core;

import java.util.List;
import java.util.Map;

public class FilesConfiguration extends ModelConfiguration {

    private List<String> copy;
    private Map<String, FileConfiguration> files;

    public List<String> getCopy() {
        return copy;
    }

    public void setCopy(List<String> copy) {
        this.copy = copy;
    }

    public Map<String, FileConfiguration> getFiles() {
        return files;
    }

    public void setFiles(Map<String, FileConfiguration> files) {
        this.files = files;
    }

}
