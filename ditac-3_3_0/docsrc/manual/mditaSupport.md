---
id: mditaSupport
---

# MDITA support

XMLmind DITA Converter fully supports 
[MDITA](http://docs.oasis-open.org/dita/LwDITA/v1.0/cn01/LwDITA-v1.0-cn01.html#what-is-mdita), 
which specifies how to write DITA topics and maps in 
[Markdown](https://daringfireball.net/projects/markdown/).

Template of an MDITA topic
[lwdita_templates/mdita_topic.md](lwdita_templates/mdita_topic.md){rel="external" type="text"}:

```
---
id: ???
---

# Topic title here

Short description here.

Topic body starts here.
```

Template of an MDITA map 
[lwdita_templates/mdita_map.md](lwdita_templates/mdita_map.md){rel="external" type="text"}:

```
# Map title here {.map}
   
- [???](???)
  - [???](???)
  - [???](???)
- [???](???)
```

Notice the `{.map}` `class` attribute added to the title of the map. 
Without it, the above template would be translated to a DITA `topic`.

## Implementation specificities

- Adding a `{.concept}` `class` attribute to the title of an MDITA topic may
  be used to generate a DITA concept rather than a DITA topic.

- Out of the box, ditac supports the so-called *Extended Profile*.

  This Extended Profile may be customized by the means of 
  [`-p load.mdita.XXX` parameters](commandLine.dita#./load_params). 
  These `load.mdita.XXX` parameters are documented [below](#load_params).

## Limitations

- Without a `{.map}` `class` attribute added to the title of an MDITA map,
  this map is confused with a topic.

## `load.mdita.XXX` parameters

Parameter `-p load.mdita.extended-profile true` is implicitely passed 
to `ditac`. This parameter is simply a shorthand for:
{#load_params}

```
-p load.mdita.abbreviation true
-p load.mdita.admonition true
-p load.mdita.attributes true
-p load.mdita.definition true
-p load.mdita.footnotes true
-p load.mdita.gfm-strikethrough true
-p load.mdita.ins true
-p load.mdita.superscript true
-p load.mdita.tables true
-p load.mdita.typographic true
-p load.mdita.yaml-front-matter true
```

where abbreviation, admonition, attributes, etc, are all 
*Markdown extensions*, documented in 
[Markdown extensions](markdownExtensions.md).

If for example, you don't like the stock Extended Profile and prefer to 
use a simpler one, plus the `autolink` Markdown extension[^1], then pass:

```
-p load.mdita.core-profile true
-p load.mdita.autolink true
```

to `ditac`.

Parameter `-p load.mdita.core-profile true` is simply a shorthand for:

```
-p load.mdita.gfm-strikethrough true
-p load.mdita.tables true
-p load.mdita.yaml-front-matter true
```
  
[^1]: Turns plain text URLs and email addresses into `<xref href="...">` 
      elements.
