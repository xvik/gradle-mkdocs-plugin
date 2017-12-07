# Gradle Mkdocs plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](http://www.opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/travis/xvik/gradle-mkdocs-plugin.svg)](https://travis-ci.org/xvik/gradle-mkdocs-plugin)

### About

Generates project documentation with [Mkdocs](http://www.mkdocs.org/) python tool. 

[Example documentation](http://xvik.github.io/dropwizard-guicey/) (generated and published with the plugin). 

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
[use-python](https://gihub.com/xvik/gradle-use-python-plugin)

### Setup

Releases are published to [bintray jcenter](https://bintray.com/vyarus/xvik/gradle-mkdocs-plugin/), 
[maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-mkdocs-plugin) and 
[gradle plugins portal](https://plugins.gradle.org/plugin/ru.vyarus.mkdocs).


[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/gradle-mkdocs-plugin.svg?label=jcenter)](https://bintray.com/vyarus/xvik/gradle-mkdocs-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-mkdocs-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-mkdocs-plugin)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-mkdocs-plugin:1.0.0'
    }
}
apply plugin: 'ru.vyarus.mkdocs'
```

OR 

```groovy
plugins {
    id 'ru.vyarus.mkdocs' version '1.0.0'
}
```

#### Python

**Requires installed python** 2.7 or 3.3 and above.
Required pip modules will be installed automatically (make sure exact versions are installed).

Make sure python and pip are installed:

```bash
python --version
pip --version
```

On most *nix distributions python is already installed. On windows 
[download and install](https://www.python.org/downloads/windows/) python manually or use 
[chocolately](https://chocolatey.org/packages/python/3.6.3) (`choco install python`)

See [gradle-use-python-plugin](https://gihub.com/xvik/gradle-use-python-plugin) for gradle python 
integration configuration (if required).

### Usage

By default, documentation source assumed to be in `src/doc`.

To change location: 

```groovy
mkdocs {
    sourcesDir = 'docs' 
}
```

Call `mkdocsInit` task to generate initial site version into `src/doc` (default):

```
src/doc/
    docs/               - documentation source
        ...
        index.md
    mkdocs.yml          - site configuration
```

Call `mkdocsServe` task to start live reload server: look generated default docs ([http://127.0.0.1:8000/](http://127.0.0.1:8000/)).

### Initial site configuration

Note: plugin does not use [mkdocs new](http://www.mkdocs.org/#getting-started) command for site generation: custom template used 
with pre-configured plugins and enabled material theme.

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
 
### Writing

Yaml configuration `pages` section declares your documentation structure. Pages inside `docs` folder
may be structured as you want.

To add new page simply add new markdown file (page.md) and add reference to it in `pages` config.   

Read:
* Mkdocs [getting started guide](http://www.mkdocs.org/#getting-started).
* Mkdocs-material [extensions docs](https://squidfunk.github.io/mkdocs-material/getting-started/#extensions).

### Building

By default, `mkdocsBuild` task will generate (suppose project version is '1.0-SNAPSHOT'):

```
build/mkdocs/
    /1.0-SNAPSHOT/    - mkdocs site
    index.html        - redirect to correct site
```

Plugin is configured for multi-version documentation publishing: each version is in it's own folder
and special `index.html` at the root will redirect to the latest version (when published).

Everything in `build/mkdocs/` is assumed to be published into github pages. 

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

When older documentation version needs to be updated switch off redirection `index.html` generation
(so it would not override redirection to the latest version):

```groovy
mkdocs.publish {
    docPath = '0.9'  
    rootRedirect = false  // site root must still redirect to '1.0' (assume it's already published)
}
``` 

Will build:

```
build/mkdocs/
    /0.9/    - mkdocs site for old version
```

#### Publication layouts

You may define whatever layout you want, e.g.:

```
mkdocs.publish.docPath = 'en/1.0/'  
``` 

Here generated site will be published into `/en/1.0/` folder (not just version) and 
index.html generated at the root with correct redirection.

##### Single version site

If you don't want to use multi-version support at all then:

```
mkdocs.publish.docPath = ''  // or null 
``` 

This way, mkdocs site will always be published at the root (in case of publish it will always replace 
previous site version).

#### site_url

[`site_url`](http://www.mkdocs.org/user-guide/configuration/#site_url) configuration defined in mkdocs.yml should point to the site root. It may be github pages or some custom domain.
Setting affect home icon link, page metadata and links in genearted sitemap.xml.

When multi-version publishing used (by default), this url must be modified to match target documentation folder
(otherwise links will be incorrect)). To avoid manual changes, just configure *root site url* and 
plugin will *automatically* change site_url before `mkdocsBuild` (config is reverted back after the task, so
you will not have to commit or revert changes).

If `site_url` option is not defined at all (or multi-version publishing is not enabled) then
config will not be changed.

You can disable automatic configuration changes:

```groovy
mkdocs.updateSiteUrl = false
```

Note that `mkdocsServe` is not affected (will generate with the original site_url) because it is
not important for documentation writing (you can always call `mkdocsBuild` and validate urls correctness).

### Configuration

Configuration properties with default values:
 
```groovy
mkdocs {
    // mkdocs sources
    sourcesDir = 'src/doc'    
    // strict build (fail on build errors)
    strict = true    
    // target build directory (publication root)
    buildDir = 'build/mkdocs'
    // automatically update site_url in mkdocs.yml before mkdocsBuild
    updateSiteUrl = true
    
    publish {
        // publication sub-folder (by default project version)
        docPath = '$version'        
        // generate index.html' for root redirection to the last published version 
        rootRedirect = true
        // publish repository uri (bu default the same as current repository)
        repoUri = null
        // publication branch
        branch = 'gh-pages'
        // publication comment
        comment = 'Publish $docPath documentation'
        // directory publication repository checkout, update and push
        repoDir = '.gradle/gh-pages'
    }
}
```

### Publication

Plugin does not use [mkdocs publication](http://www.mkdocs.org/#deploying), because it does not support
multi-versioning. Instead, [git-publish](https://github.com/ajoberstar/gradle-git-publish) plugin is used for publication.

By default, no configuration is required. Only project itself must be published to git so that plugin could calculate default url 
(or mkdocs.publish.repoUrl manually specified).

On the first `mkdocksPublish` task call:

* `gh-pages` branch will be created in the same repo
* built site pushed to gh-pages repository branch 

Later `mkdocsPublish` task calls will only remove current version folder (replace with the new one)
or publish completely new version only.

You can find actual `gh-pages` branch inside `.gradle/gh-pages` (this checkout is used for publishing). 
Gradle folder is used to cache repository checkout because eventually it would contain many versions
and there is no need to checkout all of them each time (folder could be changed with `mkdocs.publish.repoDir`).

#### Publish additional resources

If you want to publish not only generated site, but something else too then configure
[git-publish](https://github.com/ajoberstar/gradle-git-publish) plugin to include additional content.

For example, to include javadoc:

```groovy
gitPublish.contents {
    from(javadoc) {
        // need to use resolveDocPath because by default it's a template 
        into "\${mkdocs.resolveDocPath()}/javadoc"
    }
}

// dependency will NOT be set automatically by copySpec above
gitPublishReset.dependsOn javadoc
```

With this configuration, calling `mkdocsPublish` will publish generated mkdocs site
with extra `javadoc` folder inside (you can put relative link to it inside documentation).

##### Advanced publishing configuration

To be able to configure advanced cases, you need to understand how everything works in detail.

Here is how [git-publish](https://github.com/ajoberstar/gradle-git-publish) plugin is configured by default:

```groovy
gitPublish {

    repoUri = mkdocs.publish.repoUri
    branch = mkdocs.publish.branch
    repoDir = file(mkdocs.publish.repoDir)
    commitMessage = mkdocs.publish.comment

    contents {
        from("${mkdocs.buildDir}")
    }

    if (multi_version_publish) {
        preserve {
            include '**'
            exclude "${mkdocs.publish.docPath}/**"
        }
    }    
}
```

Customized tasks dependency chain:
```
mkdocsBuild <- gitPublishReset <- gitPublishCopy <- gitPublishCommit <- gitPublishPush <- mkdocsPublish
```

Publication process:

1. `mkdocsBuild` build site into  `$mkdocs.buildDir/$mkdocs.publish.docPath` (by default, `build/mkdocs/$version/`)
    - root redirect `index.html` generated (by default, `build/mkdocs/index.html`)
1. `gitPublishReset` clones gh-pages repo (by default, into `.gradle/gh-pages`) or creates new one
    - cleanup repo according to `gitPublish.preserve` (by default, `.gradle/gh-pages/$version/` folder removed only) 
1. `gitPublishCopy` copies everything according to `gitPublish.contents` (by default, everything from `build/mkdocs`)
1. `gitPublishCommit`, `gitPublishPush` - commit changes and push to gh-pages repository (by default, `gh-pages` branch in current repo)

You can configure additional folders for publication with `contents` (as shown above with javadoc) 
and cleanup extra directories with `preserve` configuration. For example:

```groovy
gitPublish {
    contents {
        from 'build/custom-dir' {
            into 'custom-dir'        
        }
    }
    
    preserve {
        exclude 'custom-dir'
    }
}
```

Here extra `build/custom-dir` directory added for publication (into `custom-dir`)
and previous `custom-dir` folder (already committed) will be removed before publication.

   
### Pip modules

Plugin will install by default the following pip modules:

* [mkdocs:0.17.2](https://pypi.python.org/pypi/mkdocs)
* [mkdocs-material:2.2.1](https://pypi.python.org/pypi/mkdocs-material)
* [pygments:2.2.0](https://pypi.python.org/pypi/Pygments)
* [pymdown-extensions:4.6](https://pypi.python.org/pypi/pymdown-extensions)

If you want to use other python modules (e.g. other theme):

```groovy
python.pip 'other-module:12', 'and-other:1.0'
```

Also, you can override default modules versions:

```groovy
python.pip 'mkdocs:18.0'
```

And even downgrade:

```groovy
python.pip 'mkdocs:16.0'
```

By default, `pipInstall` prints all global modules into console. If you want to disable it use:

```groovy
python.showInstalledVersions = false
```

You can use `pipUpdates` task to check if newer module versions are available.

See [gradle-use-python-plugin](https://github.com/xvik/gradle-use-python-plugin) for other configuration options.

### Custom Mkdocs task

If you need to use custom mkdocs command:

```groovy
task doSomething(type: MkdocsTask) {
    command = '--help'
}
```

`:doSomething` task call will do: `python -m mkdocs --help`.  

### Might also like

* [quality-plugin](https://github.com/xvik/gradle-quality-plugin) - java and groovy source quality checks
* [animalsniffer-plugin](https://github.com/xvik/gradle-animalsniffer-plugin) - java compatibility checks
* [pom-plugin](https://github.com/xvik/gradle-pom-plugin) - improves pom generation
* [java-lib-plugin](https://github.com/xvik/gradle-java-lib-plugin) - avoid boilerplate for java or groovy library project
* [github-info-plugin](https://github.com/xvik/gradle-github-info-plugin) - pre-configure common plugins with github related info


---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
