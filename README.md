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


[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/gradle-mkdocs-plugin.svg?label=jcenter)](https://bintray.com/vyarus/xvik/gradle-mkdocs-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-mkdocs-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-mkdocs-plugin)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/ru/vyarus/mkdocs/ru.vyarus.mkdocs.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=plugins%20portal)](https://plugins.gradle.org/plugin/ru.vyarus.mkdocs)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-mkdocs-plugin:1.1.0'
    }
}
apply plugin: 'ru.vyarus.mkdocs'
```

OR 

```groovy
plugins {
    id 'ru.vyarus.mkdocs' version '1.1.0'
}
```

#### Compatibility

Plugin compiled for java 7

**Requires installed python** 2.7 or 3.4 and above with pip.

[Check and install python if required](https://github.com/xvik/gradle-use-python-plugin#python--pip).


#### Snapshots

<details>
      <summary>Snapshots may be used through JitPack</summary>

* Go to [JitPack project page](https://jitpack.io/#ru.vyarus/gradle-mkdocs-plugin)
* Select `Commits` section and click `Get it` on commit you want to use 
    or use `master-SNAPSHOT` to use the most recent snapshot

For gradle before 6.0 use `buildscript` block with required commit hash as version:

```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'ru.vyarus:gradle-mkdocs-plugin:2450c7e881'
    }
}
apply plugin: 'ru.vyarus.mkdocs'
```

For gradle 6.0 and above:

* Add to `settings.gradle` (top most!) with required commit hash as version:

  ```groovy
  pluginManagement {
      resolutionStrategy {
          eachPlugin {
              if (requested.id.namespace == 'ru.vyarus.java-lib') {
                  useModule('ru.vyarus:gradle-mkdocs-plugin:2450c7e881')
              }
          }
      }
      repositories {
          maven { url 'https://jitpack.io' }
          gradlePluginPortal()          
      }
  }    
  ``` 
* Use plugin without declaring version: 

  ```groovy
  plugins {
      id 'ru.vyarus.mkdocs'
  }
  ```  

</details>  


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
