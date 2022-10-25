# Pip modules

Plugin will install by default the following pip modules:

* [mkdocs:{{ gradle.mkdocs }}](https://pypi.python.org/pypi/mkdocs)
* [mkdocs-material:{{ gradle.mkdocs_material }}](https://pypi.python.org/pypi/mkdocs-material)
* [pygments:{{ gradle.pygments }}](https://pypi.python.org/pypi/Pygments)
* [pymdown-extensions:{{ gradle.pymdown_extensions }}](https://pypi.python.org/pypi/pymdown-extensions)
* [mkdocs-markdownextradata-plugin:{{ gradle.mkdocs_markdownextradata_plugin }}](https://pypi.org/project/mkdocs-markdownextradata-plugin/)

By default, modules are installed into project specific [virtualenv](https://xvik.github.io/gradle-use-python-plugin/{{ gradle.pythonPlugin }}/guide/configuration/#virtualenv)
(located in `.gradle/python`). 
You can see all installed modules with `pipList` task.

If you want to use other python modules (e.g. other theme):

```groovy
python.pip 'other-module:12', 'and-other:1.0'
```

Also, you can override default modules versions:

```groovy
python.pip 'mkdocs:1.1'
```

And even downgrade:

```groovy
python.pip 'mkdocs:1.0.3'
```

You can use `pipUpdates` task to check if newer module [versions are available](https://xvik.github.io/gradle-use-python-plugin/{{ gradle.pythonPlugin }}/guide/pip/#check-modules-updates).
