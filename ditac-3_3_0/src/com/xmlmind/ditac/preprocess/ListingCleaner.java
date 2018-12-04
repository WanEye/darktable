/*
 * Copyright (c) 2018 XMLmind Software. All rights reserved.
 *
 * Author: Hussein Shafie
 *
 * This file is part of the XMLmind DITA Converter project.
 * For conditions of distribution and use, see the accompanying LEGAL.txt file.
 */
package com.xmlmind.ditac.preprocess;

import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.Element;
import com.xmlmind.util.StringList;

/*package*/ class ListingCleaner {
    protected ListingCleaner() {}

    // ----------------------------------
    // checkHLCode
    // ----------------------------------

    private static final String[][] HL_CODE_LISTS = {
        // Canonical name, alias, alias, ...
        { "bourne", "shell", "sh" },
        { "c" },
        { "cmake", "make", "makefile" },
        { "cpp" },
        { "csharp", "c#" },
        { "css21", "css" },
        { "delphi" },
        { "ini" },
        { "java" },
        { "javascript" },
        { "lua" },
        { "m2" },
        { "perl" },
        { "php" },
        { "python" },
        { "ruby" },
        { "sql1999" },
        { "sql2003" },
        { "sql92", "sql" },
        { "tcl" },
        { "upc" },
        { "html" },
        { "xml" }
    };

    /**
     * Returns checked highlighter code. For example, returns "<tt>css21</tt>"
     * for "<tt>CSS</tt>" and returns <code>null</code> for "<tt>foo</tt>".
     */
    public static String checkHLCode(String hlCode) {
        for (String[] hdCodeList : HL_CODE_LISTS) {
            if (StringList.containsIgnoreCase(hdCodeList, hlCode)) {
                return hdCodeList[0];
            }
        }

        return null;
    }

    // ----------------------------------
    // normalizeWhiteSpace
    // ----------------------------------

    private static final class TextChars {
        public final Text node;
        public char[] chars; // May be empty.
        public boolean modified;

        public TextChars(Text node) {
            this.node = node;
            chars = node.getData().toCharArray();
        }
    }

    protected static void normalizeWhiteSpace(Element tree, int tabWidth, 
                                              boolean unindent) {
        ArrayList<TextChars> list = new ArrayList<TextChars>();
        collectTextChars(tree, list);

        if (tabWidth > 0) {
            expandTabs(list, tabWidth);
        }

        if (unindent) {
            unindent(list);
        }

        for (TextChars textChars : list) {
            if (textChars.modified) {
                textChars.node.setData(new String(textChars.chars));
            }
        }
    }

    private static void collectTextChars(Element tree, List<TextChars> list) {
        Node node = tree.getFirstChild();
        while (node != null) {
            switch (node.getNodeType()) {
            case Node.TEXT_NODE:
                list.add(new TextChars((Text) node));
                break;
            case Node.ELEMENT_NODE:
                collectTextChars((Element) node, list);
                break;
            }

            node = node.getNextSibling();
        }
    }

    private static void expandTabs(List<TextChars> list, int tabWidth) {
        int[] lineLength = new int[1];
        for (TextChars textChars : list) {
            expandTabs(textChars, tabWidth, lineLength);
        }
    }

    @SuppressWarnings("fallthrough")
    private static void expandTabs(TextChars textChars, int tabWidth,
                                   int[] lineLength) {
        StringBuilder buffer = new StringBuilder();
        boolean expanded = false;

        final char[] chars = textChars.chars;
        final int charCount = chars.length;
        for (int i = 0; i < charCount; ++i) {
            char c = chars[i];

            switch (c) {
            case '\t':
                {
                    int spaceCount = tabWidth - (lineLength[0] % tabWidth);
                    lineLength[0] += spaceCount;

                    while (spaceCount > 0) {
                        buffer.append(' ');
                        --spaceCount;
                    }

                    expanded = true;
                }
                break;

            case '\n':
                buffer.append(c);
                lineLength[0] = 0;
                break;

            case '\r':
                if (i+1 < charCount && chars[i+1] == '\n') {
                    // Convert "\r\n" to "\n".
                    break;
                }
                /*FALLTHROUGH*/
            default:
                buffer.append(c);
                ++lineLength[0];
                break;
            }
        }

        if (expanded) {
            textChars.chars = buffer.toString().toCharArray();
            textChars.modified = true;
        }
    }

    private static void unindent(List<TextChars> list) {
        int current = -1;
        int[] indentation = new int[] { -1 };
        for (TextChars textChars : list) {
            current = indentation(textChars, current, indentation);
        }

        if (current != -1) {
            if (current < -1) {
                // Last line only contains space chars.
                current = -current - 1;
            }

            if (indentation[0] < 0 || // No value yet.
                current < indentation[0]) {
                indentation[0] = current;
            }
        }

        // ---

        int unindent = indentation[0];
        if (unindent > 0) {
            int remain = unindent;
            for (TextChars textChars : list) {
                remain = unindent(textChars, unindent, remain);
            }
        }
    }

    private static int indentation(TextChars textChars, int current, 
                                   int[] indentation) {
        final char[] chars = textChars.chars;
        final int charCount = chars.length;
        for (int i = 0; i < charCount; ++i) {
            char c = chars[i];

            switch (c) {
            case ' ':
                if (current <= -1) {
                    --current;
                }
                break;

            case '\n':
                if (current != -1) {
                    if (current < -1) {
                        // This line only contains space chars.
                        current = -current - 1;
                    }

                    if (indentation[0] < 0 || // No value yet.
                        current < indentation[0]) {
                        indentation[0] = current;
                    }

                    current = -1;
                }
                // Otherwise, ignore open line.
                break;

            default:
                if (current <= -1) {
                    // Not a space char anymore. 
                    // Prevent current from decreasing.
                    current = -current - 1;
                }
                break;
            }
        }

        return current;
    }

    private static int unindent(TextChars textChars, int unindent, int remain) {
        StringBuilder buffer = new StringBuilder();
        boolean unindented = false;

        final char[] chars = textChars.chars;
        final int charCount = chars.length;
        for (int i = 0; i < charCount; ++i) {
            char c = chars[i];

            switch (c) {
            case ' ':
                if (remain > 0) {
                    --remain;
                    unindented = true;
                } else {
                    buffer.append(c);
                }
                break;

            case '\n':
                // Space chars before a newline char are useless.
                for (int j = buffer.length()-1; j >= 0; --j) {
                    if (buffer.charAt(j) == ' ') {
                        buffer.deleteCharAt(j);
                    } else {
                        break;
                    }
                }
                buffer.append(c);
                remain = unindent;
                break;

            default:
                buffer.append(c);
                // Not a space char anymore. 
                remain = 0;
                break;
            }
        }

        if (unindented) {
            textChars.chars = buffer.toString().toCharArray();
            textChars.modified = true;
        }

        return remain;
    }
}
