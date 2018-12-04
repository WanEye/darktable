/*
 * Copyright (c) 2018 XMLmind Software. All rights reserved.
 *
 * Author: Hussein Shafie
 *
 * This file is part of the XMLmind DITA Converter project.
 * For conditions of distribution and use, see the accompanying LEGAL.txt file.
 */
package com.xmlmind.ditac.load.mdita;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.net.URL;
import org.w3c.dom.Document;
import com.vladsch.flexmark.ast.Visitor;
import com.vladsch.flexmark.ast.VisitHandler;
import com.vladsch.flexmark.ast.NodeVisitor;
import com.vladsch.flexmark.ast.util.ReferenceRepository;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterNode;
import com.xmlmind.util.StringUtil;
import com.xmlmind.util.URLUtil;
import com.xmlmind.util.XMLUtil;
import com.xmlmind.util.XMLText;
import com.xmlmind.util.Console;
import com.xmlmind.ditac.preprocess.DocumentLoader;
import com.xmlmind.ditac.load.hdita.HDITALoader;
import com.xmlmind.ditac.load.hdita.HDITALoaderFactory;

/**
 * The MDITA document loader.
 */
public final class MDITALoader implements DocumentLoader {
    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    // -----------------------------------------------------------------------

    public MDITALoader(Parser parser, HtmlRenderer htmlRenderer) {
        this.parser = parser;
        this.htmlRenderer = htmlRenderer;
    }

    public Document load(URL url, boolean validate, Console console)
        throws Exception {
        InputStream in =
            new BufferedInputStream(URLUtil.openStreamNoCache(url));

        Document doc;
        try {
            doc = load(in, url, validate, console);
        } finally {
            in.close();
        }

        return doc;
    }

    public Document load(InputStream in, URL url,
                         boolean validate, Console console)
        throws Exception {
        String html = loadHTML(in);

        // ---

        URL urlForErrorMessages = URLUtil.setExtension(url, "xhtml");

        HDITALoader loader =
            (new HDITALoaderFactory()).createLoader(/*options*/ null, console);
        Document doc = 
            loader.load(new ByteArrayInputStream(html.getBytes("UTF-8")),
                        urlForErrorMessages, validate, console);

        doc.setDocumentURI(url.toExternalForm()); // Real ".md" URL.

        return doc;
    }

    // -----------------------------------------------------------------------

    /*package*/ String loadHTML(URL url)
        throws Exception {
        InputStream in =
            new BufferedInputStream(URLUtil.openStreamNoCache(url));

        String html;
        try {
            html = loadHTML(in);
        } finally {
            in.close();
        }

        return html;
    }

    /*package*/ String loadHTML(InputStream in) 
        throws IOException {
        Reader source = XMLUtil.createReader(in, "UTF-8", /*enc*/ null);
        com.vladsch.flexmark.ast.Node node = parser.parseReader(source);

        String htmlContent = htmlRenderer.render(node);

        // Why not just HTML5? 
        //
        // Because the typographic extensions generates named character
        // entities such as "&hellip;" (cannot be overridden; which is a
        // typographic extension bug).

        StringBuilder xhtml = new StringBuilder(
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<!DOCTYPE html  PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\""+
            " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "<head>");

        final String[] id = new String[1];
        final String[] title = new String[] { "" };
        int titleIndex = xhtml.length();

        VisitHandler<YamlFrontMatterNode> visitHandler =
            new VisitHandler<YamlFrontMatterNode>(
                YamlFrontMatterNode.class,
                new Visitor<YamlFrontMatterNode>() {
                    public void visit(YamlFrontMatterNode n) {
                        String key = n.getKey();
                        for (String value : n.getValues()) {
                            value = yamlToXML(value);

                            if ("title".equals(key)) {
                                title[0] = value;
                            } else if ("id".equals(key)) {
                                if (XMLText.isNCName(value)) {
                                    id[0] = value;
                                }
                            } else {
                                xhtml.append("<meta name=\"");
                                XMLText.escapeXML(key, xhtml);
                                xhtml.append("\" content=\"");
                                xhtml.append(value);
                                xhtml.append("\" />\n");
                            }
                        }
                    }
                });
        (new NodeVisitor(visitHandler)).visit(node);

        xhtml.insert(titleIndex, "</title>\n");
        xhtml.insert(titleIndex, title[0]);
        xhtml.insert(titleIndex, "<title>");

        xhtml.append("</head><body><article");
        if (id[0] != null) {
            xhtml.append(" id=\"" + XMLText.escapeXML(id[0]) + "\"");
        }
        xhtml.append(">\n");
        xhtml.append(htmlContent);
        xhtml.append("\n</article></body></html>");
        
        return xhtml.toString();
    }

    private static String yamlToXML(String s) {
        // YAML: Strings (scalars) are ordinarily unquoted, but may be
        // enclosed in double-quotes ("), or single-quotes (').  Within
        // double-quotes, special characters may be represented with
        // C-style escape sequences starting with a backslash (\).

        int length;
        if ((length = s.length()) >= 2) {
            char firstC = s.charAt(0);
            char lastC = s.charAt(length-1);

            if (firstC == '\'' && lastC == '\'') {
                s = s.substring(1, length-1);
            } else if (firstC == '"' && lastC == '"') {
                s = StringUtil.unquote(s);
            }
        }

        s = XMLText.collapseWhiteSpace(s);

        return XMLText.escapeXML(s);
    }
}
