# oxygen-launcher-api

一个为 **Oxygen Launcher** 提供启动器 API、Mindustry 适配后端、附加模组与示例程序的多模块项目。

它不是 Android 启动器本体，而是 Oxygen Launcher 运行 `Mindustry.jar` 时依赖的桥接层与附加组件仓库。  

## 项目简介

这个仓库主要解决的是“启动器如何与游戏/后端通信”的问题，包含：

- 提供 Android JNI 桥接声明的 `api` 模块
- 适配 Mindustry 的 Oxygen 图形与输入后端
- 启动器注入到游戏内的附加模组
- 供本地验证桥接与渲染流程的示例程序
- 运行时需要的 LWJGL Android 原生库资源

主启动器仓库负责 Android 侧运行时、JRE 管理、JVM 启动与资源准备；本仓库负责被启动的 Java 侧接口与扩展实现。

## 与 Oxygen Launcher 的关系

[`Oxygen Launcher`](https://github.com/EmmmM9O/oxygen-launcher) 的整体职责包括：

- 在 Android 上准备运行环境
- 解压和管理 JRE
- 启动 JVM
- 加载 `Mindustry.jar`

而本仓库提供的是启动后所需的附加能力：

- `oxygen.api.LauncherBridge`：Java 到 Android 启动器的 JNI 接口声明
- `mindustry` 模块：基于 LWJGL / EGL / OpenGL ES 的 Mindustry 适配入口与后端
- `launcher-mod` 模块：注入到游戏内的启动器 UI / 功能模组
- `lwjgl-natives` 模块：Android 侧所需的 LWJGL 原生库资源

## 工作方式

Oxygen Launcher 与本仓库大致按下面的方式协作：

1. Android 启动器准备 JRE、工作目录和游戏文件
2. 启动器加载本仓库产出的后端与模组组件
3. Java 侧通过 `LauncherBridge` 调用 Android 提供的 JNI 实现
4. `mindustry` 模块接管图形、输入、文件选择、横屏控制等桥接能力
5. `launcher-mod` 在游戏内部显示附加 UI，并与启动器行为配合

## 项目结构

```text
api/             启动器桥接 API，定义 JNI 接口与回调
mindustry/       Mindustry Oxygen 后端与启动入口
launcher-mod/    注入到 Mindustry 的启动器模组
lwjgl-natives/   Android 所需 LWJGL / GLES / EGL 原生库资源
example/         最小示例程序，用于测试桥接与渲染链路
refers/          参考文档（Oxygen Launcher 主项目 README）
```

## 模块说明

### `api`

提供启动器与 Java 侧通信用到的核心接口，例如：

- 日志输出
- Android 权限申请
- 文件选择器
- 剪贴板读写
- Surface / 生命周期回调
- 文本输入与震动控制

核心入口包括：

- `oxygen.api.LauncherBridge`
- `oxygen.api.LauncherBridgeCallback`

该模块配置了 `maven-publish`，可发布到本地 Maven：

```bash
./gradlew :api:publishToMavenLocal
```

### `mindustry`

这是对 Mindustry 的启动器适配模块，包含：

- `mindustry.oxygen.MainKt` 启动入口
- `arc.backend.oxygen.*` 后端实现
- 面向 Android 启动器场景的文件选择、权限与窗口行为接入

该模块会读取 `mindustry/libs/` 下的依赖资源，并在构建时按需：

- 裁剪 `Mindustry.jar`
- 提取并重打包 Android natives
- 生成可分发的整合产物

常用任务：

```bash
./gradlew :mindustry:dist
```

如果需要构建附加组件形态，可传入 `addon` 属性：

```bash
./gradlew :mindustry:dist -Paddon=true
```

### `launcher-mod`

这是 Mindustry 内部的附加模组，当前主要包含：

- `LauncherMain`
- 启动器浮动 UI
- Fabric 安装相关辅助逻辑

构建后产物文件名默认为：

```text
oxygen-launcher-ui.jar
```

### `example`

提供一个最小运行示例，用于验证：

- `LauncherBridge` 回调链路
- EGL Surface 创建
- OpenGL ES 渲染
- 启动器文本输入、剪贴板、日志等接口

常用任务：

```bash
./gradlew :example:run
./gradlew :example:dist
```

### `lwjgl-natives`

存放 Android/Linux ARM 环境下需要的 LWJGL 相关原生库资源，供 `mindustry` 与 `example` 模块使用。

## 构建说明

这是一个基于 Gradle 的 Kotlin/JVM 多模块项目，当前配置主要包括：

- Kotlin 2.3.0
- Gradle Wrapper
- Java Toolchain 25
- LWJGL 3.4.1

构建前请先准备：

- JDK 25
- 可用的 Gradle 运行环境（或直接使用仓库内 `./gradlew`）
- `mindustry` 模块构建所需的 `Mindustry.jar` 与相关 natives 依赖文件

常见命令：

```bash
./gradlew build
./gradlew :api:publishToMavenLocal
./gradlew :launcher-mod:jar
./gradlew :mindustry:dist
./gradlew :example:dist
```

## 依赖与输入文件

`mindustry` 模块默认会从 `mindustry/libs/` 读取外部输入文件。按当前构建脚本，常见文件包括：

- `Mindustry.jar`
- `natives-android.jar`
- `natives-freetype-android.jar`
- 其他运行所需 `.jar`

如果这些文件不存在，构建脚本会输出提示，部分产物不会被打入最终结果。

## 适用场景

- 为 Oxygen Launcher 提供 Java 侧桥接接口
- 在 Android 上为 Mindustry 注入自定义后端
- 研究 Mindustry 在 LWJGL + EGL + OpenGL ES 路径下的运行方式
- 调试启动器与游戏之间的 JNI / 生命周期 / Surface 协作流程

## 当前状态

这是一个明显偏实验性、偏工程验证的仓库，重点是：

- 定义启动器与 Java 侧的桥接协议
- 验证 Mindustry 在 Android 启动器中的附加后端可行性
- 提供可注入的模组与可复用的原生资源组织方式

如果你想了解完整启动流程，建议与主仓库配合阅读：

- 启动器主项目：<https://github.com/EmmmM9O/Oxygen-launcher>
- 当前仓库：<https://github.com/EmmmM9O/oxygen-launcher-api>

## 说明

Mindustry 及其相关资源版权归原作者与对应项目所有。  
本仓库主要提供启动器桥接接口、附加后端与模组实现，不等同于游戏本体。
