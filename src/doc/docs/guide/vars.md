# Variables

You can declare variables as:

```groovy
mkdocs {
    extras = [
            'version': "${-> project.version}",
            'something': 'something else'
    ]
}
```

!!! warning
    You'll have to use lazy evaluation syntax for sensitive properties. In the example above
    actual version (`"${-> project.version}"`) would be resolved only just before mkdocs task execution.
    
    As an alternative, you can declare some properties just before task execution:
    ```groovy
    mkdocsBuild.doFirst {
        extras.prop = ... some value calculation
    }
    ```
    But this will apply property only for one task (e.g. mkdocsServe will not see this property)!

Variables applied with [markdownextradata](https://github.com/rosscdh/mkdocs-markdownextradata-plugin) mkdocs plugin.
The plugin is installed, but not activated by default. To use variables it must be enabled:

```yaml
plugins:
  - search
  - markdownextradata
```

(search plugin is enabled by default when plugins section is not declared and so have to be manually specified)

Now you can use variables in markdown files:

```
{% raw %}
{{ gradle.version }} and {{ gradle.something }}
{% endraw %}
```

!!! note 
    Variables will work for all mkdocs tasks (`MkdocsTask`), including `mkdocsServe`!

!!! tip
    All generated variables are also printed in console (for reference).

## Keys

Plugin will automatically replace ' ' and '-' in variable keys into '_'.

For example:

```groovy
mkdocs {
    extras = [
            'something-else other': 'something else'
    ]
}
```

Will appear as `gradle.something_else_other` variable.

## Automation example

Variables addition may be scripted. For example, mkdocs plugin's build use this to 
store used pip modules versions as vars:

```groovy
afterEvaluate {
    // iterating over modules declared with python.pip
    python.modules.each {
        def mod = it.split(':')
        // storing module name as-is: plugin will auto correct '-' to '_'
        mkdocs.extras[mod[0]] = mod[1]
    }
}
```

And version reference in docs looks like:

```markdown
* [mkdocs:{% raw %} {{ gradle.mkdocs }} {% endraw %}](https://pypi.python.org/pypi/mkdocs)
* [mkdocs-material:{% raw %} {{ gradle.mkdocs_material }} {% endraw %}](https://pypi.python.org/pypi/mkdocs-material)
```

`afterEvaluate` block is not required in this case, but it's usually safer to use it 
to avoid "configuration property not yet ready" errors.

## Show template syntax in doc

If you want to prevent replacing variable, for example:

```
{% raw %}
{{ not_var }}
{% endraw %}
```

Then simply apply text "as variable" :

```
{{ '{{ \'{{ not_var }}\' }}' }}
```

or use [jinja raw block syntax](https://jinja.palletsprojects.com/en/2.11.x/templates/#escaping)

```
{{ '{% raw %}' }}
{{ '{{ not_var }}' }}
{{ '{% endraw %}' }}
```

## How it works

When variables declared (`mkdocs.extras`), plugin will generate a special data file before mkdocs task execution:

```
[mkdocs.yml location]/docs/_data/gradle.yml
```

Markdownextradata plugin loads all yaml files in `_data` directory by default and so it would
recognize and load gradle-declared properties automatically.

After task execution, file is removed (because it located inside sources).

You can declare additinoal (not gradle-related) variables directly in mkdocs.yml's `extra` section
or using additinoal `_data` files: see [plugin documentation](https://github.com/rosscdh/mkdocs-markdownextradata-plugin#use-extra-variables-in-your-markdown-files).

!!! tip
    To update plugin version (for example, in case of bugs): 
    ```groovy
    python.pip 'mkdocs-markdownextradata-plugin:0.2.5'
    ```

## Potential problem on linux

Markdownextradata plugin requires PyYaml 5.1 or above. If you use older version, you may face 
the following error:

```
 ...
    File "/home/travis/.local/lib/python3.6/site-packages/markdownextradata/plugin.py", line 90, in on_pre_build
     if filename.suffix in [".yml", ".yaml"]
 AttributeError: module 'yaml' has no attribute 'FullLoader'
```

To workaround it either upgrade global PyYaml:

```
pip3 install --ignore-installed pyyaml
```

(`--ignore-installed` required!)

Or specify exact pyyaml version for installation inside environment:

```groovy
python.pip 'pyyaml:5.4.1'
```

!!! note
    PyYaml module not declared by default because it's a system module, installed with python.
    It may require additional packages for installation (`python3-dev`) and so it could 
    cause more problems if it would be updated by default.
    But, this problem affects only old python versions, and you may never face this.

### Travis ci

Pyyaml problem might be faced on travis: even `bionic` shipped with python 3.6 brings old pyyaml version.

Workaround:

```yaml
language: java
dist: bionic
addons:
  apt:
    packages:
      - "python3"
      - "python3-pip"
      - "python3-setuptools"

matrix:
  include:
    - jdk: openjdk8
    - jdk: openjdk11

before_install:
  - chmod +x gradlew
  - pip3 install --upgrade pip
  # markdownextradata plugin requires at least pyyaml 5.1
  - pip3 install --ignore-installed pyyaml
```