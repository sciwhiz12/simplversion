# simplversion

**simplversion** is a simple [Git][git]-based versioning plugin.

This plugin is meant for maintainer-controlled versioning -- releases are only published on tagged commits (without any
uncommitted changes). This gives flexibility to the maintainer in making releases, as opposed to a more automated 
versioning setup which builds and publishes all commits as releases.

The version calculated by this plugin is based on Git tags, and tries its best to be compatible with the [Semantic 
Versioning  specification, v2.0.0][semver]. A notable caveat is that pre-release versions have both the pre-release 
version and the `-SNAPSHOT` suffix in the full version.

## Installation

Add the Modding Inquisition releases maven as a plugin repository to your `settings.gradle`:
```gradle
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name 'ModdingInquisition'
            url 'https://maven.moddinginquisition.org/releases'
        }
    }
}
```

Apply the `dev.sciwhiz12.gradle.simplversion` plugin:
```gradle
plugins {
    id 'dev.sciwhiz12.gradle.simplversion' version '0.1.0' // Replace version with the latest release
}
```

Configure the `versions` extension, and configure your project to use the version from the extension:
```gradle
versions {
    // Defaults included with the plugin, do not include unless customizing
    stripBranchPrefix = true
    incrementPositionIfSnapshot(-2) // Second to last version: #.#.(#).#
    skipIncrementForClassifiers('alpha', 'beta', 'pre', 'rc')
}

// Set project version
version = versions.version
```

_Optionally, if publishing:_ Configure the publication to use the simple version from the extension:
```gradle
publishing {
    publications {
        mavenJava { // Example publication
            // ...
            version = versions.simpleVersion

        }
    }
}
```

## Configuration

By default, the following configuration options are set:

- The branch name is stripped from the raw version.
- No dirty workspace prefix is configured.
- The second-to-last position in the raw version is incremented for snapshot versions.
- The snapshot increment is skipped when the following classifiers (prefixed with a `-`) are found in the raw version: 
  `alpha`, `beta`, `pre`, and `rc`.

  For example, `1.3.0-beta`, `2.4.6-rc2`, and `5.0.0-pre7` will not be incremented, even if it is a snapshot version.

Some terms used in the plugin:

- **raw version**: the version as taken from the nearest reachable tag, after stripping the branch and custom prefixes 
  and removing parts that match the stripping pattern
- **full version** or simply **version**: the raw version and any calculated classifiers
- **classifiers**: when part of the _raw version_, any text after the first hyphen (`-`). when part of the _full version_,
  metadata calculated by the plugin that is separated from the raw version with a hyphen (`-`).

  Classifiers currently used by the plugin are as follows:
    - `-SNAPSHOT` - a Maven-recognized version classifier/suffix that is appended for snapshot versions
    - `+<commit ID>` - the abbreviated commit ID, separated using a plus sign as indicated in [SemVer 2.0.0][semver], when
      not in a detached HEAD state
    - the configured dirty workspace suffix

### `versions` extension

The `versions` project-level extension configures the plugin and provides the calculated version information. The 
extension's class is `VersionExtension`, and the version information is represented by the `VersionInformation` class.
For additional details, consult the documented source code.

### Version stripping

This plugin is configured to strip parts of the raw version based on a mix of configuration from the user and hardcoded
defaults. Stripping is done in four parts:

- First, the **branch name prefix**, separated from the rest of the raw version with a hyphen (`-`) or forward slash (`/`),
  is removed when the extension is configured to do so through the `stripBranchPrefix` boolean property.
- Second, any **custom prefixes**, separated from the rest of the raw version with a hyphen (`-`) or forward slash (`/`), 
  are removed according to the `customPrefixes` string list property.
- Third, the **stripping pattern** is matched against the raw version and any matching parts are removed, as configured
  by the `stripPattern` regular expression pattern property.

  It should be noted that only the parts matched by the pattern are removed. When creating the pattern, users should 
  take care to include any separator between the parts and the rest of the raw version. Users should also use the line 
  start boundary matcher (`^`) if they are stripping out a prefix, to ensure no accidental removals in the middle of the
  raw version happen.
- Fourth, the `v` prefix is removed from the raw version if present. 

### Positional increment

Snapshot versions may be configured to increment an integer position in the raw version. This allows in-development 
versions to attempt to match to the next likely version to be released.

The `snapshotIncrementPosition` integer property (which can be set using the `incrementPositionIfSnapshot` method) 
configures the position to be incremented. Positions are counted by separating the raw version into parts by first 
taking the part of the raw version before any hyphens (`-`), plus signs (`+`), or underscores (`_`), then splitting that
into parts based on the dot/period (`.`).

For example, the version `1.4.5.3-pre3+2` will be first cleaned up into `1.2.5.4`, and then split into four parts: `1`,
`2`, `5`, and `3`.

The values of the property are interpreted as follows:
- A value of zero means no increment will be done.
- A positive value means the position starting from the beginning of the version is incremented. In the above example,
a value of `2` would mean the second position would be incremented: `2` becomes `3`.
- A negative value means the position starting from the end of the version is incremented. In the above example, a value
of `-2` would mean the second-to-last (third) position would be incremented: `5` becomes `6`.

#### Skipping the increment

The `skipIncrement` predicate allows customizing when the above incrementing behavior is skipped for certain snapshot 
versions. The predicate is provided with a `VersionInformation` encapsulating the current version information, with the 
notable caveat that it is not the _final_ version information, as the raw version contains the version before any 
increment and the classifiers do not contain the `-SNAPSHOT` suffix.

For convenience, the `skipIncrementForClassifiers` method is provided to configure a predicate which skips the increment
if any of the given classifiers (along with a `-` prepended before them) are found anywhere in the raw version. For 
example, passing in `pre` would mean it would look for the string `-pre` in any place of the raw version.

### Dirty workspace suffix

The `dirtySuffix` string property configures a suffix to be appended if the current workspace is dirty -- if there are
any uncommitted (including untracked) changes. This may be useful in ensuring that any artifacts built from a dirty
workspace are appropriately marked in their filename, reducing possible confusion. For example, a dirty suffix of 
`.dirty` in a workspace on a commit tagged `1.5.0` and commit `afbc345` would result in a full version of 
`1.5.0+afbc345.dirty`.

## Reading the version programmatically

The version information is available through the extension's `versionInfo` property, as a provider. The provider's value
is calculated once on the first query to the provider , and all properties in the extension are finalized once the 
version is calculated. For convenience, the properties available on the `VersionInformation` object are available on 
the `VersionExtension` as well, which call the provider and retrieve the corresponding property.

- `version` - the full version
- `rawVersion` - the raw version, without any classifiers
- `simpleVersion` - the simple version, which is composed of the raw version and the `-SNAPSHOT` classifier for a 
  snapshot version
- `snapshot` - a boolean for whether it is a snapshot version
- `classifiers` - the classifiers, as a string which can be appended to the raw version (forming the full version)
- `fullCommitId` - the full SHA-1 (or SHA-256) ID of the commit, in lowercase hexadecimal
- `abbreivatedCommitId` - the abbreviated SHA-1 (or SHA-256) ID of the commit, in lowercase hexadecimal
  - This is unambiguous in the repository as of the time of calculation, and no guarantees are made about its minimum 
    or maximum length. However, it will almost always be considerably shorter than the full ID, and is more suitable for
    filenames and other length-restricted names.
- `commitTimestamp` - the timestamp of the commit, in ISO-8601 extended offset date-time format (`1970-01-01T00:00:00+00:00`)

## License

This project is licensed under the MIT License. See the `LICENSE.txt` file for the full license text.

[git]: https://git-scm.com/
[semver]: https://semver.org/spec/v2.0.0.html