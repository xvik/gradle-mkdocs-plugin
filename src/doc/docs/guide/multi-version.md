# Multi-version documentation

By default, plugin assume multi-version documentation publishing. 

Configuration, responsible for versioning:

```groovy
mkdocs.publish {
    docPath = '$version'  
    rootRedirect = true 
    
    versionTitle = '$docPath'
    versionAliases = []
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

!!! important
    Keep in mind that alias folder contains a *copy* of generated documentation, which means
    that sitemap or root page link would lead to path of exact version.
    It does not limit usage, just might be unexpected.

If same version is re-published - aliases would be correctly updated too.

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
    You will not see switcher under `mkdocsServe` command, but if you call `mkdocsBuild`, and manually
    open new version it would show switcher with all versions (because versions.json was generated) 

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

