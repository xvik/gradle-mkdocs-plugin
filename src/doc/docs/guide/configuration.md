# Configuration

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
    id 'ru.vyarus.mkdocs' version '2.1.0' apply false                                
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
        mkdocs.yaml
build.gradle
settings.gradle            
```

For simplicity, gradle configuration for `doc` module is declared in the main file,
but it could be declared inside doc's own build.gradle.