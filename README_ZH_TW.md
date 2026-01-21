# Zalith Launcher 2
![Downloads](https://img.shields.io/github/downloads/ZalithLauncher/ZalithLauncher2/total)
![Discord](https://img.shields.io/discord/1409012263423185039?logo=discord&label=Discord&color=7289DA&link=https%3A%2F%2Fdiscord.gg%2FyDDkTHp4cJ)
[![Sponsor](https://img.shields.io/badge/sponsor-30363D?logo=GitHub-Sponsors)](https://afdian.com/a/MovTery)
<!-- [![QQ](https://img.shields.io/badge/QQ-blue)](https://qm.qq.com/q/2MVxS0B29y) -->

[English](README.md) | [简体中文](README_ZH_CN.md)


> [!IMPORTANT]
> 該專案與 [ZalithLauncher](https://github.com/ZalithLauncher/ZalithLauncher) 屬於兩個完全不同的專案  

**Zalith Launcher 2** 是一個全新設計、面向 **Android 裝置** 的 [Minecraft: Java Edition](https://www.minecraft.net/) 啟動器。專案使用 [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/tree/v3_openjdk/app_pojavlauncher/src/main/jni) 作為啟動核心，採用 **Jetpack Compose** 與 **Material Design 3** 構建現代化 UI 體驗。  
我們目前正在搭建自己的官方網站 [zalithlauncher.cn](https://zalithlauncher.cn)  
此外，我們已注意到有第三方使用「Zalith Launcher」名稱搭建了一個看似官方的網站。請注意：**該網站並非我們創建**，其透過冒用名義並植入廣告牟利。我們對此類行為**不參與、不認可、不信任**。  
請務必提高警覺，**謹防個人隱私資訊洩露**！  






## 🌐 語言與翻譯支援

我們正在使用 Weblate 平台翻譯 Zalith Launcher 2，歡迎您前往我們的 [Weblate 專案](https://hosted.weblate.org/projects/zalithlauncher2) 參與翻譯！  
感謝每一位語言貢獻者的支持，讓 Zalith Launcher 2 更加多語化、更加國際化！





## 📦 構建方式（開發者）

> 以下內容適用於希望參與開發或自行構建應用的使用者。

### 環境要求

* Android Studio Bumblebee 以上
* Android SDK：
    * **最低 API**：26
    * **目標 API**：35
* JDK 11

### 構建步驟

```bash
git clone git@github.com:ZalithLauncher/ZalithLauncher2.git
# 使用 Android Studio 開啟專案並進行構建
```




## 📜 License

本專案程式碼遵循 **[GPL-3.0 license](LICENSE)** 開源協議。

### 附加條款（依據 GPLv3 開源授權條款第七條）

1. 當你分發本程式的修改版本時，必須以合理方式修改該程式的名稱或版本號，以區別於原始版本。（依據 [GPLv3, 7(c)](https://github.com/ZalithLauncher/ZalithLauncher2/blob/969827b/LICENSE#L372-L374)）
    - 修改版本 **不得在名稱中包含原程式名稱「ZalithLauncher」或其縮寫「ZL」，亦不得使用與官方名稱相近、可能造成混淆的名稱**。
    - 所有修改版本 **必須在程式啟動畫面或主介面中以明顯方式標示其為「非官方修改版」**。
    - 程式的應用名稱可於 [gradle.properties](./ZalithLauncher/gradle.properties) 中進行修改。

2. 你不得移除本程式所顯示的版權聲明。（依據 [GPLv3, 7(b)](https://github.com/ZalithLauncher/ZalithLauncher2/blob/969827b/LICENSE#L368-L370)）

## 引用開源專案
  
本軟體使用以下開源函式庫:

| Library                               | Copyright                                                                                                                                  | License              | Official Link                                                                     |
|---------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|----------------------|-----------------------------------------------------------------------------------|
| androidx-constraintlayout-compose     | Copyright © The Android Open Source Project                                                                                                | Apache 2.0           | [链接](https://developer.android.com/develop/ui/compose/layouts/constraintlayout) |
| androidx-material-icons-core          | Copyright © The Android Open Source Project                                                                                                | Apache 2.0           | [链接](https://developer.android.com/jetpack/androidx/releases/compose-material)  |
| androidx-material-icons-extended      | Copyright © The Android Open Source Project                                                                                                | Apache 2.0           | [链接](https://developer.android.com/jetpack/androidx/releases/compose-material)  |
| ANGLE                                 | Copyright 2018 The ANGLE Project Authors                                                                                                   | BSD 3-Clause License | [链接](http://angleproject.org/)                                                  |
| Apache Commons Codec                  | -                                                                                                                                          | Apache 2.0           | [链接](https://commons.apache.org/proper/commons-codec)                           |
| Apache Commons Compress               | -                                                                                                                                          | Apache 2.0           | [链接](https://commons.apache.org/proper/commons-compress)                        |
| Apache Commons IO                     | -                                                                                                                                          | Apache 2.0           | [链接](https://commons.apache.org/proper/commons-io)                              |
| ByteHook                              | Copyright © 2020-2024 ByteDance, Inc.                                                                                                      | MIT License          | [链接](https://github.com/bytedance/bhook)                                        |
| Coil Compose                          | Copyright © 2025 Coil Contributors                                                                                                         | Apache 2.0           | [链接](https://github.com/coil-kt/coil)                                           |
| Coil Gifs                             | Copyright © 2025 Coil Contributors                                                                                                         | Apache 2.0           | [链接](https://github.com/coil-kt/coil)                                           |
| Fishnet                               | Copyright © 2025 Kyant                                                                                                                     | Apache 2.0           | [链接](https://github.com/Kyant0/Fishnet)                                         |
| gl4es_extra_extra                     | Copyright (c) 2016-2018 Sebastien Chevalier; Copyright (c) 2013-2016 Ryan Hileman                                                          | MIT License          | [链接](https://github.com/PojavLauncherTeam/gl4es_extra_extra)                    |
| Gson                                  | Copyright © 2008 Google Inc.                                                                                                               | Apache 2.0           | [链接](https://github.com/google/gson)                                            |
| kotlinx.coroutines                    | Copyright © 2000-2020 JetBrains s.r.o.                                                                                                     | Apache 2.0           | [链接](https://github.com/Kotlin/kotlinx.coroutines)                              |
| ktor-client-cio                       | Copyright © 2000-2023 JetBrains s.r.o.                                                                                                     | Apache 2.0           | [链接](https://ktor.io)                                                           |
| ktor-client-content-negotiation       | Copyright © 2000-2023 JetBrains s.r.o.                                                                                                     | Apache 2.0           | [链接](https://ktor.io)                                                           |
| ktor-client-core                      | Copyright © 2000-2023 JetBrains s.r.o.                                                                                                     | Apache 2.0           | [链接](https://ktor.io)                                                           |
| ktor-http                             | Copyright © 2000-2023 JetBrains s.r.o.                                                                                                     | Apache 2.0           | [链接](https://ktor.io)                                                           |
| ktor-serialization-kotlinx-json       | Copyright © 2000-2023 JetBrains s.r.o.                                                                                                     | Apache 2.0           | [链接](https://ktor.io)                                                           |
| LWJGL - Lightweight Java Game Library | Copyright © 2012-present Lightweight Java Game Library All rights reserved.                                                                | BSD 3-Clause License | [链接](https://github.com/LWJGL/lwjgl3)                                           |
| material-color-utilities              | Copyright 2021 Google LLC                                                                                                                  | Apache 2.0           | [链接](https://github.com/material-foundation/material-color-utilities)           |
| Maven Artifact                        | Copyright © The Apache Software Foundation                                                                                                 | Apache 2.0           | [链接](https://github.com/apache/maven/tree/maven-3.9.9/maven-artifact)           |
| Media3                                | Copyright © The Android Open Source Project                                                                                                | Apache 2.0           | [链接](https://developer.android.com/jetpack/androidx/releases/media3)            |
| Mesa                                  | Copyright © The Mesa Authors                                                                                                               | MIT License          | [链接](https://mesa3d.org/)                                                       |
| MMKV                                  | Copyright © 2018 THL A29 Limited, a Tencent company.                                                                                       | BSD 3-Clause License | [链接](https://github.com/Tencent/MMKV)                                           |
| Navigation 3                          | Copyright © The Android Open Source Project                                                                                                | Apache 2.0           | [链接](https://developer.android.com/jetpack/androidx/releases/navigation3)       |
| NBT                                   | Copyright © 2016 - 2020 Querz                                                                                                              | MIT License          | [链接](https://github.com/Querz/NBT)                                              |
| NG-GL4ES                              | Copyright (c) 2016-2018 Sebastien Chevalier; Copyright (c) 2013-2016 Ryan Hileman; Copyright (c) 2025-2026 BZLZHH                          | MIT License          | [链接](https://github.com/BZLZHH/NG-GL4ES)                                        |
| OkHttp                                | Copyright © 2019 Square, Inc.                                                                                                              | Apache 2.0           | [链接](https://github.com/square/okhttp)                                          |
| Process Phoenix                       | Copyright © 2015 Jake Wharton                                                                                                              | Apache 2.0           | [链接](https://github.com/JakeWharton/ProcessPhoenix)                             |
| proxy-client-android                  | -                                                                                                                                          | LGPL-3.0 License     | [链接](https://github.com/TouchController/TouchController)                        |
| Reorderable                           | Copyright © 2023 Calvin Liang                                                                                                              | Apache 2.0           | [链接](https://github.com/Calvin-LL/Reorderable)                                  |
| StringFog                             | Copyright © 2016-2023, Megatron King                                                                                                       | Apache 2.0           | [链接](https://github.com/MegatronKing/StringFog)                                 |
| XZ for Java                           | Copyright © The XZ for Java authors and contributors                                                                                       | 0BSD License         | [链接](https://tukaani.org/xz/java.html)                                          |
