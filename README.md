# FitFlow - 智能健身跟练助手

一个帮助你轻松管理健身计划的 Android 应用。将网上看到的训练计划输入后，自动生成专属的跟练计划，每个动作都有计时/计数功能，让训练更专注。

## 功能特性

- 📋 **计划管理**：创建、编辑、重命名多个健身计划
- 🎯 **动作配置**：为每个动作设置次数或时长（如俯卧撑 15 个/组、高抬腿 30 秒/组）
- ⏱️ **智能跟练**：自动计时/计数，无需手动记录
- 🔄 **流畅切换**：动作之间无缝切换，训练不中断
- 🎨 **Material Design 3**：现代化 UI，符合 Android 设计规范

## 截图

### 计划列表页
显示所有创建的健身计划，支持新建和进入编辑

### 计划编辑页
添加/删除动作，设置每个动作的名称、次数或时长

### 跟练播放页
实时显示当前动作、进度，自动计时/计数，完成一个动作后自动跳转下一个

## 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose
- **最低版本**：Android 8.0 (API 26)
- **目标版本**：Android 15 (API 35)
- **构建工具**：Gradle 9.5 + AGP 9.x

## 运行项目

### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 35

### 克隆项目

```bash
git clone https://github.com/Duanshiwei123/FitFlow.git
cd FitFlow/fitflow-android
```

### 在 Android Studio 中打开

1. 打开 Android Studio
2. 选择 `Open` → 选择 `fitflow-android` 目录
3. 等待 Gradle 同步完成
4. 连接 Android 设备或启动模拟器
5. 点击 ▶ Run 按钮

## 项目结构

```
app/src/main/java/com/fitflow/app/
├── MainActivity.kt          # 主入口
├── data/
│   ├── Exercise.kt          # 动作数据模型
│   ├── WorkoutPlan.kt       # 计划数据模型
│   └── PlanRepository.kt    # 数据仓库
├── engine/
│   └── AudioEngine.kt       # 音频引擎（计时提示音）
└── ui/
    ├── PlanListScreen.kt    # 计划列表页
    ├── PlanEditScreen.kt    # 计划编辑页
    └── WorkoutScreen.kt     # 跟练播放页
```

## 使用说明

### 创建计划

1. 打开应用，点击右下角 `+` 按钮
2. 输入计划名称（如"胸肌训练"）
3. 点击进入计划，开始添加动作

### 添加动作

1. 在计划编辑页点击 `+ 添加动作`
2. 输入动作名称（如"俯卧撑"）
3. 选择类型：
   - **次数模式**：输入每组次数（如 15 个）
   - **时长模式**：输入每组时长（如 30 秒）
4. 保存后可继续添加更多动作

### 开始跟练

1. 在计划列表页点击目标计划
2. 点击 `开始训练`
3. 跟随屏幕提示完成每个动作
4. 完成所有动作后自动结束

## 贡献

欢迎提交 Issue 和 Pull Request！

## License

本项目采用 MIT 协议开源。详见 [LICENSE](LICENSE) 文件。

## 联系方式

- 作者：Duanshiwei123
- 邮箱：3261632676@qq.com
- GitHub：https://github.com/Duanshiwei123/FitFlow

## 致谢

感谢所有为 Android 开源生态做出贡献的开发者。
