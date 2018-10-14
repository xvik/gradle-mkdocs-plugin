# Multi-version documentation

By default, plugin assume multi-version documentation publishing. 

Configuration, responsible for versioning:

```groovy
mkdocs.publish {
    docPath = '$version'  
    rootRedirect = true  
}
``` 

!!! tip
    Usually it is more handy to manually set current doc version and not rely on project version because 
    often documentation for currently released version is updated multiple times after release.

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