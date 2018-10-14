# Tasks

## Init

`mkdocsInit` task generate initial site version into `src/doc` (or custom location).

!!! note
    Plugin does not use [mkdocs new](http://www.mkdocs.org/#getting-started) command for site generation: custom template used 
    with pre-configured plugins and enabled material theme.

Task will do nothing if target folder exists and not empty. 

## Dev server

`mkdocsServe` task start live reload server (used during development) on 
 [http://127.0.0.1:8000/](http://127.0.0.1:8000/).

!!! warning 
    Python process will not be killed after you stop gradle execution (search and kill python process manually). This is a [known gradle problem](https://github.com/gradle/gradle/issues/1128) 
    and the only known workaround is to start task without daemon: `gradlew mkdocsServe --no-daemon`. 
    Another alternative is to start serve command directly: copy console command from task execution log and use it directly. 

## Build

`mkdocsBuild` task will generate (suppose project version is '1.0-SNAPSHOT'):

```
build/mkdocs/
    /1.0-SNAPSHOT/    - mkdocs site
    index.html        - redirect to correct site
```

Plugin is configured for multi-version documentation publishing: each version is in it's own folder
and special `index.html` at the root will redirect to the latest version (when published).

Everything in `build/mkdocs/` is assumed to be published into github pages. 

!!! tip
    As documentation is often updated for already released version, it makes sense to define 
    current version manually (or define it when you need to publish to exact version):
    
    ```groovy
    mkdocs.publish.docPath = '1.0'
    ```

### Update old documentation

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

## Publish

`mkdocsPublish` calls `mkdocsBuild` and publish contents of `build/mkdocs/` into git repo
(by default, `gh-pages` branch in current repo).

See [publication](publication.md) for more details.

## Custom Mkdocs task

If you need to use custom mkdocs command:

```groovy
task doSomething(type: MkdocsTask) {
    command = '--help'
}
```

!!! note
    Full task package is not required because `MkdocsTask` is actually a property, regustered
    by plugin with the full class name in value. 
    
`:doSomething` task call will do: `python -m mkdocs --help`.  
