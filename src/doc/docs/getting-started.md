# Getting started

## Installation

Releases are published to [bintray jcenter](https://bintray.com/vyarus/xvik/gradle-mkdocs-plugin/), 
[maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-mkdocs-plugin) and 
[gradle plugins portal](https://plugins.gradle.org/plugin/ru.vyarus.mkdocs).


[![JCenter](https://api.bintray.com/packages/vyarus/xvik/gradle-mkdocs-plugin/images/download.svg)](https://bintray.com/vyarus/xvik/gradle-mkdocs-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-mkdocs-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-mkdocs-plugin)

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-mkdocs-plugin:2.0.1'
    }
}
apply plugin: 'ru.vyarus.mkdocs'
```

OR 

```groovy
plugins {
    id 'ru.vyarus.mkdocs' version '2.0.1'
}
```

### Python

**Requires installed python** 2.7 or 3.4 and above with pip.

[Check and install python if required](https://github.com/xvik/gradle-use-python-plugin#python--pip).

!!! note
    Plugin will not affect global python: it will create project-specific virtualenv (in `.gradle/python`) 
    and install all required (pip) modules there. This will grant build reproduction (once initialized virtualenv used for all 
    future executions). 

## Usage

By default, documentation source assumed to be in `src/doc`.

!!! tip
    Default location could be changed: `mkdocs.sourcesDir = 'docs'` 

Call `mkdocsInit` task to generate initial site version (into `src/doc` by default):

```
src/doc/
    docs/               - documentation source
        ...
        index.md
    mkdocs.yml          - site configuration
```

!!! note 
    Plugin does not use [mkdocs new](http://www.mkdocs.org/#getting-started) command for site generation: custom template used 
    with pre-configured plugins and enabled material theme.

Call `mkdocsServe` task to start live reload server to see default site: [http://127.0.0.1:8000/](http://127.0.0.1:8000/).

!!! tip
    Used port may be changed in mkdocs.yml with [dev_addr](https://www.mkdocs.org/user-guide/configuration/#dev_addr):
    
    ```yaml
    dev_addr: 127.0.0.1:3000
    ```

!!! warning 
    Python process will not be killed after you stop gradle execution (search and kill python process manually). This is a [known gradle problem](https://github.com/gradle/gradle/issues/1128) 
    and the only known workaround is to start task without daemon: `gradlew mkdocsServe --no-daemon`. 
    Another alternative is to start serve command directly: copy console command from task execution log and use it directly. 

## Initial site configuration

Open generated mkdocs config file `src/doc/mkdocs.yml`. It contains many commented options:

| Commented option | Recommendation |
|------------------|--------|   
| [site_author](http://www.mkdocs.org/user-guide/configuration/#site_author) | fill with you name or remove (appear in meta tags only) |
| [site_url](http://www.mkdocs.org/user-guide/configuration/#site_url) |  Set to documentation root url (gh-pages url). Used as meta tag, as a link on the home icon and inside generated sitemap.xml. **NOTE** plugin will automatically modify url to point to correct published folder (when multi-version publication used). |
| | **Repository link on each page (right top corner)** |
| [repo_name](http://www.mkdocs.org/user-guide/configuration/#repo_name) | Source repository link text (by default set to project name) |
| [repo_url](http://www.mkdocs.org/user-guide/configuration/#repo_url) | Repository url (Github or Bitbucket) |
| [edit_uri](http://www.mkdocs.org/user-guide/configuration/#edit_uri) | Path to documentation source in the source repository (required for "edit page" (pencil icon) link)|
|  | **Copyright** |
| [copyright](http://www.mkdocs.org/user-guide/configuration/#copyright)| Shown below each page |

For material theme configuration see: [configuration docs](https://squidfunk.github.io/mkdocs-material/getting-started/#configuration).
Note that most useful material theme extensions are already enabled (see `markdown_extensions` section).
 
## Writing

Yaml configuration `nav` section declares your documentation structure. Pages inside `docs` folder
may be structured as you want.

To add new page simply add new markdown file (page.md) and add reference to it in `nav` config section.   

!!! note
    All changes are immediately appeared in the started live reload server (`mkdocsServe`)

Read:

* Mkdocs [getting started guide](http://www.mkdocs.org/#getting-started).
* Mkdocs-material [extensions docs](https://squidfunk.github.io/mkdocs-material/getting-started/#extensions).

## Building

!!! warning
    You will need to stop livereload server in order to build

By default, `mkdocsBuild` task will generate (suppose project version is '1.0-SNAPSHOT'):

```
build/mkdocs/
    /1.0-SNAPSHOT/    - mkdocs site
    index.html        - redirect to correct site
```

Plugin is configured for multi-version documentation publishing: each version is in it's own folder
and special `index.html` at the root will redirect to the latest version (when published).

Everything in `build/mkdocs/` is assumed to be published into github pages (preserving all other already published folders).  

Default configuration:

```groovy
mkdocs.publish {
    docPath = '$version'  
    rootRedirect = true  
}
``` 

As documentation is often updated for already released version, it makes sense to define 
current version manually (or define it when you need to publish to exact version):

```groovy
mkdocs.publish.docPath = '1.0'
```

!!! tip
    See [multi-version](guide/multi-version.md) section for how to publish older docs version

## Single version site

If you don't want to use multi-version support at all then:

```
mkdocs.publish.docPath = ''  // or null 
``` 

This way, mkdocs site will always be published at the root (in case of publish it will always replace 
previous site version).

## Publication

When documentation site will be ready, you will need to call `mkdocksPublish` in order to 
publish it to github pages (default).

If your repo is `https://github.com/me/my-project` then documentation will be
available as `https://me.github.io/my-project/`. 

Published index.html at the root will immediately redirect you to the actual version:
`https://me.github.io/my-project/1.0.0/`.

See more about publication customization in [publication](guide/publication.md) section.
It also describes how to publish additional parts with documentation site (like javadoc).

## Pip

See [pip](guide/pip.md) section if you need to change mkdocs version, use custom theme or plugin.