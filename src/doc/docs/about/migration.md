# Migration notes

!!! summary
    This section mostly describes main changes in updated mkdocs and material
    versions, assuming you'll use default versions (provided by plugin).

## 3.0.0

Gradle 5.0-5.2 is not supported anymore.

Use python 3.8 and above: there might be problem with `importlib-metadata 5.0` package
installed by version range (transitive dependency) and incompatible with python < 3.8

New configuration `mkdocs.devPort` overrides `dev_addr` configuration in your mkdocs.yml
(this was required for proper docker support).
If you need to change default port (3000), use gradle config to change it

## 2.4.0

Python 3.6 is not supported anymore, use python 3.7 - 3.10.

## 2.3.0

There is no breaking changes since 2.2.0, but if you're migrating from earlier version please see
[2.2.0 migration notes](https://xvik.github.io/gradle-mkdocs-plugin/2.2.0/about/migration/).
