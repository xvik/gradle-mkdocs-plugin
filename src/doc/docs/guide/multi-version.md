# Multi-version documentation

By default, plugin assume multi-version documentation publishing. 

Configuration, responsible for versioning:

```groovy
mkdocs.publish {
    docPath = '$version'  
    rootRedirect = true 
    rootRedirectTo = '$docPath'
    
    versionTitle = '$docPath'
    versionAliases = []
    hideVersions = []
    hideOldBugfixVersions = false
    generateVersionsFile = true
}
``` 

!!! tip
    Usually it is more handy to manually set current doc version and not rely on project version because 
    often documentation for currently released version is updated multiple times after release.

!!! important
    Mkdocs-material [suggests mike tool usage](https://squidfunk.github.io/mkdocs-material/setup/setting-up-versioning/) for publication.
    Gradle plugin implements absolutely the same workflow as mike, but much easier
    customizable (as gradle plugin). Material theme would still be able to show version switcher because
    plugin generates required versions.json file.

## Workflow

Usually you work on documentation for current version only. When new version released - you publish
new documentation version and continue evolving it. Old version stays published for the legacy users.

### Older version update

When older documentation version needs to be updated switch off redirection `index.html` generation
(so it would not override redirection to the latest version):

```groovy
mkdocs.publish {
    docPath = '0.9'  
    rootRedirect = false  // site root must still redirect to '1.0' (assume it's already published)
}
``` 

Will build (without root index.html):

```
build/mkdocs/
    /0.9/    - mkdocs site for old version
```

Also, do not use `versionAliases` when publishing old version because it may override
more recent docs version. Plugin would try to warn you in such cases:

```java
WARNING: Publishing version '1.0' is older then the latest published '1.1' and the following overrides might not be desired: 
	root redirect override to '1.0'
	existing alias 'latest' override
	existing alias '1.x' override
```

!!! important
    This warning is produced by `mkdocsVersionsFile` file and only when versions.json file 
    generation is not disabled. This check can't be done under `mkdocsBuild` because publishing repository is required
    for validation.

    So please, when releasing an **old** version use `mkdocsVersionsFile` to see all possible
    warnings before actual publication.
    
## Publication layouts

You may define whatever layout you want, e.g.:

```
mkdocs.publish.docPath = 'en/1.0/'  
``` 

Here generated site will be published into `/en/1.0/` folder (not just version) and 
index.html generated at the root with correct redirection.

!!! warning
    If you want to use [version switcher](#doc-version-switcher) then you should not use 
    long version paths, because mkdocs-material would look for versions file [only at one level up](https://github.com/squidfunk/mkdocs-material/blob/87df85def83535b54dc74ea7d86e8c41aa9db97a/src/assets/javascripts/integrations/version/index.ts#L46). 

## Aliases

It is possible to publish version not only into version folder, but also into
aliased folders.

Could be useful for:

- Publishing the latest documentation under `latest` alias, so users could always
  reference the latest docs with the same link.
- Publishing docs for developing version under `dev` alias, so users could easily find dev docs.
- Serving the latest (patch) version for some mojor version: e.g. `5.x` alias could serve the latest
  published bugfix.

For example, to add `latest` alias:

```groovy
mkdocs.publish {
    docPath = '1.0'  
    rootRedirect = true
    versionAliases = ['latest']
}
``` 

Will build:

```
build/mkdocs/
    /0.9/    
    /latest/
    index.html
```

!!! note
    Alias folder contains a *copy* of generated documentation, which means
    that sitemap links would lead to path of exact version.

If same version is re-published - aliases would be correctly updated too.

It is also possible to *redirect root into alias* instead of exact version with `rootRedirectTo` option:

```groovy
mkdocs.publish {
    versionAliases = ['latest']
    rootRedirectTo = 'latest'
}
```

!!! tip
    In case of root redirection to alias it is better to enable version switcher to clearly show what version
    is active now (otherwise it would be not obvious)


## Doc version switcher

Version switcher might be enabled in mkdocs.yml [exactly as described in docs](https://squidfunk.github.io/mkdocs-material/setup/setting-up-versioning/#versioning):

```yaml
extra:
  version:
    provider: mike
```

!!! important
    You don't need [mike](https://github.com/jimporter/mike) itself! 

!!! important
    You will not see switcher under `mkdocsServe` command, but if you call `mkdocsVersionsFile` (which would also call
    `mkdocsBuild`), and manually open new version it would show switcher with all versions (using generated versions.json) 

Mkdocs-material requires only `versions.json` file stored at docs root. Plugin would automatically
generate such file (following mike syntax):

- Plugin verifies actually existing directories in gh-pages repository and would
  add them to generated versions file. So if you already have many versions published, just publish
  new version with enabled versions support and you'll see all of them in the version switcher.
- Theme folders are detected by using `\d+(\..*)?` regexp (version folder must start with a number)
  and it must contain 404.html file.
- Deeper versions folders are also supported: e.g. if `mkdocs.publish.docPath = 'en/1.0/'` then
  `en/1.0' folder would be recognized as version
- Existing records in versions.json file are preserved for found version folders.
    - You can modify file manually (e.g. to modify version title) and it will not be overridden on next publication
    - You can manually remove version folder in repository and on next publication versions.json would be corrected
- If aliases used, they would be correctly updated (e.g. `latest` removed from previous latest version.)

If you do not want to generate versions file:

`mkdocs.publish.generateVersionsFile = false`

To customize version title (shown in dropdown selection) use:

`mkdocs.publish.versionTitle = '1.0 (important fix)'`

### Incremental versions

There is an alternative way for versions.json file generation for cases when publishing task
is not used (e.g. when lightweight `ru.vyarus.mkdocs-build` plugin used).

The idea is to use existing versions file and add new version into it (in contrast, git based publication
could detect removed version folders and remove versions from file accordingly).

!!! tip
    Version aliases would be also correctly processed.

To enable incremental versions generation, specify current versions file location:

```groovy
mkdocs.publish.existingVersionsFile = 'path/to/actual/versions.json'
```

You can also use a URL (http, ftp or anything that URL could handle):

```groovy
mkdocs.publish.existingVersionsFile = 'https://xvik.github.io/gradle-mkdocs-plugin/versions.json'
```

When file specified, `mkdocsBuild` would load current file and apply new version there (if required).
Updated versions file would be available in target build directory (same as with git publication).

!!! important
    There will not be an error if file not found or due to download error - instead plugin
    would consider current file as not existing and would create a fresh versions file.

    This is useful for the first publication.

!!! note
    When git publication used, incremental versions file, generated by mkdocsBuild would be
    overridden by versions file, generated by publishing task. If required, it could
    be disabled with `mkdocs.publish.generateVersionsFile = false`, but keep in mind that 
    publication mechanism is safer to use.

### Hide versions

If you publish documentation for each released version, after some time you may notice too 
many versions in the version selector. Often some versions become not relevant and could be hidden
(it would not be correct to remove old version folder).

For example, suppose we have: 1.0, 1.1.0, 1.1.1, 1.1.2, 1.1.3.
It makes no sense to keep old bugfix versions in the version chooser:

```groovy
mkdocs.publish.hideVersions = ['1.1.0', '1.1.1', '1.1.2']
```

Now version selector would show only '1.0' and '1.1.3' versions, but all hidden versions
would still be accessible by direct link.

!!! important
    Version hiding does not work for [incremental versions](#incremental-versions) 
    (where the current version is just appended into some existing versions file inside mkdocsBuild task). 

For bugfix versions (like in the example above) plugin could hide versions automatically:

```groovy
mkdocs.publish.hideOldBugfixVersions = true
```

!!! note
    Automatic versions hiding does not try to be too smart:
    1. It looks only versions with the same "base" part (same number of dots). 
        E.g. 2.0.0, 2.0.1 or 2.0.0.1, 2.0.0.2, but would not compare 2.0.0 and 2.0.0.1
    2. Ignore versions not ending digit (like 1.0.2.final)

All hidden versions could be seen in the logs. You can always execute `mkdocsVersionsFile`
to verify versions file updates correctness before actual publication.