# Configuration

!!! important
    Plugin based on [python plugin](https://github.com/xvik/gradle-use-python-plugin) which manage all 
    python-related staff like virtualenv creation, pip installation and python executions.
    For python-related configurations refer to [python plugin documentation](https://xvik.github.io/gradle-use-python-plugin/{{ gradle.pythonPlugin }}/) 

!!! tip
    For docker configuration see [python plugin documentation](https://xvik.github.io/gradle-use-python-plugin/{{ gradle.pythonPlugin }}/guide/docker)  

!!! important
    There are two plugin versions:

    - `ru.vyarus.mkdocs` - full plugin
    - `ru.vyarus.mkdocs-build` - without publication tasks (lightweight)

    Both versions are equal in functionality, except publication stage.

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
    // optional variables declaration (to bypass gradle data to docs)
    extras = [:]
    // dev server port (mkdocsServe task), overrides dev_addr from mkdocs.yml
    devPort = 3000
    
    publish {
        // publication sub-folder (by default project version)
        docPath = '$version'
        // generate versions.json file for versions switcher
        // [used by publication task]
        generateVersionsFile = true
        // specify path to or URL to existing versions.json file in order to update it (add new version); 
        // executed by mkdocsBuild, suitable when git publication not used
        existingVersionsFile
        // custom version name shown in version switcher (by default version folder name)
        versionTitle = '$docPath'
        // one or more alias folders to copy generated docs to (e.g. 'latest' or 'dev')
        versionAliases = []
        // specify versions to hide in version switcher without removing actual version folders
        hideVersions = []
        // when enabled it would automatically detect old bugfix versions and hide them 
        // (because documentation for bugfix versions usually the same)
        // e.g. for versions 1.1.0, 1.1.1, 1.2.0, 1.2.0.1, 1.2.0.2 
        // it would hide 1.1.0 and 1.2.0.1 
        hideOldBugfixVersions = false
        // generate index.html' for root redirection to the last published version 
        rootRedirect = true
        // allows changing root redirection to alias folder instead of exact version 
        rootRedirectTo = '$docPath'
        // publish repository uri (bu default the same as current repository)
        // [used by publication task]
        repoUri = null
        // publication branch
        // [used by publication task]
        branch = 'gh-pages'
        // publication comment
        // [used by publication task]
        comment = 'Publish $docPath documentation'
        // directory publication repository checkout, update and push
        // [used by publication task]
        repoDir = '.gradle/gh-pages'
    }
}
```

!!! tip
    Options marked as `[used by publication task]` used ONLY during publication and so
    would be ignored when build-only plugin version (`ru.vyarus.mkdocs-build`) used.
    
    Not marked publication propeties used by `mkdocsBuild`

By default:

- All documentation sources located in `src/doc` (and `mkdocsInit` task generate stubs there)
- `mkdocsBuild` task will build site into `build/mkdocs`
- Current project version is used as documentation folder (`build/mkdocs/$version`)
- Github repository is assumed by default, so publication will be performed into `gh-pages` branch (where github will automatically detect it)
- Variables plugin is not configured. See [variables section](vars.md) for details.
    
!!! note
    In order to include something else into published docks (e.g. javadoc) see [publication](publication.md).

## Single version site

If you don't want to use multi-version support at all then:

```
mkdocs.publish.docPath = ''  // or null 
``` 

This way, mkdocs site will always be published at the root (in case of publish it will always replace 
previous site version).
    
## Docs as module

Example of moving documentation into separate gradle module: 

```groovy
plugins {
    id 'ru.vyarus.mkdocs' version '{{ gradle.version }}' apply false                                
}

version = '1.0-SNAPSHOT'

project(':doc') {
    apply plugin: 'ru.vyarus.mkdocs' 
    
    mkdocs {
        sourcesDir = 'src'
    }
}
```

Default docs location simplified to simple `src` because of no other sources in this module.

If we call `:doc:mkdocsInit` it will generate documentation stubs like this:

Project structure:

```
/
    /doc/
        src/
            docs/
            ...
            index.md
        mkdocs.yml
build.gradle
settings.gradle            
```

For simplicity, gradle configuration for `doc` module is declared in the main file,
but it could be declared inside doc's own build.gradle.