# Gradle Mkdocs plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://www.opensource.org/licenses/MIT)
[![CI](https://github.com/xvik/gradle-mkdocs-plugin/actions/workflows/CI.yml/badge.svg)](https://github.com/xvik/gradle-mkdocs-plugin/actions/workflows/CI.yml)
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
    - Dark theme switcher
* Easy documentation contribution (jump to source)
* Multi-version documentation publishing to github pages 
    - Support version aliases (latest, dev, etc)
    - Support mkdocs-material version switcher without mike tool usage
* Variables support
* Could work with direct python or docker.
* Could use requirements.txt file
* Compatible with gradle configuration cache

##### Summary

* `mkdocs-build` plugin:
  - Configuration: `mkdocs`
  - Tasks:
      - `mkdocsInit` - generate initial site 
      - `mkdocsServe` - livereload server (dev)
      - `mkdocsBuild` - build documentation  
      - `type:MkdocsTask` to call custom mkdocs commands
    
* `mkdocs` plugin adds:
  - Configuration: `gitPublish`
  - Tasks:
      - `gitPublishReset` - checkout repository
      - `mkdocsVersionsFile` - generate versions.json file for version switcher
      - `gitPublishCopy` - copy (and add) files into repository
      - `gitPublishCommit` - commit updates into repository
      - `gitPublishPush` - push commit
      - `mkdocsPublish` - publish to github pages (main task)
    
* Enable plugins: [use-python](https://github.com/xvik/gradle-use-python-plugin)

NOTE: plugin is based on [use-python plugin](https://github.com/xvik/gradle-use-python-plugin) see python-specific 
tasks there.  

### Setup

[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-mkdocs-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-mkdocs-plugin)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/ru/vyarus/mkdocs/ru.vyarus.mkdocs.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=plugins%20portal)](https://plugins.gradle.org/plugin/ru.vyarus.mkdocs)

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-mkdocs-plugin:4.0.1'
    }
}
apply plugin: 'ru.vyarus.mkdocs'
```

OR 

```groovy
plugins {
    id 'ru.vyarus.mkdocs' version '4.0.1'
}
```

#### Lightweight setup

There is also a lightweight plugin version without publication tasks:

```groovy
plugins {
    id 'ru.vyarus.mkdocs-build' version '4.0.1'
}
```

Lightweight plugin is suitable if you don't need git publication and don't want extra
tasks to appear.

#### Compatibility

Plugin compiled for java 8, compatible with java 11.

Gradle | Version
--------|-------
7.0     | 4.0.1
5.3     | [3.0.0](https://xvik.github.io/gradle-mkdocs-plugin/3.0.0/)
5-5.2   | [2.3.0](https://xvik.github.io/gradle-mkdocs-plugin/2.3.0/)
4.x     | [1.1.0](https://github.com/xvik/gradle-mkdocs-plugin/tree/1.1.0)

**Requires installed python** 3.8 and above with pip.

[Check and install python if required](https://xvik.github.io/gradle-use-python-plugin/2.3.0/guide/python/).


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
              if (requested.id.namespace == 'ru.vyarus.mkdocs') {
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
* [yaml-updater](https://github.com/xvik/yaml-updater) - yaml configuration update tool, preserving comments and whitespaces


---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
