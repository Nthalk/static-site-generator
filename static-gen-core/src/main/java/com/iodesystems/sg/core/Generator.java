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
import java.util.List;
import java.util.Map;

public class Generator {
    private final File inputDir;
    private final File outputPath;
    private final String manifest;
    private final Yaml yaml;
    private final PebbleEngine fileTemplateEngine;
    private final PebbleEngine stringTemplateEngine;
    private final Gson gson;
    private final Markdown markdown;

    public Generator(File inputPath, File outputPath, String manifest) {
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
        fileTemplateEngine = pebbleEngineBuilder.build();
        stringTemplateEngine = new PebbleEngine.Builder().loader(new StringLoader()).build();
    }

    public void generate() throws ConfigurationException {
        Manifest manifest = getManifest();

        stringTemplateEngine.getTemplateCache().invalidateAll();
        fileTemplateEngine.getTemplateCache().invalidateAll();

        outputPath.mkdirs();
        try {
            // Copy assets
            if (manifest.getCopy() != null) {
                for (String asset : manifest.getCopy()) {
                    File copyFile = new File(inputDir, asset);
                    File outputAsset = new File(outputPath, asset);
                    if (copyFile.isDirectory()) {
                        outputAsset.mkdirs();
                        FileUtils.copyDirectory(copyFile, outputAsset);
                    } else if (copyFile.exists()) {
                        outputAsset.getParentFile().mkdirs();
                        FileUtils.copyFile(copyFile, outputAsset);
                    }
                }
            }
            // Filter assets
            if (manifest.getGenerate() != null) {
                for (String asset : manifest.getFilter()) {
                    File filterFile = new File(inputDir, asset);
                    File outputAsset = new File(outputPath, asset);
                    if (filterFile.isDirectory()) {
                        outputAsset.mkdirs();
                        filterDirectory(manifest, filterFile, outputAsset);
                    } else if (filterFile.exists()) {
                        outputAsset.getParentFile().mkdirs();
                        filterFile(manifest, filterFile, outputAsset);
                    }
                }
            }

            // Build routes
            if (manifest.getGenerate() != null) {
                for (Map.Entry<String, GenerateEntry> entry : manifest.getGenerate().entrySet()) {
                    GenerateEntry route = entry.getValue();
                    route.setPath(entry.getKey());
                    renderFile(manifest, route);
                }
            }
        } catch (IOException | PebbleException e) {
            throw new ConfigurationException("Error loading resources", e);
        }
    }

    private void filterDirectory(Manifest manifest, File input, File output) throws IOException, PebbleException {
        File[] files = input.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                filterDirectory(manifest, file, new File(output, file.getName()));
            } else {
                filterFile(manifest, file, new File(output, file.getName()));
            }
        }
    }

    private void filterFile(Manifest manifest, File input, File output) throws IOException, PebbleException {
        fileTemplateEngine.getTemplate(input.getPath()).evaluate(new FileWriter(output), manifest.getModel());
    }

    private Manifest getManifest() throws ConfigurationException {
        try {
            return yaml.loadAs(new FileInputStream(new File(inputDir, manifest)), Manifest.class);
        } catch (FileNotFoundException e) {
            try {
                return gson.fromJson(new FileReader(new File(inputDir, manifest)), Manifest.class);
            } catch (FileNotFoundException e1) {
                throw new ConfigurationException("Could not find " + manifest + " in " + inputDir.getPath());
            }
        }
    }

    private void renderFile(Manifest manifest, GenerateEntry route) throws IOException, ConfigurationException {
        try {
            final String outputFileTemplate;
            if (route.getPath().endsWith("/")) {
                outputFileTemplate = route.getPath() + manifest.getConfig().getEmptyFileName();
            } else {
                outputFileTemplate = route.getPath();
            }

            // Imported contexts, used for link building

            Object routeModel = route.getModel();
            Model model = new Model(manifest.getModel());
            if (routeModel == null) {
                // Support empty model
                renderFile(outputFileTemplate, route.getView(), model);
            } else if (routeModel instanceof Map<?, ?>) {
                // Support inline model
                model.putAll((Map<String, Object>) routeModel);
                renderFile(outputFileTemplate, route.getView(), model);
            } else if (routeModel instanceof List<?>) {
                // Support multiple yaml files
                for (Object routeModelEntry : ((List<?>) routeModel)) {
                    if (routeModelEntry instanceof String) {
                        renderExtractedModels(route, outputFileTemplate, (String) routeModelEntry, model);
                    } else {
                        throw new ConfigurationException("Unknown model type:" + routeModelEntry.getClass().getName());
                    }
                }
            } else if (routeModel instanceof String) {
                // Support string loading
                renderExtractedModels(route, outputFileTemplate, (String) routeModel, model);
            } else {
                // No idea what this is...
                throw new ConfigurationException("Unknown model type:" + model.getClass().getName());
            }

        } catch (PebbleException e) {
            throw new ConfigurationException("Could not render template", e);
        }
    }

    private void renderExtractedModels(GenerateEntry route, String outputFileTemplate, String routeModel, Model model) throws PebbleException, IOException, ConfigurationException {
        for (Model extracedModel : extractModels(new File(inputDir, routeModel))) {
            renderFile(outputFileTemplate, route.getView(), new Model(model, extracedModel));
        }
    }

    private List<Model> extractModels(File file) throws IOException, ConfigurationException {
        List<Model> models = new ArrayList<>();

        if (file.getName().endsWith(".yml")) {
            models.add(withFileInfo(loadYamlModel(file), file));
        } else if (file.getName().endsWith(".json")) {
            models.add(withFileInfo(loadJsonModel(file), file));
        } else if (file.getName().endsWith(".md")) {
            models.add(withFileInfo(loadMarkdownModel(file), file));
        } else {
            if (file.isDirectory()) {
                String[] files = file.list();
                if (files != null) {
                    for (String directoryFile : files) {
                        models.addAll(extractModels(new File(file, directoryFile)));
                    }
                }
            } else {
                models.add(withFileInfo(loadGenericFileModel(file), file));
            }
        }
        return models;
    }

    private Model withFileInfo(Model model, File file) {
        model.put("fileName", file.getName());
        model.put("fileModified", file.lastModified());
        return model;
    }

    private Model loadJsonModel(File file) throws FileNotFoundException {
        return new Model(gson.fromJson(new FileReader(file), Map.class));
    }

    private void renderFile(String outputFileTemplate, String view, Model model) throws PebbleException, IOException {
        StringWriter outputFileWriter = new StringWriter();
        PebbleTemplate template = fileTemplateEngine.getTemplate(view);
        stringTemplateEngine.getTemplate(outputFileTemplate).evaluate(outputFileWriter, model);
        File renderOutputFile = new File(outputPath, outputFileWriter.getBuffer().toString());
        renderOutputFile.getParentFile().mkdirs();
        template.evaluate(new FileWriter(renderOutputFile), model);
    }

    private Model loadGenericFileModel(File file) throws IOException {
        Model model = new Model();
        model.put("content", FileUtils.readFileToString(file, Charset.defaultCharset()));
        return model;
    }

    private Model loadYamlModel(File file) throws FileNotFoundException {
        FileReader fileReader = new FileReader(file);
        return new Model(yaml.loadAs(fileReader, Map.class));
    }

    private Model loadMarkdownModel(File file) throws ConfigurationException {
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
            Model model = new Model(yaml.loadAs(dataContents.toString(), Map.class));
            model.put("content", markdown.render(contents.toString()));
            return model;
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Unsupported file does not exist: " + file.getPath(), e);
        } catch (IOException e) {
            throw new ConfigurationException("Error Reading lines in file: " + file.getPath(), e);
        }
    }

}
