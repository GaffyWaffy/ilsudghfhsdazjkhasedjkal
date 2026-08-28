# Getting a jar

## Option A — let GitHub build it (no local setup)

1. Create a new repository on github.com.
2. Upload the contents of this folder to it (the web UI's "uploading an existing file"
   link accepts a drag-and-drop of the whole folder).
3. Before or after uploading, edit `gradle.properties` and put the real 1.21.11 values for
   `yarn_mappings` and `fabric_version` in it — get them from https://fabricmc.net/develop.
4. Open the **Actions** tab. The `Build mod` workflow runs automatically on push; if it
   doesn't, select it and press **Run workflow**.
5. When it goes green, open the run and download the `chattabs-jar` artifact from the
   Artifacts section at the bottom. Unzip it — the mod jar is inside.

If the run fails, open it and expand the **Build** step. The compiler or Mixin error there is
what you need to fix (or send to me).

## Option B — build locally

There is no Gradle wrapper in this project, so either:

    # install Gradle 8.10+ (sdkman, homebrew, scoop, or gradle.org), then:
    gradle wrapper      # generates ./gradlew, one time only
    ./gradlew build

or skip the wrapper and just run `gradle build` directly. Requires JDK 21.

The jar lands in `build/libs/chattabs-1.0.0.jar`. Ignore the `-sources.jar` beside it.

The first build downloads and decompiles Minecraft, so expect 5–10 minutes.
