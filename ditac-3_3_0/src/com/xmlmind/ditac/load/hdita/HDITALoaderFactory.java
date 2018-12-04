/*
 * Copyright (c) 2018 XMLmind Software. All rights reserved.
 *
 * Author: Hussein Shafie
 *
 * This file is part of the XMLmind DITA Converter project.
 * For conditions of distribution and use, see the accompanying LEGAL.txt file.
 */
package com.xmlmind.ditac.load.hdita;

import java.net.URL;
import javax.xml.transform.URIResolver;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import com.xmlmind.util.Console;
import com.xmlmind.ditac.util.ConsoleErrorListener;
import com.xmlmind.ditac.util.Resolve;
import com.xmlmind.ditac.preprocess.DocumentLoaderFactory;

// Needed by main() ---
import java.io.File;
import org.w3c.dom.Document;
import com.xmlmind.util.StringList;
import com.xmlmind.util.URLUtil;
import com.xmlmind.ditac.util.SimpleConsole;
import com.xmlmind.ditac.util.SaveDocument;

/**
 * The factory used to create HDITA document loaders.
 */
public final class HDITALoaderFactory implements DocumentLoaderFactory {
    private static final String[] EXTENSIONS = {
        "html", "htm", "shtml", "xhtml", "xhtm", "xht"
    };

    private final Templates[] xslTemplates;

    // -----------------------------------------------------------------------

    public HDITALoaderFactory() {
        xslTemplates = new Templates[1];
    }

    public String getName() {
        return "hdita";
    }

    public String[] getExtensions() {
        return EXTENSIONS;
    }

    public HDITALoader createLoader(String[] params, Console console) 
        throws Exception {
        Templates templates = getTemplates(console);
        return new HDITALoader(templates.newTransformer());
    }

    private Templates getTemplates(Console console) throws Exception {
        synchronized (xslTemplates) {
            if (xslTemplates[0] == null) {
                TransformerFactory factory = getTransformerFactory(console);

                URL url = Resolve.resolveURI("ditac-xsl:hdita/hdita.xsl", 
                                             /*base*/ null);
                xslTemplates[0] = factory.newTemplates(
                    new StreamSource(url.toExternalForm()));
            }

            return xslTemplates[0];
        }
    }

    private static TransformerFactory getTransformerFactory(Console console) 
        throws Exception {
        // Force the use of Saxon 9.
        Class<?> cls = Class.forName("net.sf.saxon.TransformerFactoryImpl");
        TransformerFactory transformerFactory = (TransformerFactory)
            cls.getDeclaredConstructor().newInstance();

        // For use by xsl:import and xsl:include.
        URIResolver uriResolver = Resolve.createURIResolver();
        transformerFactory.setURIResolver(uriResolver);

        // A null console is OK.
        ErrorListener errorListener = new ConsoleErrorListener(console);
        transformerFactory.setErrorListener(errorListener);

        return transformerFactory;
    }

    // -----------------------------------------------------------------------

    public static void main(String[] args) 
        throws Exception {
        String[] params = StringList.EMPTY_LIST;
        boolean validate = true;
        int l = 0;

        final int argCount = args.length;
        for (; l < argCount; ++l) {
            String arg = args[l];

            if ("-p".equals(arg)) {
                if (l+2 >= argCount) {
                    usage();
                    //NOTREACHED
                }

                params = StringList.append(params, args[l+1]);
                params = StringList.append(params, args[l+2]);
                l += 2;
            } else if ("-invalid".equals(arg)) {
                validate = false;
            } else {
                if (arg.startsWith("-")) {
                    usage();
                    //NOTREACHED
                }

                break;
            }
        }

        if (l+2 != args.length) {
            usage();
            //NOTREACHED
        }

        URL inURL = URLUtil.urlOrFile(args[l]);
        File outFile = new File(args[l+1]);

        if (inURL == null) { // file:///XXX does not exist.
            usage();
            //NOTREACHED
        }

        // ---

        HDITALoaderFactory factory = new HDITALoaderFactory();
        Console console = new SimpleConsole();
        HDITALoader loader = factory.createLoader(params, console);

        Document doc = loader.load(inURL, validate, console);
        SaveDocument.save(doc, outFile);
    }

    private static void usage() {
        System.err.println(
            "Usage: java com.xmlmind.ditac.load.hdita.HDITALoaderFactory\n" +
            "    [-invalid] in_xhtml_URL_or_file out_dita_file");
        System.exit(1);
    }
}
