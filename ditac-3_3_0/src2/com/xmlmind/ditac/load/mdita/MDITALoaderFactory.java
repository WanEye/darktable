/*
 * Copyright (c) 2018 XMLmind Software. All rights reserved.
 *
 * Author: Hussein Shafie
 *
 * This file is part of the XMLmind DITA Converter project.
 * For conditions of distribution and use, see the accompanying LEGAL.txt file.
 */
package com.xmlmind.ditac.load.mdita;

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collections;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.options.DataKey;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;
import com.vladsch.flexmark.util.html.Attributes;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.LinkRef;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.CustomNodeRenderer;
import com.vladsch.flexmark.html.renderer.LinkType;
import com.vladsch.flexmark.html.renderer.LinkStatus;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.LinkResolver;
import com.vladsch.flexmark.html.IndependentLinkResolverFactory;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ext.admonition.AdmonitionBlock;
import com.vladsch.flexmark.ext.footnotes.Footnote;
import com.vladsch.flexmark.ext.footnotes.FootnoteBlock;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.xmlmind.util.ThrowableUtil;
import com.xmlmind.util.StringList;
import com.xmlmind.util.StringUtil;
import com.xmlmind.util.XMLText;
import com.xmlmind.util.Console;
import com.xmlmind.util.Log;
import com.xmlmind.ditac.preprocess.DocumentLoaderFactory;
import com.xmlmind.ditac.preprocess.ListingProcessor;

// Needed by main() ---
import java.io.File;
import java.net.URL;
import org.w3c.dom.Document;
import com.xmlmind.util.URLUtil;
import com.xmlmind.util.FileUtil;
import com.xmlmind.ditac.util.SimpleConsole;
import com.xmlmind.ditac.util.SaveDocument;

/**
 * The factory used to create MDITA document loaders.
 */
public final class MDITALoaderFactory implements DocumentLoaderFactory {
    private static final String[] EXTENSIONS = {
        "md", "markdown", "mdown", "mkdn", "mdwn", "mkd", "rmd"
    };

    private static final String[]  KEY_TO_CLASS_NAME = {
      "parser", "com.vladsch.flexmark.parser.Parser",

      "renderer", "com.vladsch.flexmark.html.HtmlRenderer",

      "abbreviation",
        "com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension",
      "admonition",
        "com.vladsch.flexmark.ext.admonition.AdmonitionExtension",
      "anchorlink",
        "com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension",
      "aside",
        "com.vladsch.flexmark.ext.aside.AsideExtension",
      "attributes",
        "com.vladsch.flexmark.ext.attributes.AttributesExtension",
      "autolink",
        "com.vladsch.flexmark.ext.autolink.AutolinkExtension",
      "definition",
        "com.vladsch.flexmark.ext.definition.DefinitionExtension",
      "emoji",
        "com.vladsch.flexmark.ext.emoji.EmojiExtension",
      "enumerated-reference",
        "com.vladsch.flexmark.ext.enumerated.reference.EnumeratedReferenceExtension",
      "footnotes",
        "com.vladsch.flexmark.ext.footnotes.FootnoteExtension",
      "gfm-issues",
        "com.vladsch.flexmark.ext.gfm.issues.GfmIssuesExtension",
      // Separate StrikethroughExtension AND SubscriptExtension
      // cause: Delimiter processor conflict with delimiter char '~'
      "gfm-strikethrough",
        "com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension",
      "gfm-tasklist",
        "com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension",
      "gfm-users",
        "com.vladsch.flexmark.ext.gfm.users.GfmUsersExtension",
      "ins",
        "com.vladsch.flexmark.ext.ins.InsExtension",
      "superscript",
        "com.vladsch.flexmark.superscript.SuperscriptExtension",
      "tables",
        "com.vladsch.flexmark.ext.tables.TablesExtension",
      "toc",
        "com.vladsch.flexmark.ext.toc.TocExtension",
      "typographic",
        "com.vladsch.flexmark.ext.typographic.TypographicExtension",
      "wikilink",
        "com.vladsch.flexmark.ext.wikilink.WikiLinkExtension",
      "yaml-front-matter",
        "com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension",
      "youtube-embedded",
        "com.vladsch.flexmark.ext.youtube.embedded.YouTubeLinkExtension",
    };
    private static final HashMap<String,Class<?>> KEY_TO_CLASS =
        new HashMap<String,Class<?>>();
    static {
        final int count = KEY_TO_CLASS_NAME.length;
        for (int i = 0; i < count; i += 2) {
            String clsName = KEY_TO_CLASS_NAME[i+1];
            try {
                KEY_TO_CLASS.put(KEY_TO_CLASS_NAME[i], Class.forName(clsName));
            } catch (Throwable shouldNotHappen) {
                String error = "cannot load class '" + clsName + "': " + 
                    ThrowableUtil.reason(shouldNotHappen);
                System.err.println("ditac: ERROR: " + error);
                Log.getLog("ditac").error(error);
            }
        }
    }

    // Corresponds to the "Markdown Cheatsheet"
    // https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet
    // plus all tested, documented, extensions (TDEx). 
    // That is, corresponds to MDITA Extended Profile.

    private static final String[] DEFAULT_PARAMS = {
        "abbreviation", "true", // TDEx
        "admonition", "true", // TDEx
        "anchorlink", "false",
        "aside", "false",
        "attributes", "true", // TDEx
        "autolink", "false",
        "definition", "true", // TDEx
        "emoji", "false",
        "enumerated-reference", "false",
        "footnotes", "true", // TDEx
        "gfm-issues", "false",
        "gfm-strikethrough", "true", // Includes subscript.
        "gfm-tasklist", "false",
        "gfm-users", "false",
        "ins", "true", // TDEx
        "superscript", "true", // TDEx
        "tables", "true",
        "toc", "false",
        "typographic", "true", // TDEx
        "wikilink", "false",
        "yaml-front-matter", "true",
        "youtube-embedded", "false",
    };

    private static final String[] FORCED_PARAMS = {
        "parser.FENCED_CODE_CONTENT_BLOCK", "false", // code contained in Text.

        "renderer.SOFT_BREAK", " ", // '\n' within <p> not useful.
    };

    private static final String[] CORE_PROFILE = {
        "gfm-strikethrough", // Includes subscript.
        "tables",
        "yaml-front-matter",
    };

    private static final String[] EXTENDED_PROFILE = {
        "abbreviation",
        "admonition",
        "attributes",
        // Not autolink because it works without explicit tagging.
        "definition",
        "footnotes",
        "gfm-strikethrough", // Includes subscript.
        "ins",
        "superscript",
        "tables",
        "typographic",
        "yaml-front-matter",
    };

    // -----------------------------------------------------------------------

    public String getName() {
        return "mdita";
    }

    public String[] getExtensions() {
        return EXTENSIONS;
    }

    public MDITALoader createLoader(String[] params, Console console) {
        MutableDataSet options = new MutableDataSet();
        HashSet<String> extensions = new HashSet<String>();

        addOptions(DEFAULT_PARAMS, extensions, options);
        if (params != null) {
            addOptions(params, extensions, options);
        }
        addOptions(FORCED_PARAMS, extensions, options);

        ArrayList<Extension> parserExts = new ArrayList<Extension>();
        ArrayList<Extension> rendererExts = new ArrayList<Extension>();
        for (String extension : extensions) {
            Extension ext = createExtension(extension);
            parserExts.add(ext);

            // As an HtmlRenderer.HtmlRendererExtension, 
            // - AdmonitionExtension is not useful and gets in the way 
            // (and is buggy).
            // - FootnoteExtension is not useful and gets in the way.

            if (!"admonition".equals(extension) &&
                !"footnotes".equals(extension)) {
                rendererExts.add(ext);
            }

        }
        options.set(Parser.EXTENSIONS, parserExts);

        Parser parser = Parser.builder(options).build();

        options.set(Parser.EXTENSIONS, rendererExts);

        HtmlRenderer htmlRenderer = HtmlRenderer.builder(options)
            .linkResolverFactory(new IndependentLinkResolverFactory() {
                public LinkResolver create(LinkResolverContext context) {
                    return new LinkResolverImpl();
                }
            })
            .attributeProviderFactory(new IndependentAttributeProviderFactory(){
                public AttributeProvider create(LinkResolverContext context) {
                    return new AttributeProviderImpl();
                }
            })
            .nodeRendererFactory(new NodeRendererFactory() {
                public NodeRenderer create(DataHolder options) {
                    return new NodeRendererImpl();
                }
            })
            .build();

        return new MDITALoader(parser, htmlRenderer);
    }

    private static void addOptions(String[] params, Set<String> extensions, 
                                   MutableDataSet options) 
        throws IllegalArgumentException {
        final int count = params.length;
        for (int i = 0; i < count; i += 2) {
            String key = params[i];
            String value = params[i+1];

            if (key.startsWith("load.mdita.")) {
                key = key.substring(11);
            }

            if (key.startsWith("parser.")) {
                String keyName = key.substring(7);
                if (keyName.length() > 0) {
                    addOption("parser", Parser.class, keyName, value, 
                              options);
                }
            } else if (key.startsWith("renderer.")) {
                String keyName = key.substring(9);
                if (keyName.length() > 0) {
                    addOption("renderer", HtmlRenderer.class, keyName, value, 
                              options);
                }
            } else if ("profile".equals(key)) {
                try {
                    ParserEmulationProfile profile =
                        ParserEmulationProfile.valueOf(value);
                    options.setFrom(profile);
                } catch (Exception ignored) {
                    throw new IllegalArgumentException(
                        "invalid markdown parameter profile=\"" + 
                        value + "\": '" + 
                        value + "', unknown parser emulation profile");
                }
            } else {
                String extension;
                String keyName;

                int pos = key.indexOf('.');
                if (pos > 0 && pos < key.length()-1) {
                    extension = key.substring(0, pos);
                    keyName = key.substring(pos+1);
                } else {
                    extension = key;
                    keyName = null;
                }

                if (keyName == null) {
                    if ("core-profile".equals(extension)) {
                        if ("true".equals(value)) {
                            extensions.clear();
                            Collections.addAll(extensions, CORE_PROFILE);
                        }
                        // false has no effect.
                    } else if ("extended-profile".equals(extension)) {
                        if ("true".equals(value)) {
                            extensions.clear();
                            Collections.addAll(extensions, EXTENDED_PROFILE);
                        }
                        // false has no effect.
                    } else {
                        if ("true".equals(value)) {
                            extensions.add(extension);
                        } else {
                            extensions.remove(extension);
                        }
                    }
                } else {
                    Class<?> extensionClass = KEY_TO_CLASS.get(extension);
                    if (extensionClass == null) {
                        throw new IllegalArgumentException(
                            "invalid markdown parameter " + 
                            key + "=\"" + value + "\": '" +
                            extension + "', unknown flexmark extension");
                    }

                    addOption(extension, extensionClass, keyName, value,
                              options);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void addOption(String extension, Class<?> extensionClass,
                                  String keyName, String keyValue, 
                                  MutableDataSet options) 
        throws IllegalArgumentException {
        try {
            Field field = extensionClass.getField(keyName);

            ParameterizedType fieldType = 
                (ParameterizedType) field.getGenericType();
            Type fieldTypeArg = fieldType.getActualTypeArguments()[0];
            String keyValueClassName = fieldTypeArg.getTypeName();

            if ("java.lang.String".equals(keyValueClassName)) {
                options.set((DataKey<String>) field.get(null), keyValue);
            } else if ("java.lang.Boolean".equals(keyValueClassName)) {
                options.set((DataKey<Boolean>) field.get(null), 
                            "true".equals(keyValue)? 
                            Boolean.TRUE : Boolean.FALSE);
            } else if ("java.lang.Integer".equals(keyValueClassName)) {
                options.set((DataKey<Integer>) field.get(null), 
                            new Integer(keyValue));
            } else {
                boolean addedEnum = false;
                Class<?> enumClass = Class.forName(keyValueClassName);
                if (enumClass.isEnum()) {
                    for (Object enumValue : enumClass.getEnumConstants()) {
                        if (enumValue.toString().equals(keyValue)) {
                            options.set((DataKey) field.get(null), enumValue);
                            addedEnum = true;
                            break;
                        }
                    }
                }

                if (!addedEnum) {
                    throw new RuntimeException(
                        "don't know how to parse \"" + keyValue + 
                        "\" as a " + keyValueClassName);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "invalid markdown parameter " + 
                extension + "." + keyName + "=\"" + keyValue + "\": " +
                ThrowableUtil.reason(e));
        }
    }

    private static Extension createExtension(String extension) 
        throws RuntimeException {
        try {
            Class<?> extensionClass = KEY_TO_CLASS.get(extension);
            if (extensionClass == null) {
                throw new RuntimeException("'" + extension + 
                                           "', unknown flexmark extension");
            }

            Method createMethod = extensionClass.getMethod("create");
            return (Extension) createMethod.invoke(null);
        } catch (Throwable shouldNotHappen) {
            throw new RuntimeException(
                "cannot create flexmark extension '" + extension + "': " + 
                ThrowableUtil.reason(shouldNotHappen));
        }
    }

    // -----------------------------------------------------------------------

    private static final class LinkResolverImpl implements LinkResolver {
        public ResolvedLink resolveLink(Node node, LinkResolverContext context,
                                        ResolvedLink link) {
            LinkType linkType = link.getLinkType();
            if (linkType == LinkType.LINK_REF ||
                linkType == LinkType.IMAGE_REF) {
                Attributes attrs = new Attributes();
                attrs.addValue("data-keyref", link.getUrl());

                link = new ResolvedLink(linkType, "", attrs, 
                                        LinkStatus.UNCHECKED);
            } else if (linkType == LinkType.LINK) {
                String url = link.getUrl();
                String url2 = url.toLowerCase();
                Attributes attrs = null;

                if (url2.matches("^[\\w]+:/.+")) {
                    attrs = new Attributes();
                    attrs.addValue("rel", "external");
                }
                
                if (url2.matches(".+\\.(html|htm|shtml|xhtml|xhtm|xht)$") ||
                    ((url2.startsWith("http://") || 
                      url2.startsWith("https://")) && url2.endsWith("/"))) {
                    if (attrs == null) {
                        attrs = new Attributes();
                    }
                    attrs.addValue("type", "html");
                } else if (url2.endsWith(".pdf")) {
                    if (attrs == null) {
                        attrs = new Attributes();
                    }
                    attrs.addValue("type", "pdf");
                } else if (url2.endsWith(".txt")) {
                    if (attrs == null) {
                        attrs = new Attributes();
                    }
                    attrs.addValue("type", "txt");
                }

                if (attrs != null) {
                    link = new ResolvedLink(linkType, url, attrs, 
                                            LinkStatus.UNCHECKED);
                }
            }

            return link;
        }
    }

    // -----------------------------------------------------------------------

    private static final class AttributeProviderImpl
                         implements AttributeProvider {
        public void setAttributes(Node node, AttributablePart part, 
                                  Attributes attributes) {
            if (node instanceof TableBlock) {
                // We generate real tables, hence having a border.
                attributes.replaceValue("border", "1");
            }
        }
    }

    // -----------------------------------------------------------------------

    private static final class NodeRendererImpl implements NodeRenderer {
        public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
            HashSet<NodeRenderingHandler<?>> handlers = 
                new HashSet<NodeRenderingHandler<?>>();
            handlers.add(new IndentedCodeBlockHandler());
            handlers.add(new FencedCodeBlockHandler());
            handlers.add(new AdmonitionBlockHandler());
            handlers.add(new FootnoteHandler());
            handlers.add(new FootnoteBlockHandler());
            return handlers;
        }
    }

    private static final class IndentedCodeBlockHandler
                         extends NodeRenderingHandler<IndentedCodeBlock> {
        public IndentedCodeBlockHandler() {
            super(IndentedCodeBlock.class, new IndentedCodeBlockRenderer());
        }
    }

    private static final class IndentedCodeBlockRenderer 
                         implements CustomNodeRenderer<IndentedCodeBlock> {
        public void render(IndentedCodeBlock node, NodeRendererContext context,
                           HtmlWriter html) {
            // Generate just <pre>; not <pre><code>.

            html.line();

            html.withAttr().srcPosWithEOL(node.getChars());

            html.tag("pre").openPre();
            html.text(node.getContentChars()
                          .trimTailBlankLines().normalizeEndWithEOL());
            html.tag("/pre").closePre();

            html.lineIf(context.getHtmlOptions().htmlBlockCloseTagEol);
        }
    }

    private static final class FencedCodeBlockHandler
                         extends NodeRenderingHandler<FencedCodeBlock> {
        public FencedCodeBlockHandler() {
            super(FencedCodeBlock.class, new FencedCodeBlockRenderer());
        }
    }

    private static final class FencedCodeBlockRenderer 
                         implements CustomNodeRenderer<FencedCodeBlock> {
        public void render(FencedCodeBlock node, NodeRendererContext context, 
                           HtmlWriter html) {
            // Generate something like <pre class="language-cpp line-numbers">.

            ArrayList<String> cls = new ArrayList<String>();

            String info = null;
            BasedSequence infoChars = node.getInfo();
            if (infoChars != null) {
                info = infoChars.toString();
            }
            if (info != null && (info = info.trim()).length() > 0) {
                String numbered = null;

                if (info.endsWith("}")) {
                    int pos = info.lastIndexOf('{');
                    if (pos >= 0) {
                        String[] split = StringUtil.split(
                            info.substring(pos+1, info.length()-1));
                        info = info.substring(0, pos).trim();

                        for (String segment : split) {
                            if (segment.startsWith(".")) {
                                segment = segment.substring(1);
                                if (segment.length() > 0) {
                                    if ("line-numbers".equals(segment)) {
                                        numbered = "line-numbers";
                                    } else {
                                        cls.add(segment);
                                    }
                                }
                            }
                        }
                    }
                }

                if (numbered != null) {
                    cls.add(0, numbered);
                }

                // ---

                String lang = null;

                int pos = info.indexOf(' ');
                if (pos > 0) {
                    info = info.substring(0, pos);
                }
                info = ListingProcessor.checkHLCode(info);
                if (info != null) {
                    lang = "language-" + info;
                }

                if (lang != null) {
                    cls.add(0, lang);
                }
            }

            // ---

            html.line();

            html.withAttr().srcPosWithTrailingEOL(node.getChars());

            if (cls.size() > 0) {
                html.attr("class", String.join(" ", cls));
            }

            html.tag("pre").openPre();
            html.text(node.getContentChars().normalizeEOL());
            html.tag("/pre").closePre();

            html.lineIf(context.getHtmlOptions().htmlBlockCloseTagEol);
        }
    }

    private static final class AdmonitionBlockHandler
                         extends NodeRenderingHandler<AdmonitionBlock> {
        public AdmonitionBlockHandler() {
            super(AdmonitionBlock.class, new AdmonitionBlockRenderer());
        }
    }

    private static final class AdmonitionBlockRenderer 
                         implements CustomNodeRenderer<AdmonitionBlock> {
        private static final String[] ADMONITION_TYPES = {
            "note",
            "attention",
            "caution",
            "danger",
            "fastpath",
            "important",
            "notice",
            "remember",
            "restriction",
            "tip",
            "trouble",
            "warning"
        };

        public void render(AdmonitionBlock node, NodeRendererContext context,
                           HtmlWriter html) {
            String type = null;
            BasedSequence typeChars = node.getInfo();
            if (typeChars != null) {
                type = typeChars.toString().toLowerCase();
                if (!StringList.contains(ADMONITION_TYPES, type)) {
                    type = null;
                }
            }

            String title = null;
            BasedSequence titleChars = node.getTitle();
            if (titleChars != null) {
                title = XMLText.collapseWhiteSpace(titleChars.toString());
            }

            html.withAttr().srcPos(node.getChars());
            html.attr("data-class", "note");
            if (type != null && !"note".equals(type)) {
                html.attr("data-type", type);
            }
            html.tag("div").line();

            if (title != null && title.length() > 0) {
                html.withAttr().srcPos(node.getChars());
                html.attr("class", "note-title");
                html.tag("h4");
                html.text(title);
                html.tag("/h4").line();
            }

            context.renderChildren(node);

            html.tag("/div").line();
        }
    }

    private static final class FootnoteHandler
                         extends NodeRenderingHandler<Footnote> {
        public FootnoteHandler() {
            super(Footnote.class, new FootnoteRenderer());
        }
    }

    private static final class FootnoteRenderer 
                         implements CustomNodeRenderer<Footnote> {
        public void render(Footnote node, NodeRendererContext context,
                           HtmlWriter html) {
            html.withAttr().srcPos(node.getChars());
            html.attr("href", "#" + toFootnoteId(node.getText()));
            html.tag("a");
            html.tag("/a");
        }

        public static String toFootnoteId(BasedSequence text) {
            char[] chars = new char[0];
            if (text != null) {
                chars = 
                    XMLText.collapseWhiteSpace(text.toString()).toCharArray();
            }

            StringBuilder buffer = new StringBuilder();

            char lastChar = '\0';
            final int count = chars.length;
            for (int i = 0; i < count; ++i) {
                char c = chars[i];

                if (XMLText.isNCNameChar(c)) {
                    buffer.append(c);
                    lastChar = c;
                } else {
                    if (Character.isWhitespace(c)) {
                        if (lastChar != '_') {
                            buffer.append('_');
                            lastChar = '_';
                        }
                    } else {
                        buffer.append("0x");
                        buffer.append(Integer.toHexString(c).toUpperCase());
                        lastChar = c;
                    }
                }
            }

            buffer.insert(0, "__FN");
            return buffer.toString();
        }
    }

    private static final class FootnoteBlockHandler
                         extends NodeRenderingHandler<FootnoteBlock> {
        public FootnoteBlockHandler() {
            super(FootnoteBlock.class, new FootnoteBlockRenderer());
        }
    }

    private static final class FootnoteBlockRenderer 
                         implements CustomNodeRenderer<FootnoteBlock> {
        public void render(FootnoteBlock node, NodeRendererContext context,
                           HtmlWriter html) {
            html.withAttr().srcPos(node.getChars());
            html.attr("data-class", "fn");
            html.attr("id", FootnoteRenderer.toFootnoteId(node.getText()));
            html.tag("div");

            Node childNode = node.getFirstChild();
            if (childNode != null && (childNode instanceof Paragraph)) {
                // Make most common case nicer.
                context.renderChildren(childNode);

                childNode = childNode.getNext();
                while (childNode != null) {
                    context.render(childNode);
                    childNode = childNode.getNext();
                }
            } else {
                html.line();
                context.renderChildren(node);
            }

            html.tag("/div").line();
        }
    }

    // -----------------------------------------------------------------------

    public static void main(String[] args) 
        throws Exception {
        String[] params = StringList.EMPTY_LIST;
        boolean generateHTML = false;
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
            } else if ("-html".equals(arg)) {
                generateHTML = true;
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

        MDITALoaderFactory factory = new MDITALoaderFactory();
        Console console = new SimpleConsole();
        MDITALoader loader = factory.createLoader(params, console);

        if (generateHTML) {
            String html = loader.loadHTML(inURL);
            FileUtil.saveString(html, outFile, "UTF-8");
        } else {
            Document doc = loader.load(inURL, validate, console);
            SaveDocument.save(doc, outFile);
        }
    }

    private static void usage() {
        System.err.println(
            "Usage: java com.xmlmind.ditac.load.mdita.MDITALoaderFactory" +
            " [-p param_name param_value ]*\n" +
            "    -html in_md_URL_or_file out_xhtml_file\n" +
            "    | [-invalid] in_md_URL_or_file out_dita_file");
        System.exit(1);
    }
}
