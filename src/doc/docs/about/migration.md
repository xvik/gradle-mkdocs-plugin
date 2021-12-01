# Migration notes

!!! summary
    This section mostly describes main changes in updated mkdocs and material
    versions, assuming you'll use default versions (provided by plugin).

## 1.2.0 

Guides:
* Mkdocs 1.2 [breaking changes list](https://www.mkdocs.org/about/release-notes/#backward-incompatible-changes-in-12)
* Material 8 [migration notes](https://squidfunk.github.io/mkdocs-material/upgrade/#upgrading-from-7x-to-8x)

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
The list of enabled extensions, produced by init task is now:

```yaml
markdown_extensions:
  - admonition
  - footnotes
  - meta
  - def_list
  - toc:
      permalink: true
  - pymdownx.betterem:
      smart_enable: all
  - pymdownx.caret
  - pymdownx.inlinehilite
  - pymdownx.smartsymbols
  - pymdownx.highlight
  - pymdownx.superfences
```

See all possible extensions in [material docs](https://squidfunk.github.io/mkdocs-material/setup/extensions/).

New list of proposed [navigation features](https://squidfunk.github.io/mkdocs-material/setup/setting-up-navigation/) is:

```yaml
theme:
  name: 'material'
  features:
    #- navigation.tabs
    #- navigation.tabs.sticky
    #- navigation.instant
    - navigation.tracking
    - navigation.top
```