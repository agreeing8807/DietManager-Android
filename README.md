# 饮食管理（DietManager）

一款**本地优先**的 Android 饮食与饮水记录应用。用相机或一句话记下一餐，应用负责识别食物、换算营养、统计当日摄入，并给出符合当前时段的可执行建议。

数据全部保存在设备本地；AI 只负责「识别看到了什么」，数值由本地成分表确定性计算，因此结果可追溯、可修正。

- 语言 / 平台：Kotlin · Android 8.0+（minSdk 26）
- 构建方式：Gradle 8.6 + Android Gradle Plugin 8.2.2
- 许可证：GPL-3.0

---

## 目录

- [项目简介](#项目简介)
- [主要功能](#主要功能)
- [安装与使用](#安装与使用)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [配置说明](#配置说明)
- [数据与计算口径](#数据与计算口径)
- [常见问题](#常见问题)
- [许可证](#许可证)

---

## 项目简介

记录饮食的难点不在「记」，而在「记得准」。让大模型直接估算热量，数字看着合理却无法追溯，记错了也无从修改。

本项目按以下顺序处理一餐：

1. **识别**：模型只回答「看到了什么食物、品牌、份数或克数、是否包装食品」，不输出任何营养数值；
2. **计算**：常见食物直接查内置成分表，按克数做确定性换算；
3. **补数**：仅在成分表未命中时，才让模型补「每 100g」数据，并要求给出数据来源与置信度，低于 0.3 直接丢弃；
4. **落库**：识别结果以「食物条目」持久化，用户可逐项改克数、删除，或对缺失项重新补全。

因此每条记录都带有明确的数据状态（已确认 / 估算 / 部分缺失 / 缺失），界面不再靠文案猜测。

## 主要功能

### 饮食记录

| 能力 | 说明 |
| --- | --- |
| 拍照识别 | CameraX 拍照，压缩后上传；图片仅驻留内存，不写入相册与本地存储 |
| 文字记录 | 一句话描述即可，例如「食堂鸡腿饭 + 一杯冰美式」 |
| 食物明细 | 每餐保存条目（名称、品牌、克数、每 100g 基准、来源），可改克数或删除后自动重算合计 |
| 数据状态 | 缺失的条目会保留并标红，支持「补全缺失营养」或「重新识别这餐」 |
| 快捷记录 | 常吃食物保存为快捷项，一键加入当日 |
| 餐次归档 | 早餐 / 午加餐 / 午餐 / 晚加餐 / 晚餐，支持浏览历史日期 |

### 饮水与冲调

| 能力 | 说明 |
| --- | --- |
| 液体型水种 | 按每 100ml 记录，可设置水分比例（如牛奶 87%）与含糖热量 |
| 冲调型水种 | 按「每份干粉（一勺）」记录固定营养；冲调水量只影响总容量与浓度，**不改变蛋白质等营养总量** |
| 分份计算 | 支持 0.5～4 份，实时预览总容量、干粉重量与浓度 |
| 往日修改 | 往日仅展示每日汇总，点按展开明细后可修改或删除 |
| 喝水提醒 | 可自定义时间窗口与间隔，精确闹钟 + 开机重排 + WorkManager 兜底 |

### 其他

- **首页概览**：今日热量 / 三大宏量 / 饮水进度、目标体重趋势、月度汇总。
- **吃什么建议**：结合当前时间、剩余额度与在校大学生场景，推荐校园里能直接买到的食物。
- **本地备份**：导出 / 导入 JSON（含餐食条目明细与水种配置，**不含 API Key**）。
- **隐私**：API Key 加密存储在 EncryptedSharedPreferences；无账号体系、无云端同步。

## 安装与使用

### 环境要求

| 项目 | 版本 |
| --- | --- |
| JDK | 17 |
| Android SDK | Platform 34、Build-Tools 34.0.0 |
| Android Studio | Hedgehog（2023.1.1）及以上 |
| 运行设备 | Android 8.0（API 26）及以上 |

### 构建

推荐使用 Android Studio 打开项目根目录，等待 Gradle Sync 完成后直接运行。

命令行构建（仓库未包含 `gradle-wrapper.jar`，需使用本机 Gradle 8.6）：

```bash
gradle :app:assembleDebug        # 产物：app/build/outputs/apk/debug/app-debug.apk
gradle :app:assembleRelease      # 产物：app/build/outputs/apk/release/app-release.apk
gradle :app:testDebugUnitTest    # 运行单元测试
```

Windows 环境下若工程路径包含中文，`testDebugUnitTest` 会因测试 JVM 的 classpath 解析失败而报 `ClassNotFoundException`（编译不受影响）。解决办法是把工程放到纯 ASCII 路径，或建立一个指向该目录的 ASCII 联接后在其中执行测试。

### 首次使用

1. 完成引导：填写身高、体重、目标体重、活动水平，应用会推算每日热量目标；
2. 进入 **设置 → API 与模型**，添加一个 OpenAI 兼容服务：选择厂商预设或自定义 Base URL、模型名，填入 API Key，保存并设为当前使用；
3. 回到 **记录** 页，点「+」拍照或输入描述，等待识别完成；
4. 若某餐出现「营养数据缺失」，点开该餐 → 改动克数，或点「补全缺失营养」。

> 未配置 API Key 时，拍照与文字分析不可用，其余功能（手动记录、饮水、提醒、统计）均可正常使用。

## 技术栈

| 领域 | 选型 |
| --- | --- |
| 语言 / UI | Kotlin 1.9.22、Jetpack Compose（BOM 2024.02.00）、Material 3 |
| 架构 | 单 Activity、MVVM、Repository、StateFlow |
| 依赖注入 | Hilt 2.50（KSP） |
| 本地存储 | Room 2.6.1（当前数据库版本 8，含 7 次迁移）、DataStore Preferences |
| 网络 | Retrofit 2.11 + OkHttp 4.12，统一 OpenAI 兼容协议 |
| 序列化 | kotlinx.serialization 1.6.2（结构化 JSON 输出） |
| 相机 | CameraX 1.3.1 |
| 定时任务 | AlarmManager（精确闹钟）+ WorkManager 2.9.0 |
| 安全 | androidx.security-crypto（API Key 加密存储） |
| 测试 | JUnit 4，47 个纯函数单元测试 |

## 项目结构

```
app/src/main/java/com/example/diet/
├── data/
│   ├── local/                  Room 实体、DAO、数据库、类型转换器
│   │   └── FoodCompositionTable.kt    内置食物成分表与模糊匹配
│   ├── remote/                 OpenAI 兼容接口、厂商预设、结构化模型
│   │   └── provider/           识别、补数、建议的提示词与调用编排
│   ├── repository/             餐食 / 饮水 / 体重 / 资料 / 分析
│   └── BackupManager.kt         JSON 备份导出与导入
├── di/                         Hilt 模块（数据库、网络、仓库）
├── domain/                     营养换算与领域规则（无 Android 依赖）
│   ├── NutrientBasis.kt        统一营养基准（每 100g / 每 100ml / 每份）
│   ├── NutritionTools.kt       换算、求和、合理性校验
│   ├── MealItems.kt            条目换算与整餐状态推导
│   ├── DayLedger.kt            全应用唯一的单日统计口径
│   └── DrinkCalculator.kt      液体型与冲调型换算
├── ui/                         首页 / 记录 / 饮水 / 设置 / 引导 / 通用组件
├── receiver/ worker/           提醒广播与周期任务
├── util/                       日期、图片压缩等工具
├── DietApp.kt
└── MainActivity.kt
```

```
app/src/test/java/com/example/diet/     47 个单元测试（纯函数，无需设备）
├── domain/                            换算、口径、份量基准、冲调
└── data/local/                        成分表匹配规则
```

## 配置说明

### 应用内配置（设置页）

| 配置项 | 位置 | 说明 |
| --- | --- | --- |
| API 服务 | 设置 → API 与模型 | 可保存多套配置（厂商、Base URL、模型名、Key），随时切换当前使用的一套 |
| 提示词 | 设置 → API 与模型 → 提示词 | 三份独立可编辑：`prompt_identify`（识别）、`prompt_fill`（补数）、`prompt_suggest`（建议）；支持一键恢复默认 |
| 个人信息与目标 | 设置 → 个人资料 | 性别、生日、身高、当前 / 目标体重、活动水平；热量目标可手动覆盖 |
| 每日开始时间 | 设置 → 个人资料 | 0～12 点，默认为凌晨 4 点；该时间之前的记录归属前一天 |
| 饮水目标 | 设置 → 个人资料 | 默认 2000 ml |
| 数据备份 | 设置 → 数据 | 导出 / 导入 JSON，导入会覆盖现有数据 |

### 构建期环境变量

| 变量 | 用途 | 缺省 |
| --- | --- | --- |
| `VERSION_CODE` / `VERSION_NAME` | 覆盖版本号 | `2` / `1.1.0` |
| `RELEASE_KEYSTORE_PATH` | 发布签名 keystore 路径 | 回退 debug 签名 |
| `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD` | 签名口令 | 同上 |

CI（`.github/workflows/build-apk.yml`）：推送 `v*` tag 会自动构建并发布 APK 到 Release；在 Actions 页手动触发则以 artifact 形式产出。

### 支持的模型

任何兼容 OpenAI Chat Completions 的服务均可接入。内置预设包括通义千问、智谱 GLM 与 DeepSeek；**图片识别需要视觉模型**（如 `qwen-vl-plus`、`glm-4v`），纯文本模型只能处理文字描述。

## 数据与计算口径

以下规则由 `DayLedger`、`MealItems`、`NutritionTools` 统一定义，各页面不再各自求和：

- **热量** = 餐食 + 饮品（含冲调干粉带来的热量）；
- **饮水量**只算水：冲调型按实际加水量计，干粉体积不算饮水；
- **冲调营养** = 每份数值 × 份数，与加水量无关（水量只影响总容量与浓度）；
- **能量校验**：热量与 4/4/9 推算值偏差超过 30% 或 80 kcal 时以宏量重算；单餐超过 3000 kcal 会被封顶；
- **缺失处理**：没有可靠数据的条目保留但不参与合计，并计入「数据不完整」提示；
- **条目为真源**：`meal_record` 上的四项数值是条目汇总的缓存，条目变更后由 `syncTotals` 重算。

## 常见问题

<details>
<summary>没有 API Key 可以用吗？</summary>

可以。手动记录、饮水与冲调、提醒、统计、备份均不依赖网络；只有拍照识别、文字分析和食物建议需要 API。
</details>

<details>
<summary>提示「当前模型不支持图片识别」怎么办？</summary>

在设置中把模型换成视觉模型（如 `qwen-vl-plus`、`glm-4v`），或直接改用文字描述记录。
</details>

<details>
<summary>为什么某餐显示「营养数据缺失」？</summary>

该餐的食物没有命中内置成分表，模型补数也失败了，或返回的置信度低于 0.3（宁可留空也不编造）。点开该餐后可以手动改克数、删除条目，或点「补全缺失营养」重新请求一次。
</details>

<details>
<summary>冲调饮品加了很多水，为什么热量没变？</summary>

这是设计如此。冲调型的营养按「每份干粉」固定，水只改变总容量与浓度；杯数不变时蛋白质总量也不会变。
</details>

<details>
<summary>为什么我改的热量和显示的不一致？</summary>

当该餐已有食物明细时，数值以明细为准，手改总热量不会生效。请在明细里调整克数。
</details>

<details>
<summary>换手机后数据怎么迁移？</summary>

设置 → 数据 → 导出备份，在新设备导入即可。备份包含餐食条目、水种配置与体重记录，但**不包含 API Key**，需要重新填写。
</details>

<details>
<summary>凌晨记录的数据算哪一天？</summary>

按「每日开始时间」（默认凌晨 4 点）归属，4 点前记录算前一天。可在个人资料中调整。
</details>

<details>
<summary>命令行运行单元测试报 ClassNotFoundException？</summary>

工程路径含中文时，Windows 下 Gradle 8.6 的测试 JVM 无法正确解析 classpath。请将工程放到纯 ASCII 路径，或建立 ASCII 目录联接后在其中执行。
</details>

## 许可证

本项目基于 [GNU General Public License v3.0](LICENSE) 发布。你可以自由使用、修改与分发，但衍生作品需以相同许可证开源。
