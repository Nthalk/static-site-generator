package com.iodesystems.sg.core;

import com.google.gson.Gson;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.StringLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class StaticSiteGenerator {
    private final File inputDir;
    private final File outputPath;
    private final Yaml yaml;
    private final PebbleEngine pageTemplateEngine;
    private final PebbleEngine titleTemplateEngine;
    private final Gson gson;

    public StaticSiteGenerator(File inputPath, File outputPath) {
        this.inputDir = inputPath;
        this.outputPath = outputPath;
        this.yaml = new Yaml();
        this.gson = new Gson();

        PebbleEngine.Builder pebbleEngineBuilder = new PebbleEngine.Builder();
        FileLoader fileLoader = new FileLoader();
        fileLoader.setPrefix(inputDir.getAbsolutePath());
        pebbleEngineBuilder.loader(fileLoader);
        pageTemplateEngine = pebbleEngineBuilder.build();

        titleTemplateEngine = new PebbleEngine.Builder().loader(new StringLoader()).build();
    }

    public void generate() throws IOException, PebbleException, SiteConfigurationException {
        SiteConfiguration siteConfiguration = getSite();

        outputPath.mkdirs();

        // Copy assets
        for (String asset : siteConfiguration.getAssets()) {
            File assetFile = new File(inputDir, asset);
            File outputAsset = new File(outputPath, asset);
            if (assetFile.isDirectory()) {
                outputAsset.mkdirs();
                FileUtils.copyDirectory(assetFile, outputAsset);
            } else {
                outputAsset.getParentFile().mkdirs();
                FileUtils.copyFile(assetFile, outputAsset);
            }
        }

        Map<String, Object> context = getContext(siteConfiguration);
        // Build routes
        for (Map.Entry<String, RouteConfiguration> entry : siteConfiguration.getRoutes().entrySet()) {
            RouteConfiguration route = entry.getValue();
            route.setPath(entry.getKey());
            buildRoute(context, route);
        }
    }

    private Map<String, Object> getContext(ContextConfiguration contextConfiguration) throws FileNotFoundException, SiteConfigurationException {
        Map<String, Object> context = new HashMap<>();
        if (contextConfiguration.getContext() != null) context.putAll(contextConfiguration.getContext());
        if (contextConfiguration.getImportContexts() != null) {
            for (Map.Entry<String, String> entry : contextConfiguration.getImportContexts().entrySet()) {
                context.put(entry.getKey(), getContexts(entry.getValue()));
            }
        }
        return context;
    }

    private SiteConfiguration getSite() throws SiteConfigurationException {
        try {
            return yaml.loadAs(new FileInputStream(new File(inputDir, "site.yml")), SiteConfiguration.class);
        } catch (FileNotFoundException e) {
            try {
                return gson.fromJson(new FileReader(new File(inputDir, "site.json")), SiteConfiguration.class);
            } catch (FileNotFoundException e1) {
                throw new SiteConfigurationException("Could not find site.yml or site.json in " + inputDir.getPath());
            }
        }
    }

    private void buildRoute(Map<String, Object> parentContext, RouteConfiguration route) throws PebbleException, IOException, SiteConfigurationException {
        PebbleTemplate template = pageTemplateEngine.getTemplate(route.getView());

        final String outputFileTemplate;
        if (route.getPath().endsWith("/")) {
            outputFileTemplate = route.getPath() + "index.html";
        } else {
            outputFileTemplate = route.getPath();
        }

        // Build context
        Map<String, Object> context = new HashMap<>();
        context.putAll(parentContext);
        context.putAll(getContext(route));

        // Imported contexts, used for link building
        StringWriter outputFileWriter = new StringWriter();
        if (route.getExpandContexts() != null) {
            Contexts contexts = getContexts(route.getExpandContexts());
            for (Map<String, Object> itemContext : contexts) {
                itemContext.putAll(context);
                titleTemplateEngine.getTemplate(outputFileTemplate).evaluate(outputFileWriter, itemContext);
                File outputFile = new File(outputPath, outputFileWriter.getBuffer().toString());
                File parentFile = outputFile.getParentFile();
                if (!parentFile.exists() && !parentFile.mkdirs()) {
                    throw new IOException("Could not make directories required for " + parentFile.getAbsolutePath());
                }
                template.evaluate(new FileWriter(outputFile), itemContext);
            }
        } else {
            titleTemplateEngine.getTemplate(outputFileTemplate).evaluate(outputFileWriter, context);
            template.evaluate(new FileWriter(new File(outputPath, outputFileWriter.getBuffer().toString())), context);
        }

        Map<String, RouteConfiguration> childRoutes = route.getRoutes();
        if (childRoutes != null) {
            for (Map.Entry<String, RouteConfiguration> entry : childRoutes.entrySet()) {
                RouteConfiguration childRoute = entry.getValue();
                childRoute.setPath(entry.getKey());
                buildRoute(context, childRoute);
            }
        }
    }

    private Contexts getContexts(String expandedContexts) throws FileNotFoundException, SiteConfigurationException {
        FileReader fileReader = new FileReader(new File(inputDir, expandedContexts));
        if (expandedContexts.endsWith("json")) {
            return gson.fromJson(fileReader, Contexts.class);
        } else if (expandedContexts.endsWith("yml")) {
            return yaml.loadAs(fileReader, Contexts.class);
        } else {
            throw new SiteConfigurationException("External contexts file not supported, please use json or yml");
        }
    }

}
