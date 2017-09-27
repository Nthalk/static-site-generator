package com.iodesystems.sg.core;

import java.util.HashMap;
import java.util.Map;

public class Model extends HashMap<String, Object> {
    public Model(Map... maps) {
        for (Map map : maps) {
            if (map != null) putAll(map);
        }
    }

    public Model() {
    }
}
