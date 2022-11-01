# Example configurations

Common configurations cheat-sheet.

## Single version

```groovy
mkdocs.publish.docPath = ''
```

Documentation always published into root folder:

```
gh-pages root/
    generated docs
```

Very specific option, suitable for some site, blog etc. 
Project documentation requires multi-version publication so users could 
always consult with old docs.

## Simple multi-version

```java
mkdocs.publish {
    docPath = '$version'  
    rootRedirect = true
}
```

Documentation published into sub-folder. Only this sub folder would be updated on
publication - other folders remain untouched.

!!! tip
    It's better to specify exact version in `docPath`, because usually documentation site being
    updated after release and with default `$version` you would publish snapshot version instead.
    
    ```
    docPath = '1.0'  
    ```

Each publication would publish:

```
gh-pages root/
    version/
        generated docs
    index.html
    versions.json    
```

Root index.html required to redirect use to the latest version when it opens site root.

To activate [version switcher component](https://squidfunk.github.io/mkdocs-material/setup/setting-up-versioning/#versioning)
add in mkdocs.yml:

```yaml
extra:
  version:
    provider: mike
```

(component requires only versions.json file generated at site root)

## Using latest alias

It is quite common for projects to allow user to put `latest` in doc url
instead of exact version: this way user could put link in his own code/docs which
would always lead to up-to-date page.

```groovy
mkdocs.publish {
    docPath = '1.0'  
    rootRedirect = true
    versionAliases = ['latest']
}
```

Everything as before, but on publication new folder would appear:

```
gh-pages root/
    version/
        generated docs
    latest/
        generated docs    
    index.html
    versions.json    
```

## Root redirect to alias

In the example above root is still redirecting to exact version. To redirect into `latest` alias: 

```groovy
mkdocs.publish {
    docPath = '1.0'  
    rootRedirect = true
    rootRedirectTo = 'latest'
    versionAliases = ['latest']
}
```

And now site root will lead to `latest` directory.
See [plugin site](https://xvik.github.io/gradle-mkdocs-plugin/) as an example.

## Reference project version in docs

It is a very common need to reference project version instead of changing it everywhere before each release.
It is possible with [variables support](guide/vars.md).

In mkdocs.yml add plugins:

```yaml
plugins:
  - search
  - markdownextradata
```

In build.gradle declare variables:

```groovy
mkdocs {
    extras = [
            'version': "${-> project.version}"
    ]
}
```

Now you can reference version as:

```
{% raw %}
{{ gradle.version }}
{% endraw %}
```

But, as mentioned before, it is more common to put raw version instead of relying on project version (because release docs could be updated after release):

```groovy
mkdocs {
    publish.docPath = '1.0'
    extras = [
            'version': '1.0'
    ]
}
```

Or, to avoid duplication:

```groovy
mkdocs {
    extras = [
            'version': '1.0'
    ]
    publish.docPath = mkdocs.extras['version']
}
```

## Old version publish

You can always change and re-publish any old documentation version: it works
the same as with new version publication.

EXCEPT:

* **Remember** to switch off root redirection with `mkdocs.publish.rootRedirect = false`:
  otherwise root index.html would be overridden and site root would point to wrong docs version
* **Remember** to remove aliases (remove `mkdocs.publish.versionAliases`):
  otherwise alias folders would be overridden (e.g. latest would contain old version)
* **Use** `mkdocsVersionsFile` task to validate old version correctness:
  It would try to warn you if it can detect more recent versions override

## Incremental versions update

When plugin used only for documentation generation and publication task is not used, then
versions.json file can be updated incrementally (by adding new version on each release):

```groovy
mkdocs.publish {
    docPath = '1.0'  
    rootRedirect = true
    rootRedirectTo = 'latest'
    versionAliases = ['latest']
    existingVersionsFile = 'https://xvik.github.io/gradle-mkdocs-plugin/versions.json'
}
```

On `mkdocsBuild` it would load remote json (it might be local path) and add new version, so build dir would contain:

```
build/mkdocs/
    /1.0/            - mkdocs site
    /latest/         - alias (copy)
    index.html       - redirect to 'latest'
    versions.json    - updated remote versions file    
```

After that built folder might be simply uploaded, for example into some ftp with other versions
(append).