<p align="center">
  <img src="assets/logo.svg" width="160" height="160" alt="WriteRom Logo" />
</p>

<h1 align="center">WriteRom · ROM 存储极限填充与压测工具</h1>

<p align="center">
  <b>面向 Android 设备的闪存空间填充、低存储（Low Storage）边缘场景验证与闪存老化测试工具</b><br>
  <i>Android flash storage filler, low-storage scenario simulator, and wear-leveling stress testing utility.</i>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Min%20SDK-26%2B%20(Android%208.0%2B)-blue.svg" alt="Min SDK" />
  <img src="https://img.shields.io/badge/Target%20SDK-34-informational.svg" alt="Target SDK" />
  <img src="https://img.shields.io/badge/Architecture-Java%20%2B%20Kotlin%20Compose-6366F1.svg" alt="Architecture" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-orange.svg" alt="License" />
</p>

---

## 📖 项目简介

**WriteRom** 是一款专为 Android 开发者、测试工程师（QA）以及系统固件调优人员打造的**存储空间填充与低存储压力测试工具**。

在移动应用测试与系统 ROM 开发中，验证应用在闪存空间即将耗尽（Low Storage / Out of Space / ENOSPC）极端场景下的鲁棒性至关重要。WriteRom 支持以高吞吐量向手机 ROM 写入大块数据文件、真实合规 JPG 图片以及标准 MP4 视频，并提供精确的阈值设置与一键清理还原机制，帮助快速复现与验证极限边缘场景。

---

## ⚡ 核心功能特性

### 1. 真实精确的 ROM 存储状态动态感知
* **双重动态推演与校准**：基于 `StatFs` 与已写入字节数（每 400ms 刷新一次），使顶部 **手机 ROM 存储状态** 模块与彩色环形进度条伴随写入平滑动态跳动；文件落盘后自动同步 Linux 文件系统底层最新数据块。
* **防溢出安全硬约束**：设定阈值无论如何调整，计算出的实际填充量**绝对不会超过手机自身的可用 ROM 大小**。

### 2. 灵活的填充阈值与策略设置
* **保留剩余空间模式**：允许自定义保留空间（**默认保留 0 MB**，支持榨干闪存进行极限压测）。
* **目标占用比例模式**：支持一键将 ROM 占用比例填充至 90%、95%、99% 乃至 100%。
* **指定填充大小模式**：支持直接指定填充大小（例如填充 10GB、50GB）。
* **实时预估计算**：滑动调节阈值时，实时演算并显示本次任务预计还将写入的真实数据量。

### 3. 多介质、多类型填充内容支持
* **大块普通文件（RAW_FILE）**：
  * **零字节极速填充（Zero-fill）**：以极高吞吐量快速占位；
  * **随机伪数据（Random Data）**：生成不可压缩的高熵数据，真实消耗闪存块，防止 F2FS / ZRAM 等透明压缩算法抵消存储占用。
* **真实合规图片（IMAGE）**：
  * 动态生成带有时间戳、色彩梯度和几何噪点的合法 JPEG 图片，可被系统与第三方相册正常索引与生成缩略图。
* **真实合规视频（VIDEO）**：
  * 基于 ISO-BMFF 标准规范构建合规 MP4 容器结构，支持播放器正常解码与相册视频预览。
* **混合模式（MIXED）**：
  * 循环交替生成文件、图片与视频，综合模拟日常多媒体使用场景。

### 4. 媒体可见性隔离（方案 A）
* **专属目录防污染**：默认所有生成数据存储在应用专属隔离目录中（`.nomedia` 隔离），**绝不污染用户的日常个人相册**。
* **系统相册导入开关**：可手动勾选【导入系统相册】，开启后主动向 Android `MediaStore` 触发扫描与索引，用于专项压测相册缩略图生成器（MediaScanner）的系统负载。

### 5. 前台服务常驻与平滑控制
* 采用 `ForegroundService` 结合 `PARTIAL_WAKE_LOCK`，确保锁屏或切到后台时填充任务不被系统 Low Memory Killer 中断。
* 常驻通知栏实时展示瞬时写入速率（MB/s）、已写入进度与预计剩余时间（ETA）。
* 支持 **【开始】**、**【暂停】**、**【恢复填充】**、**【终止任务】** 平滑控制。

### 6. 一键极速清理与空间复原
* 独立追踪所有生成的数据，提供 **【一键清理填充数据】** 功能。
* 后台多线程异步彻底删除所有填充文件，并同步剔除 MediaStore 索引，瞬间恢复手机可用存储空间。

### 7. 专属定制微芯片图标
* 配备全套 Android Adaptive Icon 自适应矢量图标与全密度位图（`mipmap-xxxhdpi` 等），适配现代 Android 系统的圆角、方形、圆形及单色桌面规范。

---

## 🏗️ 架构设计

本项目严格遵循企业级 **Java 业务逻辑 + Kotlin Jetpack Compose UI 混编架构**：

```
┌────────────────────────────────────────────────────────┐
│               Kotlin Compose UI 表现层                 │
│    (Bento 仪表盘卡片、滑块控制、Material 3 响应式界面)   │
└──────────────────────────▲─────────────────────────────┘
                           │ 观察不可变 State / 分发 UI 事件
┌──────────────────────────┴─────────────────────────────┐
│             Kotlin 桥接层 (UiBridge.kt)                │
│    - ComposeView 桥接 (ViewCompositionStrategy 销毁)   │
│    - 不可变 StorageUiState 状态装载                     │
│    - UI 事件回调接口 (UiCallbacks)                      │
└──────────────────────────▲─────────────────────────────┘
                           │ 方法调用 / 主线程回调
┌──────────────────────────┴─────────────────────────────┐
│              Java 业务逻辑与系统层                     │
│   - MainActivity.java (宿主 Activity 与权限管理)       │
│   - StorageFillerService.java (前台服务保活常驻)       │
│   - FillEngine.java (填充调度核心引擎)                 │
│   - FileGenerator.java (大块数据生成，零填/随机)       │
│   - ImageGenerator.java (标准合法 JPG 图片生成)        │
│   - VideoGenerator.java (合规标准 MP4 视频生成)        │
│   - CleanupManager.java (一键释放与多媒体库清理)       │
│   - StorageHelper.java (StatFs 容量感知与格式化)       │
└────────────────────────────────────────────────────────┘
```

### 目录结构规范
```text
WriteRom/
├── app/
│   ├── src/main/
│   │   ├── java/com/mobiiot/writerom/
│   │   │   ├── engine/          # [Java] 核心任务调度与空间清理引擎
│   │   │   ├── generator/       # [Java] 文件、JPG图片、MP4视频生成器
│   │   │   ├── model/           # [Java] 存储状态、配置与进度数据模型
│   │   │   ├── service/         # [Java] 前台保活与通知栏服务
│   │   │   ├── ui/              # [Kotlin] ComposeView 桥接与 Material3 页面
│   │   │   │   ├── components/  # [Kotlin] 仪表盘、配置卡片、实时速率监控组件
│   │   │   │   └── theme/       # [Kotlin] 视觉色彩与主题
│   │   │   ├── utils/           # [Java] StatFs 容量计算与多媒体扫描工具
│   │   │   ├── MainActivity.java
│   │   │   └── WriteRomApplication.java
│   │   ├── res/                 # 布局、微芯片自适应矢量图标与界面资源
│   │   └── AndroidManifest.xml  # 权限声明与前台服务配置
│   └── build.gradle
├── assets/                      # 项目 Logo (assets/logo.svg)
├── build.gradle
├── gradle.properties            # 脱敏构建属性（去除本机私有路径）
├── settings.gradle
├── .gitignore                   # 行业标准 Android 忽略规则
├── LICENSE                      # Apache 2.0 开源许可
└── README.md
```

---

## 🛠️ 编译与安装

### 环境要求
* **Android Studio**: Hedgehog (2023.1.1) 或更高版本
* **JDK**: OpenJDK 17 或以上
* **Android SDK**: `compileSdk 34`，`minSdk 26`（Android 8.0+）

### 源码编译
```bash
# 克隆仓库
git clone https://github.com/<your-username>/WriteRom.git
cd WriteRom

# 编译 Debug APK
./gradlew assembleDebug

# 安装到已连接的 Android 设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 隐私与安全性声明

* **无任何第三方统计/网络 SDK**：本应用不包含任何网络请求代码、无埋点、无广告、不收集任何设备或个人隐私信息。
* **数据安全隔离**：所有填充操作严格限制在专用目录（`WriteRom_Data`），绝不扫描、读取、修改或删除用户的任何现有个人文件。
* **代码开源透明**：所有源码公开，零隐私泄露隐患，便于代码审计与二次开发。

---

## 📄 开源许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证开源。
