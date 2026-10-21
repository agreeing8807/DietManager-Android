# DietManager-Android

一款**本地优先**的安卓饮食管理应用，使用 Kotlin + Jetpack Compose 构建。

## 功能特性
- 🍱 接入国内大模型（通义千问 / 智谱 GLM / DeepSeek）分析一日三餐，自动估算热量与三大宏量营养素
- ⚖️ 记录目标体重与每日热量摄入，首页可视化对比目标（剩余/超额）
- 🥤 记录每日饮水量；奶茶等含糖饮品同时计算热量与水分
- 🔔 可自定义的喝水提醒（精确闹钟 + 开机自动重排 + 通知）
- 💡 基于剩余热量给出建议食物
- 🔒 用户数据本地存储（Room）；API Key 加密保存于 EncryptedSharedPreferences
- 📦 支持本地 JSON 备份导出 / 导入

## 技术栈
- 原生 Kotlin + Jetpack Compose
- Hilt（依赖注入）、Room（本地数据库）、Navigation Compose
- Retrofit + kotlinx.serialization（大模型接入，统一 OpenAI 兼容协议）
- AlarmManager + WorkManager（提醒）、CameraX（拍照分析，图片仅驻留内存不落盘）

> 注意：三餐 AI 分析与食物建议需自备 API Key；未配置时仍可手动记录。
