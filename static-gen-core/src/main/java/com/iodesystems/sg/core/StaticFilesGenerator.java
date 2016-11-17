package com.iodesystems.sg.core;

import com.google.gson.Gson;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.StringLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaticFilesGenerator {
    private final File inputDir;
    private final File outputPath;
    private final String manifest;
    private final Yaml yaml;
    private final PebbleEngine pageTemplateEngine;
    private final PebbleEngine titleTemplateEngine;
    private final Gson gson;
    private final Markdown markdown;

    public StaticFilesGenerator(File inputPath, File outputPath, String manifest) {
        this.inputDir = inputPath;
        this.outputPath = outputPath;
        this.manifest = manifest;
        this.yaml = new Yaml();
        this.gson = new Gson();

        markdown = new Markdown();

        PebbleEngine.Builder pebbleEngineBuilder = new PebbleEngine.Builder();
        FileLoader fileLoader = new FileLoader();
        fileLoader.setPrefix(inputDir.getAbsolutePath());
        pebbleEngineBuilder.loader(fileLoader);
        pageTemplateEngine = pebbleEngineBuilder.build();
        titleTemplateEngine = new PebbleEngine.Builder().loader(new StringLoader()).build();
    }

    public void generate() throws ConfigurationException {
        FilesConfiguration filesConfiguration = getFilesConfiguration();

        titleTemplateEngine.getTemplateCache().invalidateAll();
        pageTemplateEngine.getTemplateCache().invalidateAll();

        outputPath.mkdirs();
        try {
            // Copy assets
            for (String asset : filesConfiguration.getCopy()) {
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

            Map<String, Object> baseModel = getModel(filesConfiguration);
            // Build routes
            for (Map.Entry<String, FileConfiguration> entry : filesConfiguration.getFiles().entrySet()) {
                FileConfiguration route = entry.getValue();
                route.setPath(entry.getKey());
                buildRoute(baseModel, route);
            }
        } catch (IOException e) {
            throw new ConfigurationException("Error loading resources", e);
        }
    }

    private Map<String, Object> getModel(ModelConfiguration modelConfiguration) throws FileNotFoundException, ConfigurationException {
        Map<String, Object> model = new HashMap<>();
        if (modelConfiguration.getModel() != null) {
            for (Map.Entry<String, Object> entry : modelConfiguration.getModel().entrySet()) {
                if (entry.getValue() instanceof String && entry.getValue().toString().startsWith("@")) {
                    model.put(entry.getKey(), getModel(new File(inputDir, entry.getValue().toString().substring(1))));
                } else {
                    model.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return model;
    }

    private FilesConfiguration getFilesConfiguration() throws ConfigurationException {
        try {
            return yaml.loadAs(new FileInputStream(new File(inputDir, manifest)), FilesConfiguration.class);
        } catch (FileNotFoundException e) {
            try {
                return gson.fromJson(new FileReader(new File(inputDir, manifest)), FilesConfiguration.class);
            } catch (FileNotFoundException e1) {
                throw new ConfigurationException("Could not find " + manifest + " in " + inputDir.getPath());
            }
        }
    }

    private void buildRoute(Map<String, Object> parentModel, FileConfiguration route) throws IOException, ConfigurationException {
        try {
            PebbleTemplate template = pageTemplateEngine.getTemplate(route.getView());

            final String outputFileTemplate;
            if (route.getPath().endsWith("/")) {
                outputFileTemplate = route.getPath() + "index.html";
            } else {
                outputFileTemplate = route.getPath();
            }

            // Build context
            Map<String, Object> model = new HashMap<>();
            model.putAll(parentModel);
            model.putAll(getModel(route));

            // Imported contexts, used for link building
            StringWriter outputFileWriter = new StringWriter();
            if (route.getFileModels() != null) {
                for (Object fileModelData : getModels(route.getFileModels())) {
                    Map<String, Object> fileModel = new HashMap<>();
                    if (fileModelData instanceof Map) {
                        fileModel.putAll((Map) fileModelData);
                    } else if (fileModelData instanceof List) {
                        fileModel.put("items", fileModelData);
                    } else {
                        throw new ConfigurationException("Unknown type to add to model: " + fileModelData.toString());
                    }
                    fileModel.putAll(model);
                    titleTemplateEngine.getTemplate(outputFileTemplate).evaluate(outputFileWriter, fileModel);
                    File outputFile = new File(outputPath, outputFileWriter.getBuffer().toString());
                    File parentFile = outputFile.getParentFile();
                    if (!parentFile.exists() && !parentFile.mkdirs()) {
                        throw new IOException("Could not make directories required for " + parentFile.getAbsolutePath());
                    }
                    template.evaluate(new FileWriter(outputFile), fileModel);
                }
            } else {
                titleTemplateEngine.getTemplate(outputFileTemplate).evaluate(outputFileWriter, model);
                template.evaluate(new FileWriter(new File(outputPath, outputFileWriter.getBuffer().toString())), model);
            }

            Map<String, FileConfiguration> childRoutes = route.getFiles();
            if (childRoutes != null) {
                for (Map.Entry<String, FileConfiguration> entry : childRoutes.entrySet()) {
                    FileConfiguration childRoute = entry.getValue();
                    childRoute.setPath(entry.getKey());
                    buildRoute(model, childRoute);
                }
            }
        } catch (PebbleException e) {
            throw new ConfigurationException("Could not render template", e);
        }
    }

    private List<Object> getModels(String fileModels) throws FileNotFoundException, ConfigurationException {
        File fileModel = new File(inputDir, fileModels);

        if (fileModels.endsWith("json")) {
            FileReader fileReader = new FileReader(fileModel);
            return gson.fromJson(fileReader, List.class);
        } else if (fileModels.endsWith("yml")) {
            FileReader fileReader = new FileReader(fileModel);
            return yaml.loadAs(fileReader, List.class);
        } else if (fileModel.isDirectory()) {
            List<Object> models = new ArrayList<Object>();
            for (File file : fileModel.listFiles()) {
                // Take up the file
                models.add(getModel(file));
            }
            return models;
        } else {
            throw new ConfigurationException("External contexts file not supported, please use json or yml");
        }
    }

    private Object getModel(File file) throws ConfigurationException {
        try {
            if (file.getPath().endsWith(".md")) {
                try {
                    LineIterator lineIterator = FileUtils.lineIterator(file);
                    StringBuilder contents = new StringBuilder();
                    StringBuilder dataContents = new StringBuilder();
                    boolean isInDataBlock = false;
                    while (lineIterator.hasNext()) {
                        String line = lineIterator.next();

                        if (!isInDataBlock && contents.length() == 0 && line.startsWith("```")) {
                            isInDataBlock = true;
                            continue;
                        } else if (isInDataBlock && line.startsWith("```")) {
                            isInDataBlock = false;
                            continue;
                        }

                        if (isInDataBlock) {
                            dataContents.append(line);
                            dataContents.append("\n");
                        } else {
                            contents.append(line);
                            contents.append("\n");
                        }
                    }
                    Model model = new Model();
                    model.putAll(yaml.loadAs(dataContents.toString(), Model.class));
                    model.put("fileName", file.getName());
                    model.put("fileModified", file.lastModified());
                    model.put("content", markdown.render(contents.toString()));
                    return model;
                } catch (FileNotFoundException e) {
                    throw new ConfigurationException("Unsupported file does not exist: " + file.getPath(), e);
                } catch (IOException e) {
                    throw new ConfigurationException("Error Reading lines in file: " + file.getPath(), e);
                }
            } else if (file.getName().endsWith(".yml")) {
                FileReader fileReader = new FileReader(file);
                return yaml.loadAs(fileReader, Model.class);
            } else {
                Model model = new Model();
                model.put("fileName", file.getName());
                model.put("fileModified", file.lastModified());
                model.put("content", FileUtils.readFileToString(file, Charset.defaultCharset()));
                return model;
            }
        } catch (IOException e) {
            throw new ConfigurationException("Error Reading content in file: " + file.getPath(), e);
        }
    }

}
