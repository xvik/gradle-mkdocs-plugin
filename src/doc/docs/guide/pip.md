# Pip modules

Plugin will install by default the following pip modules:

* [mkdocs:1.1.2](https://pypi.python.org/pypi/mkdocs)
* [mkdocs-material:7.0.3](https://pypi.python.org/pypi/mkdocs-material)
* [pygments:2.8.0](https://pypi.python.org/pypi/Pygments)
* [pymdown-extensions:8.1.1](https://pypi.python.org/pypi/pymdown-extensions)

By default, modules are installed into project specific [virtualenv](https://github.com/xvik/gradle-use-python-plugin#virtualenv)
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

You can use `pipUpdates` task to check if newer module [versions are available](https://github.com/xvik/gradle-use-python-plugin#check-modules-updates).
