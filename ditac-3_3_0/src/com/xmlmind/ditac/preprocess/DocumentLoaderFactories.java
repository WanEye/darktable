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
import com.xmlmind.util.ThrowableUtil;
import com.xmlmind.util.StringList;
import com.xmlmind.util.ArrayUtil;
import com.xmlmind.util.URLUtil;
import com.xmlmind.util.Log;
import com.xmlmind.util.Console;

/**
 * Registry of all {@link DocumentLoaderFactory}s.
 * <p>This class is thread-safe.
 */
public final class DocumentLoaderFactories {
    private static final Object LOCK = new Object();
    private static DocumentLoaderFactory[] factories =
        new DocumentLoaderFactory[0];

    // ------------------------------------------------------------------------
    
    /**
     * Registers specified factory.
     */
    public static void register(DocumentLoaderFactory factory) {
        synchronized (LOCK) {
            int index = -1;
            final String factoryName = factory.getName();
            for (int i = factories.length-1; i >= 0; --i) {
                if (factories[i].getName().equals(factoryName)) {
                    index = i;
                    break;
                }
            }

            if (index >= 0) {
                factories = ArrayUtil.removeAt(factories, index);
            }
            factories = ArrayUtil.append(factories, factory);
        }
    }

    /**
     * Returns factory which may be used to create loaders 
     * for specified file extension. Returns <code>null</code> 
     * if such loader is not found.
     */
    public static DocumentLoaderFactory get(String extension) {
        synchronized (LOCK) {
            // Test last registered factory first.
            for (int i = factories.length-1; i >= 0; --i) {
                DocumentLoaderFactory factory = factories[i];

                if (StringList.containsIgnoreCase(factory.getExtensions(), 
                                                  extension)) {
                    return factory;
                }
            }

            return null;
        }
    }

    /**
     * Convenience method: returns loader which may be used to load the document
     * having specified URL.
     *
     * @param url URL of document to be loaded
     * @param options options parameterizing the created document loader. 
     * May be <code>null</code>.
     * @param console the console on which messages are to be displayed.
     * May be <code>null</code>.
     * @return document loader or <code>null</code> if the file extension
     * of <tt>url</tt> is not supported
     * @exception Exception if, for any reason, the document loader 
     * cannot be created
     */
    public static DocumentLoader createLoader(URL url, String[] options, 
                                              Console console) 
        throws Exception {
        String ext = URLUtil.getExtension(url);
        if (ext != null && ext.length() > 0) {
            DocumentLoaderFactory factory = get(ext);
            if (factory != null) {
                // Filter options ---

                String optionPrefix = 
                    "load." + factory.getName().toLowerCase() + ".";
                int optionPrefixLength = optionPrefix.length();

                int optionCount;
                if (options != null && (optionCount = options.length) > 0) {
                    String[] options2 = new String[optionCount];
                    int j = 0;

                    for (int i = 0; i < optionCount; i += 2) {
                        String key = options[i];

                        if (key.toLowerCase().startsWith(optionPrefix)) {
                            options2[j++] = key.substring(optionPrefixLength);
                            options2[j++] = options[i+1];
                        }
                    }

                    if (j != optionCount) {
                        if (j == 0) {
                            options = StringList.EMPTY_LIST;
                        } else {
                            options = ArrayUtil.trimToSize(options2, j);
                        }
                    } else {
                        options = options2;
                    }
                }

                return factory.createLoader(options, console);
            }
        }

        return null;
    }

    // -----------------------------------------------------------------------
    // Auto-register known factories
    // -----------------------------------------------------------------------

    static {
        String[] factoryClassNames = {
            "com.xmlmind.ditac.load.hdita.HDITALoaderFactory",
            "com.xmlmind.ditac.load.mdita.MDITALoaderFactory"
        };

        for (String factoryClassName : factoryClassNames) {
            try {
                Class<?> cls = Class.forName(factoryClassName);
                DocumentLoaderFactory factory = (DocumentLoaderFactory) 
                    cls.getDeclaredConstructor().newInstance();

                DocumentLoaderFactories.register(factory);
            } catch (Throwable t) {
                String error = 
                    "cannot load DocumentLoaderFactory '" + factoryClassName + 
                    "': " + ThrowableUtil.reason(t);
                System.err.println("ditac: ERROR: " + error);
                Log.getLog("ditac").error(error);
            }
        }
    }
}

