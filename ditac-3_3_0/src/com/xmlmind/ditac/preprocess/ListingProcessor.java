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
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import com.xmlmind.util.ThrowableUtil;
import com.xmlmind.util.ObjectUtil;
import com.xmlmind.util.StringList;
import com.xmlmind.util.URLUtil;
import com.xmlmind.util.XMLText;
import com.xmlmind.util.XMLUtil;
import com.xmlmind.ditac.util.DITAUtil;
import com.xmlmind.ditac.util.ConsoleHelper;

/**
 * <b>Not part of the public documented API</b>.
 */
public final class ListingProcessor extends ListingCleaner {
    private ListingProcessor() {}

    public static void processTopic(Element topic, ConsoleHelper console) {
        Document doc = topic.getOwnerDocument();
        int[] info = new int[2];
        process(topic, info, doc, console);
    }

    private static void process(Element element, int[] info, Document doc, 
                                ConsoleHelper console) {
        Node node = element.getFirstChild();
        while (node != null) {
            Node next = node.getNextSibling();

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;

                if (DITAUtil.hasClass(child, "topic/topic")) {
                    // Do not process nested topics.
                    return;
                }

                if (DITAUtil.hasClass(child, "topic/pre")) {
                    String outputclass =
                      DITAUtil.getNonEmptyAttribute(child, null, "outputclass");
                    if (outputclass != null) {
                        String outputclass2 = 
                            parseOutputclass(outputclass, info);
                        int firstLine = info[0];
                        int tabWidth = info[1];

                        if (!ObjectUtil.equals(outputclass2, outputclass)) {
                            if (outputclass2 == null) {
                                child.removeAttributeNS(null, "outputclass");
                            } else {
                                child.setAttributeNS(null, "outputclass",
                                                     outputclass2);
                            }
                        }

                        if (tabWidth > 0 || firstLine >= 1) {
                            includeCoderefs(child, doc, console);
                        }

                        if (tabWidth > 0) {
                            normalizeWhiteSpace(child, tabWidth, 
                                                /*unindent*/ true);
                        }

                        if (firstLine >= 1) {
                            element.removeChild(child);

                            Element numbered = 
                                numberListing(child, firstLine, doc);

                            element.insertBefore(numbered, next);
                        }
                    }
                } else {
                    process(child, info, doc, console);
                }
            }
            
            node = next;
        }
    }

    // ----------------------------------
    // parseOutputclass
    // ----------------------------------

    private static String parseOutputclass(String value, int[] info) {
        info[0] = info[1] = -1;

        StringBuilder buffer = new StringBuilder();
        String language = null;

        String[] classes = XMLText.splitList(value);
        for (String cls : classes) {
            if (cls.startsWith("language-")) {
                String hlCode = checkHLCode(cls.substring(9));
                if (hlCode != null) {
                    language = hlCode;
                }
            } else if ("line-numbers".equals(cls) ||
                       "show-line-numbers".equals(cls)) { // DITA-OT compat.
                info[0] = 1;
            } else if (cls.startsWith("line-numbers-")) {
                String spec = cls.substring(13);
                try {
                    int i = Integer.parseInt(spec);
                    if (i >= 1) {
                        info[0] = i;
                    }
                } catch (NumberFormatException ignored) {}
            } else if ("normalize-space".equals(cls)) { // DITA-OT compat.
                info[1] = 8;
            } else if (cls.startsWith("tab-width-")) {
                String spec = cls.substring(10);
                try {
                    int j = Integer.parseInt(spec);
                    if (j >= 0) { // 0 means: do not expand tabs.
                        info[1] = j;
                    }
                } catch (NumberFormatException ignored) {}
            } else {
                if (buffer.length() > 0) {
                    buffer.append(' ');
                }
                buffer.append(cls);
            }
        }

        if (info[1] < 0 && (language != null || info[0] >= 1)) {
            info[1] = 8; // Implicit tab-width-8
        }

        if (language != null) {
            if (buffer.length() > 0) {
                buffer.insert(0, ' ');
            }
            buffer.insert(0, "language-" + language);
        }

        return (buffer.length() == 0)? null : buffer.toString();
    }

    // ----------------------------------
    // includeCoderefs
    // ----------------------------------

    private static void includeCoderefs(Element pre, Document doc, 
                                        ConsoleHelper console) {
        String[] detectedEncoding = new String[1];

        Node node = pre.getFirstChild();
        while (node != null) {
            Node next = node.getNextSibling();

            if (node.getNodeType() == Node.ELEMENT_NODE &&
                DITAUtil.hasClass((Element) node, "pr-d/coderef")) {
                Element coderef = (Element) node;

                String href = 
                    DITAUtil.getNonEmptyAttribute(coderef, null, "href");
                if (href != null) { // href already made absolute.
                    String charset = null;
                    String format = 
                        DITAUtil.getNonEmptyAttribute(coderef, null, "format");
                    int pos;
                    if (format != null && 
                        (pos = format.indexOf("charset=")) >= 0) {
                        charset = format.substring(pos+8);
                    }

                    try {
                        URL url = URLUtil.createURL(href);

                        String text;
                        if (charset != null) {
                            text = URLUtil.loadString(url, charset);
                        } else {
                            text = XMLUtil.loadText(url, detectedEncoding);
                            //System.err.println(detectedEncoding[0]);
                        }

                        pre.insertBefore(doc.createTextNode(text), coderef);
                    } catch (Exception e) {
                        console.error(Msg.msg("cannotIncludeCoderef", 
                                              href, ThrowableUtil.reason(e)));
                    }
                }

                // In all cases, coderef is now useless.
                pre.removeChild(coderef);
            }

            node = next;
        }
    }

    // ----------------------------------
    // numberListing
    // ----------------------------------

    private static Element numberListing(Element pre, int firstLine,
                                         Document doc) {
        int lineCount = countLines(pre);

        Element table = createElement("table", doc);
        table.setAttributeNS(null, "outputclass", "listing-layout");

        Element tgroup = createElement("tgroup", doc);
        tgroup.setAttributeNS(null, "cols", "2");
        tgroup.setAttributeNS(null, "outputclass", "listing-table");
        table.appendChild(tgroup);

        Element colspec = createElement("colspec", doc);
        colspec.setAttributeNS(null, "outputclass", "listing-numbers-column");

        int rounded = firstLine + lineCount - 1;
        if (rounded <= 10) {
            rounded = round(rounded, 10);
        } else if (rounded <= 100) {
            rounded = round(rounded, 100);
        } else if (rounded <= 1000) {
            rounded = round(rounded, 1000);
        } else if (rounded <= 10000) {
            rounded = round(rounded, 10000);
        }
        double width = Integer.toString(rounded).length();
        width *= 0.75;
        if (width < 2) {
            width = 2;
        }
        // "em" invalid unit.
        colspec.setAttributeNS(null, "colwidth", Double.toString(width) + "em");
        tgroup.appendChild(colspec);

        Element tbody = createElement("tbody", doc);
        tbody.setAttributeNS(null, "outputclass", "listing-table-body");
        tgroup.appendChild(tbody);

        Element row = createElement("row", doc);
        row.setAttributeNS(null, "outputclass", "listing-row");
        tbody.appendChild(row);

        Element entry = createElement("entry", doc);
        entry.setAttributeNS(null, "outputclass", "listing-numbers-cell");
        row.appendChild(entry);

        Element pre2 = doc.createElementNS(null, pre.getLocalName());
        String cls = pre.getAttributeNS(null, "class"); // Do not trim.
        if (cls.length() > 0) {
            pre2.setAttributeNS(null, "class", cls);
        }
        pre2.setAttributeNS(null, "outputclass", "listing-numbers");
        entry.appendChild(pre2);

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < lineCount; ++i) {
            if (i > 0) {
                buffer.append('\n');
            }
            buffer.append(Integer.toString(firstLine + i));
        }
        pre2.appendChild(doc.createTextNode(buffer.toString()));

        entry = createElement("entry", doc);
        entry.setAttributeNS(null, "outputclass", "listing-lines-cell");
        row.appendChild(entry);

        String outputclass = pre.getAttributeNS(null, "outputclass");
        if (cls.length() > 0) {
            outputclass += " listing-lines";
        } else {
            outputclass = "listing-lines";
        }
        pre.setAttributeNS(null, "outputclass", outputclass);
        entry.appendChild(pre);

        return table;
    }

    private static Element createElement(String name, Document doc) {
        Element element = doc.createElementNS(null, name);
        element.setAttributeNS(null, "class", "- topic/" + name + " ");

        return element;
    }

    private static int countLines(Element pre) {
        int count = 0;

        char[] chars = pre.getTextContent().toCharArray();
        for (char c : chars) {
            if (c == '\n') {
                ++count;
            }
        }

        return count+1;
    }

    private static int round(int num, int max) {
        return max * ((num + max - 1) / max);
    }
}
