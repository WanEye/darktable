/*
 * Copyright (c) 2017-2018 XMLmind Software. All rights reserved.
 *
 * Author: Hussein Shafie
 *
 * This file is part of the XMLmind DITA Converter project.
 * For conditions of distribution and use, see the accompanying LEGAL.txt file.
 */
package com.xmlmind.ditac.load.hdita;

import java.io.IOException;
import java.io.StringReader;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import com.xmlmind.util.FileUtil;

/**
 * <b>Not part of the public documented API:</b> an entity resolver 
 * which substitutes any public XHTML DTD by just the definitions of 
 * all HTML character entities (e.g. <code>&amp;nbsp;</code>).
 */
public final class EntityResolverImpl implements EntityResolver {
    public static final EntityResolverImpl INSTANCE =
        new EntityResolverImpl(null);

    private final EntityResolver delegate;

    private final static String[] fakeDTD = new String[1];

    // -----------------------------------------------------------------------

    public EntityResolverImpl(EntityResolver delegate) {
        this.delegate = delegate;
    }

    public InputSource resolveEntity(String publicId, String systemId) 
        throws IOException, SAXException {
        InputSource resolved = null;
        if (delegate != null) {
            resolved = delegate.resolveEntity(publicId, systemId);

            /*
            System.err.println("publicId='" + publicId + 
                               "', systemId='" + systemId + 
                               "' --> " + resolved);
            */
        } 

        if (resolved == null &&
            publicId != null && publicId.startsWith("-//W3C//DTD XHTML")) {
            String dtd = loadFakeDTD();
            if (dtd != null) {
                resolved = new InputSource(new StringReader(dtd));
                resolved.setSystemId(systemId);
            }
        }

        return resolved;
    }

    private static String loadFakeDTD() {
        synchronized (fakeDTD) {
            if (fakeDTD[0] == null) {
                try {
                    fakeDTD[0] = FileUtil.loadString(
                        EntityResolverImpl.class.getResourceAsStream(
                            "fake-xhtml.dtd"),
                        "UTF-8");
                } catch (IOException cannotHappen) {}
            }

            return fakeDTD[0];
        }
    }
}
