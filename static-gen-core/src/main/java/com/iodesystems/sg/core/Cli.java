package com.iodesystems.sg.core;

import org.docopt.Docopt;

import java.io.File;
import java.util.Map;

public class Cli {
    private static final String DOC =
        "Static Gen.\n"
            + "\n"
            + "Usage:\n"
            + "  sg [--manifest=FILE] [--source=DIR] [--out=DIR] [--watch] [--serve [--port=PORT]]\n"
            + "  sg (-h | --help)\n"
            + "  sg --version\n"
            + "\n"
            + "Options:\n"
            + "  -h --help                  Show this screen.\n"
            + "  --version                  Show version.\n"
            + "  -m FILE --manifest=FILE    Manifest file [default: files.yml].\n"
            + "  -o DIR --out=DIR           Output directory [default: dist].\n"
            + "  -i DIR --source=DIR        Input directory [default: src].\n"
            + "  -w --watch                 Watch for input changes [default: false].\n"
            + "  -s --serve                 Serve the output directory [default: false].\n"
            + "  -p PORT --port=PORT        Port to serve on [default: 3000].\n"
            + "\n";


    public static void main(String[] args) {
        Map<String, Object> opts = new Docopt(DOC)
            .withHelp(true)
            .withVersion("Static Gen: " + Cli.class.getPackage().getImplementationVersion())
            .parse(args);

        String source = opts.get("--source").toString();
        String out = opts.get("--out").toString();
        String manifest = opts.get("--manifest").toString();
        Integer port = Integer.valueOf(opts.get("--port").toString());
        Boolean serve = Boolean.valueOf(opts.get("--serve").toString());
        Boolean watch = Boolean.valueOf(opts.get("--watch").toString());

        StaticFilesGenerator generator = new StaticFilesGenerator(new File(source), new File(out), manifest);

        try {
            generator.generate();
            if (watch) {
                new StaticFilesWatcher(source, generator, new Log() {
                    @Override
                    public void info(String message) {
                        System.out.println(message);
                    }

                    @Override
                    public void error(String message, Throwable cause) {
                        System.err.println(message);
                    }
                }).start();
            }

            if (serve) {
                new StaticFilesServer(out, port).start();
            }

        } catch (ConfigurationException e) {
            System.err.println("fatal: " + e.getMessage());
        }
    }
}
