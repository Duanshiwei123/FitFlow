# FitFlow Android（Kotlin + Jetpack Compose）

网页版 FitFlow 的原生移植。纯 Kotlin、无第三方业务库、无网络依赖，所有数据存本机。

## 目录结构

```
fitflow-android/
  settings.gradle.kts / build.gradle.kts / gradle.properties
  gradle/wrapper/gradle-wrapper.properties      （缺 gradle-wrapper.jar，见下方接入说明）
  app/
    build.gradle.kts
    src/main/
      AndroidManifest.xml
      res/values/{strings,themes}.xml  res/drawable/ic_launcher.xml
      java/com/fitflow/app/
        MainActivity.kt          三屏导航 + 返回键
        data/Models.kt           Move/Plan/Settings + 动作库 + JSON
        data/Store.kt            SharedPreferences 持久化 + 预置计划
        engine/Engine.kt         时间线编译（Step/音频事件）—— 与网页版同思路
        audio/AudioEngine.kt     AudioTrack 提示音 + 中文 TTS + 可暂停时钟
        ui/theme/Theme.kt        深色运动配色（与网页版一致）
        ui/Components.kt         按钮/分段/数值/开关/弹窗等
        ui/PlansScreen.kt        ① 计划列表
        ui/EditorScreen.kt       ② 计划编辑（动作参数/动作库）
        ui/PlayerScreen.kt       ③ 跟练播放（倒计时/环形进度/节拍/控制）
        ui/Figure.kt             Canvas 火柴人示范
```

## 运行前提（必须先做）

1. **补装 SDK 组件**（你之前 SDK 是空壳）：
   Android Studio → Settings → Android SDK →
   - SDK Platforms 勾 `Android 15 / API 35`
   - SDK Tools 确认 `Android SDK Build-Tools 35`、`Platform-Tools`
   - Apply 下载（几百 MB）
2. JDK：路径上的 **JDK 17** 即可（若 Gradle 报 JDK 错误，把 `JAVA_HOME` 指到 17 或 `D:\jdk21`）。

## 两种接入方式

### 方式 A（推荐）：用 AS 模板工程承接源码

因为本目录缺少二进制 `gradle-wrapper.jar`（无法用文本生成），最省事的是让 Android Studio 生成工程骨架：

1. AS → `File > New > New Project` → 模板 **Empty Activity**，语言 **Kotlin**，工程名 `FitFlow`，包名 **`com.fitflow.app`**，Minimum SDK **26**，位置随意（例如 `D:\AndroidStudioProjects\FitFlow`）。
2. 点 **Finish** 等它 Gradle 同步成功、能跑模板 Hello World（此时它已自带正确的 wrapper/AGP）。
3. 关闭 AS。**用本目录的 4 样东西覆盖/填入宿主工程**（宿主其余文件全部保留，别乱动）：
   ① 覆盖宿主根 `settings.gradle.kts` ← 本目录 `settings.gradle.kts`（已内置阿里云镜像，复制过去就不用再改下载源）
   ② 覆盖宿主 `gradle/wrapper/gradle-wrapper.properties` ← 本目录同名文件（已是腾讯 Gradle 镜像）
   ③ 覆盖宿主 `app/build.gradle.kts` ← 本目录 `app/build.gradle.kts`（compileSdk 35 / 包名 com.fitflow.app）
   ④ 先整个删掉宿主 `app/src`，再把本目录 `app/src` 整个复制过去（Java/res/manifest 一次到位）
4. 重新打开宿主工程 → 点 `Sync Now` → 首次会经腾讯/阿里镜像下载 Gradle 与依赖 → 跑起来即三屏 FitFlow。
   （宿主根 `build.gradle.kts` 里的 AGP/Kotlin 版本保留模板的即可，与本目录代码兼容）

### 方式 B：直接打开本目录

Open `fitflow-android`，AS 若提示缺少 `gradle-wrapper.jar`：
- 最简单：让它从模板补（同方式 A），或
- 在任意已能用的 AS 工程终端执行 `gradle wrapper` 后把生成的 `gradle/wrapper/gradle-wrapper.jar` 拷入本目录。

## 已实现（与网页版对齐的核心）

- 计划列表：预置 3 个计划；重命名 / 删除 / 开始 / 编辑
- 计划编辑：改名、准备时间、动作间休息、动作展开编辑（按个数/按时间、数量、组数、组休、节奏、要点）、动作库换动作、上移/下移/删除/添加、顶部「开始」与每动作「试练」
- 跟练播放：时间线驱动、大数字倒计时或个数计数、环形进度、中文语音播报（动作/休息/一半了/准备下一动作）、节拍器（重音每 5 拍）、最后 3 秒滴答、工作/休息/准备光晕变色、上一个/暂停/下一个、总进度、完成统计页
- 音频：AudioTrack 合成提示音（无素材）、中文 TTS、可暂停时钟
- 火柴人示范动画（示意级：休息/深蹲/平板/俯卧撑/高抬腿/卷腹等已有一组循环）

## 已知待办（MVP 之后）

- [ ] 火柴人骨架改为真正的 FK 关节动画（当前为手工折线姿势，观感一般）
- [ ] 计划复制功能（列表屏已裁）
- [ ] 每个动作上传/选择自己的跟练视频
- [ ] 设置页（语音/节拍/滴答/音量开关，代码里已读 Settings 但未做 UI 入口）
- [ ] 后台音频继续（切后台会走 onDispose 停声）
- [ ] 训练历史与记录
- [ ] 导出/导入 JSON

## 可能需要的人工自检点（本环境无法编译验证）

- `material-icons-core` 只含约 50 个基础图标；若编译报“无法解析符号 Icons.Default.xxx”，换用 `Create/Delete/ArrowBack/Add` 或文字按钮。
- Compose BOM 与 AS 内置版本不同步时，仅会提示可升级，不影响编译。
- 若编辑器“试练/动作库”弹窗或播放器 Canvas 布局不符预期，属于适配小问题，贴给我即可。
