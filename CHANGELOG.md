* (breaking) Drop java 7 support
* (breaking) Drop gradle 4 support
* Fix jgit dependency conflict (#4)  (plugin now use jgit 5)
* Update packages:
    - mkdocs 1.0.4 -> 1.1
    - mkdocs-material 3.0.4 -> 4.6.3
    - pygments 2.2.0 -> 2.6.1
    - pymdown-extensions 6.0.0 -> 6.3.0
* Use gradle tasks configuration avoidance for lazy tasks initialization (no init when tasks not needed)    

### 1.1.0 (2018-10-14)
* Default template's mkdocs.yml changes:
    - fixed `edit_uri` (missed "/docs" ending)
    - `pages` changed to `nav`
    - change parameters syntax in `markdown_extensions` section 
* Fix documentation in sub module support (use-python plugin 1.2.0)
* Support Mkdocks 1.0:
    - Update default mkdocs 0.17.3 -> 1.0.4
    - Update default mkdocs-material 2.7.2 -> 3.0.4
    - Update default pymdown-extensions 4.9.2 -> 6.0.0

[Mkdocs 1.0](https://www.mkdocs.org/about/release-notes/#version-10-2018-08-03) migration notes (for existing docs):

- Rename `pages` section into `nav` 
- Make sure `site_url` set correctly (otherwise sitemap will contain None instead of urls)
- Change `markdown_extensions` section from using `something(arg=val)` syntax into:

```yaml
markdown_extensions:
  - admonition
  - codehilite:
      guess_lang: false
  - footnotes
  - meta
  - toc:
      permalink: true
  - pymdownx.betterem:
      smart_enable: all
  - pymdownx.caret
  - pymdownx.inlinehilite
  - pymdownx.magiclink
  - pymdownx.smartsymbols
  - pymdownx.superfences
```

### 1.0.1 (2018-04-23)
* Fix pip 10 compatibility (use-python plugin 1.0.2)
* Update default mkdocs 0.17.2 -> 0.17.3
* Update default mkdocs-material 2.2.1 -> 2.7.2
* Update default pymdown-extensions 4.6 -> 4.9.2

### 1.0.0 (2017-12-30)
* Initial release