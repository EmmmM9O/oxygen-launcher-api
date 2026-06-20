# oxygen-launcher-api

A multi-module project that provides the launcher API, Mindustry backend adaptation, addon mod, and example program for **Oxygen Launcher**.

This repository is not the Android launcher itself. It contains the bridge layer and companion components used when Oxygen Launcher runs `Mindustry.jar`.

[中文说明](./README_zh.md)

## Overview

This repository mainly solves the problem of how the launcher communicates with the game and backend. It includes:

- The `api` module with Android JNI bridge declarations
- An Oxygen graphics and input backend adapted for Mindustry
- An addon mod injected into the game by the launcher
- A minimal example program for validating the bridge and rendering pipeline locally
- LWJGL native library resources required at runtime

The main launcher repository is responsible for the Android runtime, JRE management, JVM startup, and asset preparation. This repository is responsible for the Java-side interfaces and extension implementations loaded by the launcher.

## Relationship to Oxygen Launcher

[`Oxygen Launcher`](https://github.com/EmmmM9O/oxygen-launcher) is responsible for:

- Preparing the runtime environment on Android
- Extracting and managing the JRE
- Launching the JVM
- Loading `Mindustry.jar`

This repository provides the capabilities needed after startup:

- `oxygen.api.LauncherBridge`: JNI interface declarations from Java to the Android launcher
- `mindustry` module: Mindustry entrypoint and backend based on LWJGL / EGL / OpenGL ES
- `launcher-mod` module: launcher UI and feature mod injected into the game
- `lwjgl-natives` module: LWJGL native resources required on Android

## How It Works

Oxygen Launcher and this repository roughly cooperate in the following way:

1. The Android launcher prepares the JRE, working directory, and game files.
2. The launcher loads the backend and mod artifacts produced by this repository.
3. The Java side calls the Android-provided JNI implementation through `LauncherBridge`.
4. The `mindustry` module handles graphics, input, file chooser, orientation, and other bridge capabilities.
5. The `launcher-mod` module displays additional in-game UI and integrates with launcher behavior.

## Project Structure

```text
api/             Launcher bridge API, JNI interfaces, and callbacks
mindustry/       Mindustry Oxygen backend and startup entry
launcher-mod/    Launcher mod injected into Mindustry
lwjgl-natives/   LWJGL / GLES / EGL native resources required on Android
example/         Minimal sample used to test bridge and rendering flow
```

## Modules

### `api`

Provides the core interfaces used for communication between the launcher and the Java side, including:

- Logging
- Android permission requests
- File chooser access
- Clipboard read and write
- Surface and lifecycle callbacks
- Text input and vibration control

Main entry points:

- `oxygen.api.LauncherBridge`
- `oxygen.api.LauncherBridgeCallback`

This module is configured with `maven-publish` and can be published to local Maven:

```bash
./gradlew :api:publishToMavenLocal
```

### `mindustry`

This is the launcher adaptation module for Mindustry. It includes:

- `mindustry.oxygen.MainKt` startup entry
- `arc.backend.oxygen.*` backend implementations
- File chooser, permission, and window behavior integrations for Android launcher usage

This module reads dependency inputs from `mindustry/libs/` and, during build, can:

- Trim `Mindustry.jar`
- Extract and repackage Android natives
- Produce distributable combined artifacts

Common task:

```bash
./gradlew :mindustry:dist
```

If you need the addon-style artifact, pass the `addon` property:

```bash
./gradlew :mindustry:dist -Paddon=true
```

### `launcher-mod`

This is the in-game addon mod for Mindustry. It currently mainly contains:

- `LauncherMain`
- Floating launcher UI
- Fabric installation helper logic

The default output artifact name is:

```text
oxygen-launcher-ui.jar
```

### `example`

Provides a minimal runnable example for validating:

- The `LauncherBridge` callback chain
- EGL surface creation
- OpenGL ES rendering
- Launcher text input, clipboard, logging, and related interfaces

Common tasks:

```bash
./gradlew :example:run
./gradlew :example:dist
```

### `lwjgl-natives`

Contains LWJGL native library resources required for Android/Linux ARM environments and used by the `mindustry` and `example` modules.

## Build Notes

This is a Gradle-based Kotlin/JVM multi-module project. The current configuration mainly includes:

- Kotlin 2.3.0
- Gradle Wrapper
- Java Toolchain 25
- LWJGL 3.4.1

Before building, prepare:

- JDK 25
- A working Gradle environment, or just use `./gradlew`
- `Mindustry.jar` and related native dependency files required by the `mindustry` module

Common commands:

```bash
./gradlew build
./gradlew :api:publishToMavenLocal
./gradlew :launcher-mod:jar
./gradlew :mindustry:dist
./gradlew :example:dist
```

## Dependencies and Input Files

The `mindustry` module reads external input files from `mindustry/libs/` by default. Based on the current build script, common files include:

- `Mindustry.jar`
- `natives-android.jar`
- `natives-freetype-android.jar`
- Other runtime `.jar` files as needed

If these files are missing, the build script prints warnings and some outputs will not be packaged into the final artifacts.

## Use Cases

- Providing Java-side bridge interfaces for Oxygen Launcher
- Injecting a custom backend for Mindustry on Android
- Studying how Mindustry runs on an LWJGL + EGL + OpenGL ES path
- Debugging the JNI, lifecycle, and surface coordination flow between the launcher and the game

## Current Status

This repository is clearly experimental and focused on engineering validation. Its main goals are:

- Defining the bridge protocol between the launcher and the Java side
- Verifying the feasibility of an additional backend for Mindustry inside an Android launcher
- Providing an injectable mod and a reusable structure for native resources

For the full startup flow, read it together with the main launcher repository:

- Launcher main project: <https://github.com/EmmmM9O/Oxygen-launcher>
- This repository: <https://github.com/EmmmM9O/oxygen-launcher-api>
