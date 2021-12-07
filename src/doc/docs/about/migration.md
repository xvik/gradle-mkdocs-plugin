# Migration notes

!!! summary
    This section mostly describes main changes in updated mkdocs and material
    versions, assuming you'll use default versions (provided by plugin).

## 2.2.0 

Guides:

* Mkdocs 1.2.x [breaking changes list](https://www.mkdocs.org/about/release-notes/#backward-incompatible-changes-in-12)
* Material 8.x [migration notes](https://squidfunk.github.io/mkdocs-material/upgrade/#upgrading-from-7x-to-8x)

### Breaking changes

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

Even better, replace your extensions list with the [recommended configuration](https://squidfunk.github.io/mkdocs-material/setup/extensions/#recommended-configuration):

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

### Documentation aliases

It is now possible to publish version not only into version folder, but also into
aliased folders.

Could be useful for:

- Publishing the latest documentation under `latest` alias, so users could always
  reference the latest docs with the same link.
- Publishing docs for developing version under `dev` alias, so users could easily find dev docs.
- Serving the latest (patch) version for some mojor version: e.g. `5.x` alias could serve the latest
  published bugfix.

!!! note
    Feature implemented exactly the same as in [mike](https://github.com/jimporter/mike).
  
To enable aliases simply specify one or more of them:

```yaml
mkdocs.publish.versionAliases = ['latest']
```

!!! note
    Alias folder contains a *copy* of generated documentation, which means
    that sitemap links would lead to path of exact version.

If same version is re-published - aliases would be correctly updated too.

It is also possible now to *redirect root into alias* instead of exact version with new `rootRedirectTo` option:

```groovy
mkdocs.publish {
    versionAliases = ['latest']
    rootRedirectTo = 'latest'
}
```

!!! tip
    In case of root redirection to alias it is better to enable version switcher to clearly show what version
    is active now (otherwise it would be not obvious)

!!! warning
    Be careful when publishing old version: `versionAliases` option must be removed to not override
    existing aliases!
    Use `mkdocsVersionsFile` task instead of `mkdocsBuild` to validate correctness: it would
    try to detect such dangerous cases and put a warning for you (but this warning could be
    false-positive because used versions comparison could be wrong - and that's why
    it's a warning and not exception).      

### Version switcher

Version switcher might be enabled [exactly as described in docs](https://squidfunk.github.io/mkdocs-material/setup/setting-up-versioning/#versioning):

```yaml
extra:
  version:
    provider: mike
```

!!! important
    You don't need [mike](https://github.com/jimporter/mike) itself! Plugin implements exactly the same functionality,
    but in a way much easier for gradle plugin behaviour customization.

Mkdocs-material requires only `versions.json` file stored at docs root. Plugin would automatically
generate such file (following mike syntax):

- Plugin verifies actually existing directories in gh-pages repository and would
  add them to generated versions file. So if you already have many versions published, just publish
  new version with enabled versions support and you'll see all of them in the version switcher.
- Theme folders are detected by using `\d+(\..*)?` regexp (version folder must start with a number)
  and it must contain 404.html file.
- Existing records in versions.json file are preserved for found version folders.
    - You can modify file manually (e.g. to modify version title) and will not be overridden on next publication
    - You can manually remove version folder in repository and on next publication versions.json would be corrected 
- If aliases used, they would be correctly updated (e.g. `latest` removed from previous latest version.)

If you do not want to generate versions file:

`mkdocs.publish.generateVersionsFile = false`

To customize version title (shown in dropdown selection) use:

`mkdocs.publish.versionTitle = '1.0 (important fix)'`

### Dark theme

[Dark theme toggle](https://squidfunk.github.io/mkdocs-material/setup/changing-the-colors/#color-palette-toggle)
may be enabled with:

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
```

### Navigation features

Suggested [navigation features](https://squidfunk.github.io/mkdocs-material/setup/setting-up-navigation/) list:

```yaml
theme:
  features:
    #- navigation.tabs
    #- navigation.tabs.sticky
    #- navigation.instant
    - navigation.tracking
    - navigation.top
```