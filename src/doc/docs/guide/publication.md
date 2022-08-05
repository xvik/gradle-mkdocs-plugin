# Publication

!!! note
    Plugin does not use [mkdocs publication](http://www.mkdocs.org/#deploying), because it does not support
    multi-versioning. Instead, [git-publish](https://github.com/ajoberstar/gradle-git-publish) plugin is used for publication.

!!! note
    Mkdocs-material [suggests mike tool usage](https://squidfunk.github.io/mkdocs-material/setup/setting-up-versioning/) for publication.
    Gradle plugin implements [absolutely the same](multi-version.md#doc-version-switcher) workflow as mike, but much easier
    customizable (as gradle plugin). Material theme would still be able to show version switcher because
    plugin generates required versions.json file.

By default, no configuration is required. Only project itself must be published to git so that plugin could calculate default url 
(or `mkdocs.publish.repoUrl` manually specified).

On the first `mkdocsPublish` task call:

* `gh-pages` branch will be created in the same repo
* built site pushed to gh-pages repository branch 

Later `mkdocsPublish` task calls will only remove current version folder (replace with the new one)
or publish completely new version only.

You can find actual `gh-pages` branch inside `.gradle/gh-pages` (this checkout is used for publishing). 
Gradle folder is used to cache repository checkout because eventually it would contain many versions
and there is no need to checkout all of them each time (folder could be changed with `mkdocs.publish.repoDir`).

## Authentication

By default, git-publish will ask credentials with a popup (swing). Even if github pages are published on the same repo,
the repo is checked out into different folder and so current repository credentials can't be used automatically.  

You can specify credentials as:

* Environment variables: `GRGIT_USER` (could be [github token](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/)), `GRGIT_PASS`
* System properties: `org.ajoberstar.grgit.auth.username` (could be github token), `org.ajoberstar.grgit.auth.password`
* Ssh key properties: `org.ajoberstar.grgit.auth.ssh.private` (path to key), `org.ajoberstar.grgit.auth.ssh.passphrase`

See details in [grgit docs](http://ajoberstar.org/grgit/grgit-authentication.html).

Plugin will automatically bind gradle properties `org.ajoberstar.grgit.auth.*` to system properties (just before `gitPublishReset`).
This allows you to define credentials as global gradle properties in `~/.gradle/gradle.properties`:

```properties
org.ajoberstar.grgit.auth.username=user
org.ajoberstar.grgit.auth.password=pass
```

For testing, you can define properties inside gradle script:

```groovy
ext['org.ajoberstar.grgit.auth.username'] = 'user'
ext['org.ajoberstar.grgit.auth.password'] = 'pass'  
```

## Publish additional resources

If you want to publish not only generated site, but something else too then configure
[git-publish](https://github.com/ajoberstar/gradle-git-publish) plugin to include additional content.

For example, to include javadoc:

```groovy
gitPublish.contents {
    from(javadoc) {
        // need to use resolveDocPath because by default it's a template 
        into "${mkdocs.resolveDocPath()}/javadoc"
    }
}

// dependency will NOT be set automatically by copySpec above
gitPublishReset.dependsOn javadoc
```

!!! note
    When [multi-version publishing](multi-version.md) is not used (`mkdocs.publish.docPath` set to null) don't use
    `${mkdocs.resolveDocPath()}/` prefix because `resolveDocPath()` will return null in this case.
    Instead, use static target folder name: `into "javadoc"`. 

With this configuration, calling `mkdocsPublish` will publish generated mkdocs site
with extra `javadoc` folder inside (you can put relative link to it inside documentation).

## Advanced publishing configuration

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
    - if required, alias folders would be generated too (by copying generated version content)
2. `gitPublishReset` clones gh-pages repo (by default, into `.gradle/gh-pages`) or creates new one
    - cleanup repo according to `gitPublish.preserve` (by default, `.gradle/gh-pages/$version/` folder removed only)
3. `mkdocsVersionsFile` generates versions.json file based on version folders checked out from gh-pages repository
   (file generated in `$mkdocs.buildDir`)
4. `gitPublishCopy` copies everything according to `gitPublish.contents` (by default, everything from `build/mkdocs`)
5. `gitPublishCommit`, `gitPublishPush` - commit changes and push to gh-pages repository (by default, `gh-pages` branch in current repo)

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

## site_url

[`site_url`](http://www.mkdocs.org/user-guide/configuration/#site_url) configuration defined in mkdocs.yml should point to the site root. It may be github pages or some custom domain.
Setting affect home icon link, page metadata and links in generated sitemap.xml.

When multi-version publishing used (by default), this url must point to documentation version folder
(otherwise links will be incorrect in the sitemap). To avoid manual changes, just configure *root site url* (e.g. `http://myroot.com/`) and 
plugin will *automatically* change site_url before `mkdocsBuild` (for example, to `http://myroot.com/1.0/`; see build log - it will show updated url). 
Config is reverted back after the task, so you will not have to commit or revert changes.

If `site_url` option is not defined at all (or multi-version publishing is not enabled) then
config will not be changed.

You can disable automatic configuration changes:

```groovy
mkdocs.updateSiteUrl = false
```

Note that `mkdocsServe` is not affected (will generate with the original site_url) because it is
not important for documentation writing (you can always call `mkdocsBuild` and validate urls correctness).
