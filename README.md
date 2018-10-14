# Gradle Mkdocs plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://www.opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/travis/xvik/gradle-mkdocs-plugin.svg)](https://travis-ci.org/xvik/gradle-mkdocs-plugin)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/gradle-mkdocs-plugin?svg=true)](https://ci.appveyor.com/project/xvik/gradle-mkdocs-plugin)
[![codecov](https://codecov.io/gh/xvik/gradle-mkdocs-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/gradle-mkdocs-plugin)

**DOCUMENTATION** https://xvik.github.io/gradle-mkdocs-plugin/

### About

Generates project documentation with [Mkdocs](http://www.mkdocs.org/) python tool. 

Ideal for open source projects:

* Easy start: initial docs source generation
* Markdown syntax (with handy extensions)
* Great look from [material theme](https://squidfunk.github.io/mkdocs-material/) (used by default) with extra features:
    - Mobile friendly
    - Embedded search
    - Syntax highlighting
* Easy documentation contribution (jump to source)
* Multi-version documentation publishing to github pages 

##### Summary

* Configuration: `mkdocs`
* Tasks:
    - `mkdocsInit` - generate initial site 
    - `mkdocsServe` - livereload server (dev)
    - `mkdocsBuild` - build documentation
    - `mkdocsPublish` - publish to github pages
    - `type:MkdocsTask` to call custom mdocs commands   
* Enable plugins: [git-publish](https://github.com/ajoberstar/gradle-git-publish),
[use-python](https://github.com/xvik/gradle-use-python-plugin)

### Setup

Releases are published to [bintray jcenter](https://bintray.com/vyarus/xvik/gradle-mkdocs-plugin/), 
[maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-mkdocs-plugin) and 
[gradle plugins portal](https://plugins.gradle.org/plugin/ru.vyarus.mkdocs).


[![JCenter](https://api.bintray.com/packages/vyarus/xvik/gradle-mkdocs-plugin/images/download.svg)](https://bintray.com/vyarus/xvik/gradle-mkdocs-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-mkdocs-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-mkdocs-plugin)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-mkdocs-plugin:1.0.1'
    }
}
apply plugin: 'ru.vyarus.mkdocs'
```

OR 

```groovy
plugins {
    id 'ru.vyarus.mkdocs' version '1.0.1'
}
```

#### Python

**Requires installed python** 2.7 or 3.3 and above with pip.

[Check and install python if required](https://github.com/xvik/gradle-use-python-plugin#python--pip).

### Usage

Read [documentation](https://xvik.github.io/gradle-mkdocs-plugin/)

### Might also like

* [quality-plugin](https://github.com/xvik/gradle-quality-plugin) - java and groovy source quality checks
* [animalsniffer-plugin](https://github.com/xvik/gradle-animalsniffer-plugin) - java compatibility checks
* [pom-plugin](https://github.com/xvik/gradle-pom-plugin) - improves pom generation
* [java-lib-plugin](https://github.com/xvik/gradle-java-lib-plugin) - avoid boilerplate for java or groovy library project
* [github-info-plugin](https://github.com/xvik/gradle-github-info-plugin) - pre-configure common plugins with github related info


---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
