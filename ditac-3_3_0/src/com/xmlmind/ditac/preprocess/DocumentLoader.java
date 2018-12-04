/*
 * Copyright (c) 2018 XMLmind Software. All rights reserved.
 *
 * Author: Hussein Shafie
 *
 * This file is part of the XMLmind DITA Converter project.
 * For conditions of distribution and use, see the accompanying LEGAL.txt file.
 */
package com.xmlmind.ditac.preprocess;

import java.net.URL;
import org.w3c.dom.Document;
import com.xmlmind.util.Console;

/**
 * A document loader which can load topics and maps in a format 
 * other than DITA (examples: XHTML, Markdown).
 */
public interface DocumentLoader {
    /**
     * Load document from specified URL.
     *
     * @param url URL of the document to be loaded
     * @param validate if <code>true</code>, validate loaded document
     * @param console the console on which messages issued during 
     * document loading are to be displayed. May be <code>null</code>.
     * @return loaded document
     * @exception Exception if, for any reason, the document cannot be loaded
     */
    public Document load(URL url, boolean validate, Console console)
        throws Exception;
}
