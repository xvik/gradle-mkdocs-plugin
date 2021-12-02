# Migration notes

!!! summary
    This section mostly describes main changes in updated mkdocs and material
    versions, assuming you'll use default versions (provided by plugin).

## 2.2.0 

Guides:
* Mkdocs 1.2.x [breaking changes list](https://www.mkdocs.org/about/release-notes/#backward-incompatible-changes-in-12)
* Material 8.x [migration notes](https://squidfunk.github.io/mkdocs-material/upgrade/#upgrading-from-7x-to-8x)

Notable configuration changes:

`site_url` is now **required** (but it may be empty).

`google_analytics` option was deprecated, instead [material specific configuration](https://squidfunk.github.io/mkdocs-material/upgrade/#google_analytics) 
must be used:

```yaml
extra:
  analytics:
    provider: google
    property: UA-XXXXXXXX-X
```

Not required (yet), but advised to replace `codehilite` extension with `pymdownx.highlight`.

Even better, replace your extensions list with the [recommended configuration](https://squidfunk.github.io/mkdocs-material/setup/extensions/#recommended-configuration).
`mkdocsInit` task would also produce recommended list in the generated config:

```yaml
markdown_extensions:
  # Python Markdown
  - abbr
  - admonition
  - attr_list
  - def_list
  - footnotes
  - meta
  - md_in_html
  - toc:
      permalink: true

  # Python Markdown Extensions
  - pymdownx.arithmatex:
      generic: true
  - pymdownx.betterem:
      smart_enable: all
  - pymdownx.caret
  - pymdownx.details
  - pymdownx.emoji:
      emoji_index: !!python/name:materialx.emoji.twemoji
      emoji_generator: !!python/name:materialx.emoji.to_svg
  - pymdownx.highlight
  - pymdownx.inlinehilite
  - pymdownx.keys
  - pymdownx.mark
  - pymdownx.smartsymbols
  - pymdownx.superfences
  - pymdownx.tabbed:
      alternate_style: true
  - pymdownx.tasklist:
      custom_checkbox: true
  - pymdownx.tilde
```

[Dark theme toggle activation](https://squidfunk.github.io/mkdocs-material/setup/changing-the-colors/#color-palette-toggle)
with suggested [navigation features](https://squidfunk.github.io/mkdocs-material/setup/setting-up-navigation/):

```yaml
theme:
  name: 'material'
  palette:
    - media: "(prefers-color-scheme: light)"
      scheme: default
      toggle:
        icon: material/toggle-switch-off-outline
        name: Switch to dark mode
    - media: "(prefers-color-scheme: dark)"
      scheme: slate
      toggle:
        icon: material/toggle-switch
        name: Switch to light mode
  features:
    #- navigation.tabs
    #- navigation.tabs.sticky
    #- navigation.instant
    - navigation.tracking
    - navigation.top
```