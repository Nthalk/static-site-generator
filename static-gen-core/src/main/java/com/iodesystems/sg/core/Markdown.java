package com.iodesystems.sg.core;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class Markdown {
    private final Parser parser;
    private final HtmlRenderer renderer;

    public Markdown() {
        this.parser = Parser.builder().build();
        this.renderer = HtmlRenderer.builder().build();
    }

    public String render(String src) {
        return renderer.render(parser.parse(src));
    }
}
