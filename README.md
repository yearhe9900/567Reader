# 567Reader

[![Platform](https://img.shields.io/badge/platform-Android%2033%2B-3DDC84)](https://www.android.com)
[![Compile SDK](https://img.shields.io/badge/compileSdk-37-3DDC84)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-7F52FF)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

**567Reader** 是一款基于开源阅读项目 [Legado](https://github.com/gedoor/legado) 二次开发的 Android 阅读器。在继承 Legado 完整书源体系与阅读能力的基础上，重新落地了**液态玻璃（Liquid Glass）**主题系统、墨水屏适配与多主题切换等现代化体验。

> 软件本身不提供任何内容，书籍内容均来自用户自行添加的第三方书源或本地文件。

---

## 目录

- [主要特性](#主要特性)
- [技术架构](#技术架构)
- [模块划分](#模块划分)
- [构建与运行](#构建与运行)
- [快速开始](#快速开始)
- [API 说明](#api-说明)
- [开源协议与致谢](#开源协议与致谢)
- [免责声明](#免责声明)

---

## 主要特性

### 阅读与内容
- **自定义书源**：通过规则抓取网页数据，支持搜索、发现、目录、正文解析，规则简单易懂。
- **订阅源 / 替换规则**：订阅任意 RSS 类内容；替换净化可批量去除广告与冗余文本。
- **本地阅读**：支持 TXT、EPUB、PDF、MOBI、AZW / AZW3 等格式，可手动浏览或智能扫描导入。
- **高度自定义阅读界面**：字体、颜色、背景、行距、段距、加粗、简繁转换等均可配置。
- **多种翻页模式**：覆盖、仿真、滑动、滚动等。

### 多媒体
- **语音朗读（TTS）**：内置朗读引擎与在线朗读（Http TTS）支持。
- **音频播放**：带歌词的音频播放（基于 LyricViewX）。
- **视频播放**：基于 ExoPlayer / GSYVideoPlayer，支持**弹幕**（danmakuFlameMaster）。

### 现代化界面（本项目新增）
- **液态玻璃（Liquid Glass）UI**：基于 Kyant Backdrop 的玻璃态卡片、对话框与底部导航栏，支持模糊半径、折射高度 / 强度、色差等参数实时调节。
- **墨水屏（EInk）模式**：对玻璃态、对话框、深色列表等做专项降级，适配墨水屏设备刷新特性。
- **多主题模式**：浅色 / 深色 / 跟随系统 / 墨水屏，主题切换即时套用。

### 数据与互通
- **Web 服务**：内置 NanoHTTPD 局域网服务，可在浏览器管理书籍 / 书源。
- **API**：提供 ContentProvider 与 Web 两种调用方式，支持 `legado://` / `yuedu://` 一键导入。
- **备份恢复**：WebDAV、本地导出导入、旧版 Legado 数据迁移。

---

## 技术架构

| 层面 | 选型 |
|------|------|
| 语言 | Kotlin（JVM 17） |
| UI | Jetpack Compose（Material3）+ 传统 View 混合；ViewBinding |
| 玻璃态 | [Kyant Backdrop](https://github.com/kyant0/backdrop)（Liquid Glass） |
| 本地存储 | Room（KSP 代码生成）+ SharedPreferences |
| 网络 | OkHttp + Cronet |
| 解析 | Jsoup / JsoupXpath / JsonPath / Rhino（JS 引擎） |
| 媒体 | ExoPlayer、GSYVideoPlayer、弹幕、LyricViewX |
| 图片 | Glide（KSP 注解） |
| 其他 | NanoHTTPD、ZXing、Markwon、sora-editor、libarchive |

### 模块划分

```
567Reader/
├── app/                 # 主应用模块（UI、业务、服务、Provider）
├── modules/
│   ├── book/            # 书源 / 书籍解析核心逻辑
│   └── rhino/           # Rhino JavaScript 引擎封装
└── build.gradle / settings.gradle
```

---

## 构建与运行

### 环境要求
- **Android SDK**：compileSdk 37，构建工具齐全
- **JDK**：17（通过 Gradle toolchain 自动指定）
- **Gradle**：使用仓库自带 `gradlew`（无需单独安装）

### 构建命令

```bash
# 调试包
./gradlew :app:assembleAppDebug

# 正式包（需在 local.properties / 环境变量中配置签名）
./gradlew :app:assembleAppRelease
```

产物输出为 `567Reader.apk`（release 包默认带 `.release` 后缀，debug 带 `.debug` 后缀）。

> 镜像仓库：若无法连接 Google / Maven Central 源，可在 `settings.gradle` 中取消注释对应的阿里云 / 华为云镜像。

### 导入到 Android Studio
1. 打开 Android Studio（建议最新稳定版）。
2. `File → Open` 选择本仓库根目录。
3. 等待 Gradle 同步完成后直接运行 `app` 模块。

---

## 快速开始

1. 首次启动进入欢迎页，按需授予存储权限。
2. 进入「书源管理」导入书源（支持扫码、URL、`legado://` 一键导入、本地文件）。
3. 回到书架搜索书名，或进入「发现」浏览书源内置分类。
4. 在「设置」中可切换主题、开启墨水屏模式、调整液态玻璃参数等。

新用户可参考帮助文档（应用内「设置 → 帮助」或 Legado 社区 wiki）。

---

## API 说明

阅读 3.0 提供两种调用方式：`Web 方式` 与 `Content Provider 方式`。

- 通过 URL 唤起阅读进行一键导入：`legado://import/{path}?src={url}`
- `path` 类型：`bookSource`（书源）、`rssSource`（订阅源）、`replaceRule`（替换规则）、`textTocRule`（本地 TXT 目录规则）、`httpTTS`（在线朗读）、`theme`（主题）、`readConfig`（阅读排版）、`dictRule`（字典）、`addToBookshelf`（加入书架）。

---

## 开源协议与致谢

本项目继承自 [Legado（开源阅读）](https://github.com/gedoor/legado)，遵循其开源协议精神。

核心依赖（部分）：
- org.jsoup:jsoup、cn.wanghaomiao:JsoupXpath、com.jayway.jsonpath:json-path
- com.github.gedoor:rhino-android、com.squareup.okhttp3:okhttp
- com.github.bumptech.glide:glide、org.nanohttpd:nanohttpd
- io.noties.markwon:core、com.hankcs:hanlp、com.positiondev.epublib:epublib-core
- com.github.Moriafly:LyricViewX、io.github.rosemoe:sora-editor
- io.github.kyant0:backdrop（Liquid Glass）

完整依赖见 `app/build.gradle`。

---

## 免责声明

阅读依赖系统 WebView 提供网页访问能力，通过用户自定义的第三方书源返回内容，软件对返回内容概不负责，亦不承担任何法律责任。任何第三方书源均系他人制作或提供，非软件作者，对其合法性概不负责。您应对搜索结果自行承担风险。如认为某书源涉嫌侵权，请及时通知，软件将依法处理。
