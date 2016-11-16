package com.iodesystems.sg.core;

import java.util.Map;

public class RouteConfiguration extends ContextConfiguration {

    private String path;
    private String view;
    private String expandContexts;
    private Map<String, RouteConfiguration> routes;

    public String getExpandContexts() {
        return expandContexts;
    }

    public void setExpandContexts(String expandContexts) {
        this.expandContexts = expandContexts;
    }

    public Map<String, RouteConfiguration> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<String, RouteConfiguration> routes) {
        this.routes = routes;
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
