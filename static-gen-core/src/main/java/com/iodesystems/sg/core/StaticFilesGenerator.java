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
import java.util.HashMap;
import java.util.Map;

public class StaticFilesGenerator {
    private final File inputDir;
    private final File outputPath;
    private final Yaml yaml;
    private final PebbleEngine pageTemplateEngine;
    private final PebbleEngine titleTemplateEngine;
    private final Gson gson;
    private final Markdown markdown;

    public StaticFilesGenerator(File inputPath, File outputPath) {
        this.inputDir = inputPath;
        this.outputPath = outputPath;
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

    public void generate() throws IOException, PebbleException, ConfigurationException {
        FilesConfiguration filesConfiguration = getSite();

        outputPath.mkdirs();

        // Copy assets
        for (String asset : filesConfiguration.getAssets()) {
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

        Map<String, Object> model = getModel(filesConfiguration);
        // Build routes
        for (Map.Entry<String, FileConfiguration> entry : filesConfiguration.getFiles().entrySet()) {
            FileConfiguration route = entry.getValue();
            route.setPath(entry.getKey());
            buildRoute(model, route);
        }
    }

    private Map<String, Object> getModel(ModelConfiguration modelConfiguration) throws FileNotFoundException, ConfigurationException {
        Map<String, Object> model = new HashMap<>();
        if (modelConfiguration.getModel() != null) {
            for (Map.Entry<String, Object> entry : modelConfiguration.getModel().entrySet()) {
                if (entry.getValue() instanceof String && entry.getValue().toString().startsWith("@")) {
                    model.put(entry.getKey(), getModels(entry.getValue().toString().substring(1)));
                } else {
                    model.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return model;
    }

    private FilesConfiguration getSite() throws ConfigurationException {
        try {
            return yaml.loadAs(new FileInputStream(new File(inputDir, "site.yml")), FilesConfiguration.class);
        } catch (FileNotFoundException e) {
            try {
                return gson.fromJson(new FileReader(new File(inputDir, "site.json")), FilesConfiguration.class);
            } catch (FileNotFoundException e1) {
                throw new ConfigurationException("Could not find site.yml or site.json in " + inputDir.getPath());
            }
        }
    }

    private void buildRoute(Map<String, Object> parentModel, FileConfiguration route) throws PebbleException, IOException, ConfigurationException {
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
            for (Map<String, Object> fileModel : getModels(route.getFileModels())) {
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
    }

    private Models getModels(String fileModels) throws FileNotFoundException, ConfigurationException {
        File fileModel = new File(inputDir, fileModels);

        if (fileModels.endsWith("json")) {
            FileReader fileReader = new FileReader(fileModel);
            return gson.fromJson(fileReader, Models.class);
        } else if (fileModels.endsWith("yml")) {
            FileReader fileReader = new FileReader(fileModel);
            return yaml.loadAs(fileReader, Models.class);
        } else if (fileModel.isDirectory()) {
            Models models = new Models();
            for (File file : fileModel.listFiles()) {
                // Take up the file
                models.add(getModel(file));
            }
            return models;
        } else {
            throw new ConfigurationException("External contexts file not supported, please use json or yml");
        }
    }

    private Model getModel(File file) throws ConfigurationException {
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
        } else {
            throw new ConfigurationException("Unsupported file type: " + file.getPath());
        }
    }

}
