### [2.1.0](http://xvik.github.io/gradle-mkdocs-plugin/2.1.0) (2021-03-17)
* Support python installed from Windows Store (use-python-plugin 2.3.0)
* Update packages:
    - mkdocs 1.1 -> 1.1.2
    - mkdocs-material 4.6.3 -> 7.0.6
    - pygments 2.6.1 -> 2.8.0
    - pymdown-extensions 6.3.0 -> 8.1.1
* Optional variables support for all mkdocs tasks: there is no (and not planned) native support for
  variables in mkdocs, but often it is very handful. It is only possible to have it with a plugin. ([#7](https://github.com/xvik/gradle-mkdocs-plugin/issues/7))
    - Added mkdocs-markdownextradata-plugin 0.2.4 as installed module (no harm, you must active it manually!)
    - Added mkdocs.extras configuration option: map to declare additional variables
    - When extra variables declared, plugin would generate a special file, containing all declared variables,
      which markdownextradata plugin would recognize and use automatically.
    - Variables must be used with 'gradle' prefix: {% raw %} {{ gradle.declared_var_name }} {% endraw %}

### [2.0.1](http://xvik.github.io/gradle-mkdocs-plugin/2.0.1) (2020-04-06)
* Fix relative virtualenv paths support (don't rely on gradle work dir) (#5)

### [2.0.0](http://xvik.github.io/gradle-mkdocs-plugin/2.0.0) (2020-03-13)
* (breaking) Drop java 7 support
* (breaking) Drop gradle 4 support
* Fix jgit dependency conflict (#4) (plugin now use jgit 5)
* Update packages:
    - mkdocs 1.0.4 -> 1.1
    - mkdocs-material 3.0.4 -> 4.6.3
    - pygments 2.2.0 -> 2.6.1
    - pymdown-extensions 6.0.0 -> 6.3.0
* Use gradle tasks configuration avoidance for lazy tasks initialization (no init when tasks not needed)    

### [1.1.0](http://xvik.github.io/gradle-mkdocs-plugin/1.1.0) (2018-10-14)
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


### [1.0.1](http://xvik.github.io/gradle-mkdocs-plugin/1.0.1) (2018-04-23)
* Fix pip 10 compatibility (use-python plugin 1.0.2)
* Update default mkdocs 0.17.2 -> 0.17.3
* Update default mkdocs-material 2.2.1 -> 2.7.2
* Update default pymdown-extensions 4.6 -> 4.9.2

### [1.0.0](https://github.com/xvik/gradle-mkdocs-plugin/tree/1.0.0) (2017-12-30)
* Initial release