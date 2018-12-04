/*
 * Copyright (c) 2018 XMLmind Software. All rights reserved.
 *
 * Author: Hussein Shafie
 *
 * This file is part of the XMLmind DITA Converter project.
 * For conditions of distribution and use, see the accompanying LEGAL.txt file.
 */
package com.xmlmind.ditac.preprocess;

import com.xmlmind.util.Console;

/**
 * Factory used to create loaders which can load topics and maps in a format 
 * other than DITA (examples: XHTML, Markdown).
 */
public interface DocumentLoaderFactory {
    /**
     * Name of the factory.
     */
    String getName();

    /**
     * File extension supported by the loaders created by the factory.
     * <p>Must be lower case. May not start with <tt>.</tt>. Example:
     * <code>{ "md", "markdown" }</code>.
     */
    String[] getExtensions();
    
    /**
     * Create a document loader.
     *
     * @param options options parameterizing the created document loader. 
     * May be <code>null</code>.
     * @param console the console on which messages are to be displayed.
     * May be <code>null</code>.
     * @return a newly created document loader
     * @exception Exception if, for any reason, the document loader 
     * cannot be created
     */
    DocumentLoader createLoader(String[] options, Console console)
        throws Exception;
}
