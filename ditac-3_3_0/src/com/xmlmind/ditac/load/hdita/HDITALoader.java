/*
 * Copyright (c) 2018 XMLmind Software. All rights reserved.
 *
 * Author: Hussein Shafie
 *
 * This file is part of the XMLmind DITA Converter project.
 * For conditions of distribution and use, see the accompanying LEGAL.txt file.
 */
package com.xmlmind.ditac.load.hdita;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import com.xmlmind.util.URLUtil;
import com.xmlmind.util.XMLUtil;
import com.xmlmind.util.Console;
import com.xmlmind.ditac.util.DOMUtil;
import com.xmlmind.ditac.util.ConsoleErrorListener;
import com.xmlmind.ditac.util.Resolve;
import com.xmlmind.ditac.util.NodeLocation;
import com.xmlmind.ditac.util.LoadDocument;
import com.xmlmind.ditac.preprocess.DocumentLoader;

/**
 * The HDITA document loader.
 */
public final class HDITALoader implements DocumentLoader {
    private final Transformer transformer;

    // -----------------------------------------------------------------------

    /*package*/ HDITALoader(Transformer transformer) {
        this.transformer = transformer;
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
        // For use by document().
        transformer.setURIResolver(Resolve.createURIResolver());

        transformer.setErrorListener(new ConsoleErrorListener(console));

        XMLReader xmlReader = XMLUtil.newSAXParser().getXMLReader();
        // Ignore XHTML DTD if any.
        xmlReader.setEntityResolver(EntityResolverImpl.INSTANCE);

        InputSource inputSource = new InputSource(in);
        String systemId = url.toExternalForm();
        inputSource.setSystemId(systemId);
        SAXSource saxSource = new SAXSource(xmlReader, inputSource);

        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(byteArray);
        URL urlForErrorMessages = URLUtil.setExtension(url, "dita");
        result.setSystemId(urlForErrorMessages.toExternalForm());
        
        transformer.transform(saxSource, result);

        // No need to close a ByteArrayOutputStream.
        
        // ---

        byte[] bytes = byteArray.toByteArray();
        byteArray = null;

        boolean addElementPointer = LoadDocument.isAddingElementPointer();
        LoadDocument.setAddingElementPointer(false);

        Document doc;
        try {
            doc = LoadDocument.load(new ByteArrayInputStream(bytes),
                                    urlForErrorMessages, validate, console);
        } finally {
            LoadDocument.setAddingElementPointer(addElementPointer);
        }

        // No need to close a ByteArrayInputStream.

        discardNodeLocations(doc.getDocumentElement());

        doc.setDocumentURI(systemId); // Real ".html" URL.

        return doc;
    }

    private static void discardNodeLocations(Element tree) {
        NodeLocation location = 
            (NodeLocation) tree.getUserData(NodeLocation.USER_DATA_KEY);
        if (location != null) {
            tree.setUserData(NodeLocation.USER_DATA_KEY, null,
                             DOMUtil.COPY_USER_DATA);
        }

        Node node = tree.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                discardNodeLocations((Element) node);
            }

            node = node.getNextSibling();
        }
    }
}
