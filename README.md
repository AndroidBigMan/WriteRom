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

### 5. 高级压测与并发控制 (v1.2 新增)
* **1~8 线程自由并发调度**：
  * 支持 1、2、4、8 线程弹性伸缩，多线程协同共享剩余填充配额（`AtomicLong` 原子分发），充分榨干 UFS / eMMC 闪存主控的多通道并行读写吞吐能力。
  * 实时状态栏与 UI 仪表盘实时呈现“X 线程并发”徽标，并以千位分隔符展示已创建文件总量。
* **文件系统碎片化填充模式 (Mass Small Files)**：
  * **极限 Inode 与元数据（Metadata）冲击**：摆脱单一的大文件顺序写入，支持连续写入成千上万个 4KB ~ 256KB 的零散碎片文件，用于模拟长周期碎片化、测试闪存磨损均衡（Wear-Leveling）与 Linux 内核 VFS 目录索引树性能。
  * **分级目录散列管理（Directory Bucket Hashing）**：按每 500 个文件自动归档至独立散列子目录（`frag_bucket_000` ~ `frag_bucket_999`），杜绝单一目录超量节点导致 ext4/f2fs 线性遍历瘫痪或内核软死锁。
  * **三档碎片规格可选**：
    * `极小碎片 (4KB ~ 16KB)`：高密度元数据读写与极限碎片化；
    * `常用碎片 (32KB ~ 128KB)`：日常应用缓存与微日志碎片分布模拟；
    * `混合高熵 (4KB ~ 256KB)`：全跨度尺寸碎片交织填充。
    * **规格互斥与双向联动设计**：分块大小（`256MB` / `500MB` / `1024MB`）与碎片化小文件规格在 UI 与引擎层完全互斥并双向实时联动（点击任一大分块自动退出碎片化，反之亦然），实时仪表盘与常驻通知栏明确标明“当前生效规格”，彻底消除配置歧义与优先级混乱。
* **真实穿插打孔 (Write & Punch)**：
    * 模拟长期使用下的物理不连续碎块，每累计生成 500 个微文件时，以 30% 概率随机穿插剔除部分微文件，在闪存物理块中制造非连续的空白空洞，强迫底层闪存主控在后续写入时频繁触发垃圾回收（GC）与坏块迁移。

### 6. 实时性能衰减折线图与 SLC Cache 耗尽感知 (v1.3 新增)
* **Compose Canvas 平滑贝塞尔曲线渲染**：
  * 基于 `Canvas` API 与平滑贝塞尔算法，以秒级时间窗口（40秒滑动窗口）实时绘制写入吞吐性能演进趋势，杜绝生硬折线与锯齿。
  * 曲线下方采用主色垂直渐变微光填充，配合参考虚线网格、全程均速参考线与峰值速率指示。
* **SLC Cache 耗尽特征自动识别**：
  * 针对现代 UFS / eMMC 固态闪存的 SLC 模拟缓存特性，实时监测速率骤降（从峰值断崖式下跌至 40%~50% 以下），自动标注 **“⚡ SLC Cache 已耗尽”**，直观展现从高速缓存写入向 TLC/QLC 物理颗粒原生直写与后台搬迁（Folding/GC）的过渡状态。

### 7. UFS/eMMC 闪存寿命与芯片温度感知 (v1.3 新增)
* **设备核心温度实时感知**：
  * 基于电池与硬件传感器高频采样设备芯片温度（精度 0.1°C），划分四档温控安全等级（正常冷感 / 轻度温升 / 偏热负载 / 严重过热）。
* **系统热节流 (Thermal Throttling) 状态监测**：
  * 深度接入 Android 10+ (API 29) 官方 `PowerManager.OnThermalStatusChangedListener`，实时感知系统底层是否已对 CPU/闪存主控实施降频降温保护。
* **闪存损耗与 TBW 寿命统计**：
  * 持久化统计 WriteRom 累计总写入数据量（TBW），并根据设备 ROM 物理容量动态换算当前已消耗的相当完整 P/E 擦写循环次数，直观评估对闪存颗粒的老化影响。

### 8. 前台服务常驻与平滑控制
* 采用 `ForegroundService` 结合 `PARTIAL_WAKE_LOCK`，确保锁屏或切到后台时填充任务不被系统 Low Memory Killer 中断。
* 常驻通知栏实时展示瞬时写入速率（MB/s）、当前并发线程数、SLC 耗尽状态、已写入进度与预计剩余时间（ETA）。
* 支持 **【开始】**、**【暂停】**、**【恢复填充】**、**【终止任务】** 平滑控制。

### 9. 一键极速清理、进度感知与安全中断
* **专属目录与碎片极速清空**：独立追踪所有生成的数据，后台异步彻底递归删除所有大文件与散列碎片目录（`frag_bucket_*`），并同步通知系统剔除 MediaStore 索引，瞬间恢复手机可用存储空间。
* **实时动态进度感知**：清理过程中展示直观的高频反馈看板，清晰呈现“已清理 X / Y 个文件”与“已释放 Z GB 空间”，配备确定型/平滑双模进度条，拒绝盲目等待。
* **支持中途安全终止（停止清理）**：针对千万级微文件或海量占用耗时场景，提供醒目的 **【停止清理】** 红色操作按钮，采用原子线程信号安全即时中断递归删除，立即结算并保留已释放的空间成果。

### 10. 专属定制微芯片图标
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
│   - FillEngine.java (多线程并发调度核心引擎)           │
│   - FileGenerator.java (大块数据生成，零填/随机)       │
│   - FragmentationGenerator.java (海量散列碎片小文件)   │
│   - ImageGenerator.java (标准合法 JPG 图片生成)        │
│   - VideoGenerator.java (合规标准 MP4 视频生成)        │
│   - CleanupManager.java (一键释放与多媒体库清理)       │
│   - HardwareMonitor.java (芯片温度与系统热节流监测)    │
│   - LifetimeStatsManager.java (TBW 与 P/E 寿命统计)    │
│   - StorageHelper.java (StatFs 容量感知与格式化)       │
└────────────────────────────────────────────────────────┘
```

### 目录结构规范
```text
WriteRom/
├── app/
│   ├── src/main/
│   │   ├── java/com/mobiiot/writerom/
│   │   │   ├── engine/          # [Java] 核心多线程任务调度与空间清理引擎
│   │   │   │   ├── FillEngine.java
│   │   │   │   └── CleanupManager.java
│   │   │   ├── generator/       # [Java] 文件、微小碎片、JPG图片、MP4视频生成器
│   │   │   │   ├── FileGenerator.java
│   │   │   │   ├── FragmentationGenerator.java
│   │   │   │   ├── ImageGenerator.java
│   │   │   │   └── VideoGenerator.java
│   │   │   ├── model/           # [Java] 存储状态、压测配置与进度数据模型
│   │   │   │   ├── FillConfig.java
│   │   │   │   ├── FillProgress.java
│   │   │   │   ├── FragmentSizeLevel.java
│   │   │   │   └── ...
│   │   │   ├── service/         # [Java] 前台保活与多线程状态通知服务
│   │   │   ├── ui/              # [Kotlin] ComposeView 桥接与 Material3 页面
│   │   │   │   ├── components/  # [Kotlin] Bento仪表盘、高级压测卡片、折线图等
│   │   │   │   │   ├── PerformanceChartCard.kt # v1.3 Canvas 实时性能折线图与 SLC 耗尽
│   │   │   │   │   ├── HardwareHealthCard.kt   # v1.3 闪存寿命与芯片温度感知
│   │   │   │   │   ├── AdvancedStressCard.kt   # v1.2 压测并发与碎片化设置
│   │   │   │   │   ├── LiveDashboardCard.kt    # 实时并发度与速率状态监控
│   │   │   │   │   └── ...
│   │   │   │   └── theme/       # [Kotlin] 视觉色彩与主题
│   │   │   ├── utils/           # [Java] 硬件监测、TBW寿命、StatFs 容量工具
│   │   │   │   ├── HardwareMonitor.java
│   │   │   │   ├── LifetimeStatsManager.java
│   │   │   │   └── StorageHelper.java
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
