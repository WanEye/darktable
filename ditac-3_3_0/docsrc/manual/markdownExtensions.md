<!-- -*- coding: utf-8 -*- -->

Markdown extensions
===================

Abbreviations
-------------

Converts plain text abbreviations (e.g. IBM) to `<abbr>` elements.
{#ext-abbreviation}

This Markdown syntax extension, which is part of the MDITA Extended Profile, 
is enabled by default. In order to disable it, pass parameter 
`-p load.mdita.abbreviation false` to `ditac`.

Example:

```markdown
The HTML specification is maintained by the W3C.

*[HTML]: Hyper Text Markup Language
*[W3C]: World Wide Web Consortium
```

is converted to:

```xml
<p>The <keyword>HTML</keyword> specification is maintained 
by  the <keyword>W3C</keyword>.</p>
```

which is rendered as:

The HTML specification is maintained by the W3C.

*[HTML]: Hyper Text Markup Language
*[W3C]: World Wide Web Consortium

Admonitions
-----------

Syntax for creating admonitions such as notes, tips, warnings, etc.
{#ext-admonition}

This Markdown syntax extension, which is part of the MDITA Extended Profile, 
is enabled by default. In order to disable it, pass parameter
`-p load.mdita.admonition false` to `ditac`.

After the \"`!!!`\" tag, the admonition type must be one of \"`note`\",
\"`attention`\",\"`caution`\", \"`danger`\", \"`fastpath`\", \"`important`\",
\"`notice`\", \"`remember`\", \"`restriction`\", \"`tip`\",\"`trouble`\",
\"`warning`\".

A note example not having a title:

```markdown
!!! note ""
    Support is limited to bug reports.
```

is converted to:

```xml
<note>
  <p>Support is limited to bug reports.</p>
</note>
```

which is rendered as:

!!! note ""
    Support is limited to bug reports.

A tip example having a title:

```markdown
!!! tip "How do you do a hard reboot on an iPad?"
    Press and hold both the **Home** and **Power** buttons 
    until your iPad&reg; reboots.

    You can release both buttons when you see Apple&reg; logo.
```

is converted to:

```xml
<note type="tip">
  <div outputclass="note-title role-h4">How do you
  do a hard reboot on an iPad?</div>

  <p>Press and hold both the <b>Home</b> and <b>Power</b> 
  buttons until your iPad® reboots.</p>

  <p>You can release both buttons when you see 
  Apple® logo.</p>
</note>
```

which is rendered as:

!!! tip "How do you do a hard reboot on an iPad?"
    Press and hold both the **Home** and **Power** buttons 
    until your iPad&reg; reboots. 

    You can release both buttons when you see Apple&reg; logo.

Attributes 
----------

Syntax for adding attributes to the generated HTML elements:
{#ext-attributes}

```
attributes -> '{' attribute_spec ( S attribute_spec)* '}'

attribute_spec ->   name=value
                  | name='value'
                  | name="value"
                  | #id
                  |.class

```

!!! remember "An easy rule to remember"
   If an `{...}` specification is separated by space characters from
   some plain text (e.g. `some plain text {...}`) then the attributes
   are added to the parent element of the text.

This Markdown syntax extension, which is part of the MDITA Extended Profile, 
is enabled by default. In order to disable it, pass parameter 
`-p load.mdita.attributes false` to `ditac`.

Example:

```markdown
The *circumference { .first-term }* is the length of one circuit along the
circle, or the distance around the circle. {#circumference}
```

is converted to:

```xml
<p id="circumference">The <i outputclass="first-term">circumference</i> 
is the length of one circuit along the circle, or the distance around 
the circle.</p>
```

which is rendered as:

The *circumference { .first-term }* is the length of one circuit along the
circle, or the distance around the circle. {#circumference title="See
https://en.wikipedia.org/wiki/Circle"}

!!! attention "Pitfall"
    By default, heading IDs are not "rendered" in HTML 
    (which is somewhat counterintuitive). You must pass
    `-p load.mdita.renderer.RENDER_HEADER_ID true`
    to `ditac` get them "rendered".

Automatic links 
---------------

Turns plain text URLs and email addresses into `<a href="...">` elements.
{#ext-autolink}

This Markdown syntax extension is disabled by default. In order to enable it, 
pass parameter `-p load.mdita.autolink true` to `ditac`.

Example:

```markdown
Please send your bug reports to support@xmlmind.com, a public,
moderated, mailing list. More information in https://xmlmind.com/.
```

is converted to:

```xml
<p>Please send your bug reports to <xref 
href="mailto:support@xmlmind.com">support@xmlmind.com</xref>,
a public, moderated, mailing list. More information in <xref
href="https://xmlmind.com/">https://xmlmind.com/</xref>.</p>
```

which is rendered as:

Please send your bug reports to support@xmlmind.com, a public,
moderated, mailing list. More information in https://www.xmlmind.com/.

Definition lists 
----------------

Syntax for creating definition lists, that is `<dl>`, `<dt>` and `<dd>`
elements.
{#ext-definition}

This Markdown syntax extension, which is part of the MDITA Extended Profile, 
is enabled by default. In order to disable it, pass parameter 
`-p load.mdita.definition false` to `ditac`.

Example:

```markdown
Glossary:

LED
: Light emitting diode.

ABS
: Antilock braking system.

ESC
ESP
: Electronic stability control, also known as Electronic Stability Program.

: On motorcycles, ESC/ESP is called *Traction Control*.

  > Ducati was one of the first to introduce a true competition-level 
  > traction control system (**DTC**) on a production motorcycle.

EBA
: Emergency brake assist.
```

is converted to:

```xml
<p>Glossary:</p>
<dl>
   <dlentry>
     <dt>LED</dt>
     <dd>
       <p>Light emitting diode.</p>
     </dd>
   </dlentry>
   <dlentry>
     <dt>ABS</dt>
     <dd>
       <p>Antilock braking system.</p>
     </dd>
   </dlentry>
   <dlentry>
     <dt>ESC</dt>
     <dt>ESP</dt>
     <dd>
       <p>Electronic stability control, also known as
       Electronic Stability Program.</p>
     </dd>
     <dd>
       <p>On motorcycles, ESC/ESP is called <i>Traction Control</i>.</p>
       <lq>
         <p>Ducati was one of the first to introduce a
         true competition-level traction control system 
         (<b>DTC</b>) on a production motorcycle.</p>
       </lq>
     </dd>
   </dlentry>
   <dlentry>
     <dt>EBA</dt>
     <dd>
       <p>Emergency brake assist.</p>
     </dd>
   </dlentry>
 </dl>
```

which is rendered as:

Glossary:

LED
: Light emitting diode.

ABS
: Antilock braking system.

ESC
ESP
: Electronic stability control, also known as Electronic Stability Program.

: On motorcycles, ESC/ESP is called *Traction Control*.

  > Ducati was one of the first to introduce a true competition-level 
  > traction control system (**DTC**) on a production motorcycle.

EBA
: Emergency brake assist.

!!! remember ""
    Remember that:

    - The leading \"`:`\" character of a definition must be followed by one or
      more space characters.
    - Terms must be separated from the previous definition by a blank line.
    - A blank line is not allowed between two consecutive terms.
    - A blank line is allowed before a definition.

    {.compact-ul}

Footnotes
---------

Syntax for creating footnotes and footnote references.
{#ext-footnotes}

This Markdown syntax extension, which is part of the MDITA Extended Profile,
is enabled by default. In order to disable it, pass parameter
`-p load.mdita.footnotes false` to `ditac`.

Example:

```markdown
The differences between the programming languages C++[^1] and Java can be
traced to their heritage.

[^1]: The C++ Programming Language by Bjarne Stroustrup.

C++[^1] was designed for systems and applications programming, extending the
procedural programming language C[^2].

[^2]: The C Programming Language by Brian Kernighan and Dennis Ritchie.

      Originally published in 1978.
```

is converted to:

```xml
<p>The differences between the programming languages
C++<xref href="#./__FN1" type="fn"/> and Java can
be traced to their heritage.</p>

<div>
  <fn id="__FN1">The C++ Programming Language by
  Bjarne Stroustrup.</fn>
</div>

<p>C++<xref href="#./__FN1" type="fn"/> was designed 
for systems and applications programming, extending 
the procedural programming 
language C<xref href="#./__FN2" type="fn"/>.</p>

<div>
  <fn id="__FN2">The C Programming Language by 
  Brian Kernighan and Dennis Ritchie. 
  <p>Originally published in 1978.</p> </fn>
</div>
```

which is rendered as:

The differences between the programming languages C++[^1] and Java can be
traced to their heritage.

[^1]: The C++ Programming Language by Bjarne Stroustrup.

C++[^1] was designed for systems and applications programming, extending the
procedural programming language C[^2].

[^2]: The C Programming Language by Brian Kernighan and Dennis Ritchie.

      Originally published in 1978.

Strikethrough and subscript 
---------------------------

Converts
{#ext-gfm-strikethrough}

- tagged text \"`~~something deleted~~`\" to `<del>something deleted</del>`,
  which is rendered as: ~~something deleted~~
- tagged text \"`~a subscript~`\" to `<sub>a subscript<sub/>`,
  which is rendered as: ~a subscript~

{.compact-ul}

This Markdown syntax extension, which is part of the MDITA Core Profile and 
the MDITA Extended Profile, is enabled by default. In order to disable it,
pass parameter `-p load.mdita.gfm-strikethrough false` to `ditac`.

Ins 
---

Converts tagged text \"`++something new++`\" to `<ins>something new</ins>`, 
which is rendered as: ++something new++
{#ext-ins}

This Markdown syntax extension, which is part of the MDITA Extended Profile, 
is enabled by default. In order to disable it, pass parameter 
`-p load.mdita.ins false` to `ditac`.

Superscript 
-----------

Converts tagged text \"`^a superscript^`\" to `<sup>a superscript</sup>`, 
which is rendered as: ^a superscript^
{#ext-superscript}

This Markdown syntax extension, which is part of the MDITA Extended Profile, 
is enabled by default. In order to disable it, pass parameter
`-p load.mdita.superscript false` to `ditac`.

Tables 
------

Converts pipe \"`|`\" delimited text to `<table>` elements.
{#ext-tables}

This Markdown syntax extension, which is part of the MDITA Core Profile and 
the MDITA Extended Profile, is enabled by default. In order to disable it,
pass parameter `-p load.mdita.tables false` to `ditac`.

Simple table example:

```markdown
| Header 1 | Header 2 | Header 3 |
| -------- | -------- | -------- |
| Cell 1,1 | Cell 1,2 | Cell 1,3 |
| Cell 2,1 | Cell 2,2 | Cell 2,3 |
```

is converted to:

```xml
<table >
  <tgroup cols="3">
    <thead>
      <row valign="middle">
        <entry align="center">Header 1</entry>
        <entry align="center">Header 2</entry>
        <entry align="center">Header 3</entry>
      </row>
    </thead>
    <tbody>
      <row valign="middle">
        <entry>Cell 1,1</entry>
        <entry>Cell 1,2</entry>
        <entry>Cell 1,3</entry>
      </row>
      <row valign="middle">
        <entry>Cell 2,1</entry>
        <entry>Cell 2,2</entry>
        <entry>Cell 2,3</entry>
      </row>
    </tbody>
  </tgroup>
</table>
```

which is rendered as:

| Header 1 | Header 2 | Header 3 |
| -------- | -------- | -------- |
| Cell 1,1 | Cell 1,2 | Cell 1,3 |
| Cell 2,1 | Cell 2,2 | Cell 2,3 |

Table example having centered and right-aligned columns:

```markdown
| Header 1 | Header 2        | Table Header 3 |
| -------- | :-------------: | -------------: |
| Cell 1,1 | Table cell 1,2  | Cell 1,3       |
| Cell 2,1 | Cell 2,2        | Cell 2,3       |
```

is converted to:

```xml
<table>
  <tgroup cols="3">
    <thead>
      <row valign="middle">
        <entry align="center">Header 1</entry>
        <entry align="center">Header 2</entry>
        <entry align="right">Table Header
        3</entry>
      </row>
    </thead>
    <tbody>
      <row valign="middle">
        <entry>Cell 1,1</entry>
        <entry align="center">Table cell
        1,2</entry>
        <entry align="right">Cell 1,3</entry>
      </row>
      <row valign="middle">
        <entry>Cell 2,1</entry>
        <entry align="center">Cell 2,2</entry>
        <entry align="right">Cell 2,3</entry>
      </row>
    </tbody>
  </tgroup>
</table>
```

which is rendered as:

| Header 1 | Header 2        | Table Header 3 |
| -------- | :-------------: | -------------: |
| Cell 1,1 | Table cell 1,2  | Cell 1,3       |
| Cell 2,1 | Cell 2,2        | Cell 2,3       |

Table example having cells spanning several columns and a caption:

```markdown
| Header 1 | Header 2 | Header 3 |
| -------- | -------- | -------- |
| Cell 1,1 + 1,2     || Cell 1,3 |
| Cell 2,1 + 2,2 + 2,3         |||
| Cell 3,1 | Cell 3,2 | Cell 3,3 |
[Table caption here]
```

is converted to:

```xml
<table>
  <title>Table caption here</title>
  <tgroup cols="3">
    <colspec colname="c1" rowheader="headers"/>
    <colspec colname="c2" rowheader="headers"/>
    <colspec colname="c3" rowheader="headers"/>
    <thead>
      <row valign="middle">
        <entry align="center">Header 1</entry>
        <entry align="center">Header 2</entry>
        <entry align="center">Header 3</entry>
      </row>
    </thead>
    <tbody>
      <row valign="middle">
        <entry nameend="c2" namest="c1">Cell 1,1
        + 1,2</entry>
        <entry>Cell 1,3</entry>
      </row>
      <row valign="middle">
        <entry nameend="c3" namest="c1">Cell 2,1
        + 2,2 + 2,3</entry>
      </row>
      <row valign="middle">
        <entry>Cell 3,1</entry>
        <entry>Cell 3,2</entry>
        <entry>Cell 3,3</entry>
      </row>
    </tbody>
  </tgroup>
</table>
```

which is rendered as:

| Header 1 | Header 2 | Header 3 |
| -------- | -------- | -------- |
| Cell 1,1 + 1,2     || Cell 1,3 |
| Cell 2,1 + 2,2 + 2,3         |||
| Cell 3,1 | Cell 3,2 | Cell 3,3 |
[Table caption here]

Typographic characters 
----------------------

Converts
{#ext-typographic}

- \"`'`\" to apostrophe `&rsquo;`, which is rendered as in word: \"don't\"
- \"`...`\" and \"`. . .`\" to ellipsis `&hellip;`, 
  which are both rendered as: ...
- \"`--`\" to en dash `&ndash;`, which is rendered as: --
- \"`---`\" to em dash `&mdash;`, which is rendered as: ---
- single quoted `'some text'` to `&lsquo;some text&rsquo;`,
  which is rendered as: 'some text'
- double quoted `"some text"` to `&ldquo;some text&rdquo;`,
  which is rendered as: "some text"
- double angle quoted `<<some text>>` to `&laquo;some text&raquo;`, 
  which is rendered as: <<some text>>

{.compact-ul}

This Markdown syntax extension, which is part of the MDITA Extended Profile, 
is enabled by default. In order to disable it, pass parameter
`-p load.mdita.typographic false` to `ditac`.

If you don't want some of the above plain text sequences to be processed, 
specify:

`-p load.mdita.typographic.ENABLE_QUOTES false`
: Do not process single quotes, double quotes, double angle quotes.

`-p load.mdita.typographic.ENABLE_SMARTS false`
: Do not process \"`'`\", \"`...`\", \"`. . .`\", \"`--`\", \"`---`\".

YAML front matter 
-----------------

Syntax for adding metadata to the generated DITA topic or map, that is, 
for populating the `<prolog>` element of a DITA topic and 
the `<topicmeta>` element of a DITA map.
{#ext-yaml-front-matter}

These metadata are specified by key/value pairs written using a subset of the 
[YAML](https://en.wikipedia.org/wiki/YAML) (see also <http://yaml.org/>) syntax.

Supported metadata are:

- `audience`
- `author`
- `category`
- `created` (maps to `<critdates>/<created>`)
- `keyword` (maps to `<keywords>/<keyword>`)
- `permissions`
- `publisher`
- `resourceid`
- `revised` (maps to `<critdates>/<revised>`)
- `source`

{.compact-ul}

Any other metadata is translated to DITA element `<data>`.

This Markdown syntax extension, which is part of the MDITA Core Profile and 
the MDITA Extended Profile, is enabled by default. In order to disable it,
pass parameter `-p load.mdita.yaml-front-matter false` to `ditac`.

Example:

```markdown
---
author:
  - Brian W. Kernighan
  - Dennis Ritchie
publisher: Prentice Hall
created: 1978/01/01
revised: 1988/01/01
---
```

is converted to:

```xml
<prolog>
  <author>Brian W. Kernighan</author>
  <author>Dennis Ritchie</author>
  <publisher>Prentice Hall</publisher>
  <critdates>
    <created date="1978/01/01"/>
    <revised modified="1988/01/01"/>
  </critdates>
</prolog>
```

Other extensions 
----------------

The following Markdown syntax extensions are also supported:
{#other_extensions}

- anchorlink
- aside
- emoji
- enumerated-reference
- gfm-issues
- gfm-tasklist
- gfm-users
- toc
- wikilink
- youtube-embedded

{.compact-ul}

All the above extensions are disabled by default. In order to enable an
extension, pass parameter `-p load.mdita.EXTENSION_NAME true` to
`ditac`. For example: `-p load.mdita.emoji true`

Any extension listed in this section may be parameterized by passing parameter
`-p load.mdita.EXTENSION_NAME.PARAMETER_NAME PARAMETER_VALUE`[^§ a] to
`ditac`. Examples: 

- `-p load.mdita.emoji.ATTR_IMAGE_SIZE 16`
- `-p load.mdita.emoji.ATTR_ALIGN ""`
- `-p load.mdita.emoji.USE_IMAGE_TYPE IMAGE_ONLY `
- `-p load.mdita.emoji.ROOT_IMAGE_PATH https://www.webpagefx.com/tools/emoji-cheat-sheet/graphics/emojis/`

{.compact-ul}

More generally, the Markdown parser (pseudo *EXTENSION_NAME* is \"`parser`\")
and the HTML renderer (pseudo *EXTENSION_NAME* is \"`renderer`\") may also be
parameterized this way. For example, automatically generate an ID for all
headings not already having an ID **and** "render" all heading IDs
in HTML[^§ b]:

```
-p load.mdita.renderer.GENERATE_HEADER_ID true
-p load.mdita.renderer.RENDER_HEADER_ID true
```

More information about extensions and their parameters in
[Extensions][flexmark-java-ext] ([flexmark-java] is the software component
used by ditac to parse Markdown and convert it to HTML).

[flexmark-java]: https://github.com/vsch/flexmark-java
[flexmark-java-ext]: https://github.com/vsch/flexmark-java/wiki/Extensions

[^§ a]: The only types supported for *PARAMETER_VALUE* are: string, boolean
(`true` or `false`), integer and any enumerated type.
[^§ b]: By default, heading IDs are not "rendered" in HTML,
which is somewhat counterintuitive.