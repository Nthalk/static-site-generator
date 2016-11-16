package com.iodesystems.sg.core;

import java.util.List;
import java.util.Map;

public class SiteConfiguration extends ContextConfiguration {

    private List<String> assets;
    private Map<String, RouteConfiguration> routes;

    public List<String> getAssets() {
        return assets;
    }

    public void setAssets(List<String> assets) {
        this.assets = assets;
    }

    public Map<String, RouteConfiguration> getRoutes() {
        return routes;
    }

    public void setRoutes(Map<String, RouteConfiguration> routes) {
        this.routes = routes;
    }

}
