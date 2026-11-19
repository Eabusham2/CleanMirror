# Installing and building

## Just want to use it?

Download the jar from the [latest release](https://github.com/Eabusham2/CleanMirror/releases/latest) and put it in your `.minecraft/mods` folder together with:

| Mod | Needed? | Version for 1.21.11 |
|---|---|---|
| Fabric Loader | yes | 0.19.0 or newer |
| Fabric API | yes | 0.141.0 or newer |
| Mod Menu | for the settings screen | 17.0.0 |
| Cloth Config | for the settings screen | 21.11.x |

Launch the game, press **F6**.

## Building it yourself

You only need **Java 21**. Check with `java -version`; get it from https://adoptium.net if needed. No separate Gradle install is needed: the wrapper is included.

In this folder:

    ./gradlew build          (Mac / Linux)
    gradlew.bat build        (Windows)

The jar appears in `build/libs/`. Or open the folder in IntelliJ IDEA (free Community edition) and press Build.

Versions are pinned in `gradle.properties`. They were checked against Fabric's servers and Modrinth for 1.21.11 (Yarn 1.21.11+build.6, Loader 0.19.5, Fabric API 0.141.6, Loom 1.17), and the project compiles against them with zero errors.

## Running the automated in-game test (optional)

    ./gradlew runAutopilot

This launches Minecraft with CleanMirror plus fake "mods" designed to leak onto the stream (see the README), creates a superflat world, walks through every scene, and saves screenshots of both windows into `build/run/autopilot/cleanmirror-test-out/`, then closes the game. The test code is never included in the mod jar.

(`./gradlew runClientGameTest` uses Fabric's own test framework for the same scenes. On a 2-core test machine its tick lockstep stalled during world creation, so `runAutopilot` is the one that was actually run.)

## Making a release

Releases are built by GitHub Actions (`.github/workflows/build.yml`). To publish a new version:

    git tag v1.0.1
    git push origin v1.0.1

The workflow builds the mod with version `1.0.1` and creates a GitHub Release with `cleanmirror-1.0.1.jar` attached. Every normal push to `main` is also built, and its jar is downloadable from that run's page under *Artifacts*.
