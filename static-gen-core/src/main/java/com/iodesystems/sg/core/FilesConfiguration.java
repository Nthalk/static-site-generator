package com.iodesystems.sg.core;

import java.util.List;
import java.util.Map;

public class FilesConfiguration extends ModelConfiguration {

    private List<String> assets;
    private Map<String, FileConfiguration> files;

    public List<String> getAssets() {
        return assets;
    }

    public void setAssets(List<String> assets) {
        this.assets = assets;
    }

    public Map<String, FileConfiguration> getFiles() {
        return files;
    }

    public void setFiles(Map<String, FileConfiguration> files) {
        this.files = files;
    }

}
