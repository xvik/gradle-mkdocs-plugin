# Gradle Mkdocs plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://www.opensource.org/licenses/MIT)
[![CI](https://github.com/xvik/gradle-mkdocs-plugin/actions/workflows/CI.yml/badge.svg)](https://github.com/xvik/gradle-mkdocs-plugin/actions/workflows/CI.yml)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/gradle-mkdocs-plugin?svg=true)](https://ci.appveyor.com/project/xvik/gradle-mkdocs-plugin)
[![codecov](https://codecov.io/gh/xvik/gradle-mkdocs-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/gradle-mkdocs-plugin)

**DOCUMENTATION** https://xvik.github.io/gradle-mkdocs-plugin/

### MkDocs state (2026)

[Mkdocs](https://www.mkdocs.org/) developer stopped mkdocs 1.x maintenance ([one](https://github.com/mkdocs/mkdocs/discussions/3621), [two](https://github.com/mkdocs/mkdocs/discussions/4010))
and did a complete rewrite: [mkdocs 2.0 (pre-release)](https://github.com/mkdocs/mkdocs/discussions/4077)
is not backwards compatible and [completely breaks ecosystem](https://squidfunk.github.io/mkdocs-material/blog/2026/02/18/mkdocs-2.0/#whats-changing-in-mkdocs-20) (plugins).

Because of this, [mkdocs-material](https://squidfunk.github.io/mkdocs-material/) developers [stopped development](https://squidfunk.github.io/mkdocs-material/blog/2026/02/18/mkdocs-2.0/)
(only critical bug fixes until November 2026). In short, forking was not an option because plugins
depend on `mkdocs` dependency, so they decided to create a completely new tool [Zensical](https://squidfunk.github.io/mkdocs-material/blog/2025/11/05/zensical/)
(alpha state; compatibility with mkdocs-material is a [top priority](https://squidfunk.github.io/mkdocs-material/blog/2025/11/05/zensical/#maximum-compatibility)).

But the mkdocs community is strong, so we also have two maintained forks for mkdocs and mkdocs-material:

* [ProperDocs](https://github.com/ProperDocs/properdocs) with [materialx](https://github.com/jaywhj/mkdocs-materialx)
* [mkdocs-ng](https://github.com/mkdocs/mkdocs/discussions/4010#discussioncomment-16745804) ([material inside](https://github.com/mkdocs-ng/mkdocs-material)) ([why another fork](https://github.com/mkdocs/mkdocs/discussions/4010#discussioncomment-16751778))

*No need to panic*. As you can see, **we have options** to migrate. Also, the current
mkdocs release could be used for years ahead.

#### The plugin state

I'm going to release the plugin with updated defaults (latest mkdocs and material) which could
be used for years (python backwards compatibility is quite good).

Also, I'm going to add an option to switch to [materialx](https://github.com/jaywhj/mkdocs-materialx)
(two sets of defaults and, probably, initial templates), so we could have an
easy migration, if required. The migration process would be documented.

[Zensical](https://zensical.org/) is in too early stage, so just waiting for its evolution. When it matures and
becomes a good enough alternative, a new plugin could be created (we'll see).

No panic, no worries! Beautiful documentation is still with us.


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

* Add to `settings.gradle` (top most!) (exact commit hash might be used as version):

  ```groovy
  pluginManagement {
      resolutionStrategy {
          eachPlugin {
              if (requested.id.id == 'ru.vyarus.mkdocs') {
                  useModule('ru.vyarus:gradle-mkdocs-plugin:master-SNAPSHOT')
              }
          }
      }
      repositories {                        
          gradlePluginPortal()
          maven { url 'https://jitpack.io' }                    
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
