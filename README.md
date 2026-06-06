<div align="center">

# Sudoku Pop

一个正在准备上架 Google Play 的数独 App。先把它做稳，再慢慢考虑变现。

A Sudoku app being prepared for Google Play. Make it stable first, then think about monetization.

![Android](https://img.shields.io/badge/Android-35-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?logo=kotlin&logoColor=white)
![Status](https://img.shields.io/badge/status-preparing%20for%20Google%20Play-blue)

[中文](#中文) · [English](#english) · [Screenshots](#screenshots) · [Build](#build)

</div>

## Screenshots

<p align="center">
  <img src="docs/screenshots/home.png" width="220" alt="Sudoku Pop home screen" />
  <img src="docs/screenshots/game.png" width="220" alt="Sudoku Pop gameplay screen" />
  <img src="docs/screenshots/victory.png" width="220" alt="Sudoku Pop victory screen" />
</p>

## 中文

这是我做的一个数独 App，准备先上架到 Google Play。

目前还不是那种很复杂的产品。我现在想先把基础体验做好：打开能玩、题目正常、界面舒服一点、不要乱要权限，也不要一上来就塞广告。

### 现在有什么

- 经典 9x9 数独
- 多个难度
- 每日挑战
- 连续挑战记录
- 笔记模式
- 提示
- 撤销
- 成绩记录
- 亮色/暗色主题
- 简体中文、繁体中文、英文、日文

Android 包名：

```text
com.ewstudio.sudokupop
```

### 我现在的想法

第一版先免费，不接广告，不接内购，也不做登录。

原因很简单：先过 Google Play 审核，先让应用稳定上线。等真的有人用了，再看要不要加激励广告、去广告内购、主题皮肤之类的东西。

我不想一开始就把产品做得很重。数独这种东西，最重要还是打开就能玩，别烦人。

### Google Play 方向

- 免费应用
- 不包含广告
- 不收集个人数据
- 不需要账号
- 不申请敏感权限
- 先走内部测试/封闭测试

隐私政策草稿：`PRIVACY_POLICY.md`

上架资料和后续计划：`GOOGLE_PLAY_LAUNCH_PLAN.md`

### 进度

现在主要在打磨 Android 版本，先把 Google Play 的第一版跑通。

后面会继续补一些真正有用的小东西，比如更好的统计、胜利分享图、主题皮肤之类的。先不急着堆功能，慢慢把它做稳。

## English

Sudoku Pop is a Sudoku app I am building, with Google Play as the first release target.

It is not trying to be a huge product from day one. The first goal is simple: make it playable, keep the puzzles solid, make the UI feel decent, avoid unnecessary permissions, and not throw ads at people immediately.

### What's inside

- Classic 9x9 Sudoku
- Multiple difficulty levels
- Daily challenge
- Daily streak tracking
- Notes mode
- Hints
- Undo
- Best records
- Light and dark themes
- Simplified Chinese, Traditional Chinese, English, and Japanese

Android package name:

```text
com.ewstudio.sudokupop
```

### Current thinking

The first version will be free, with no ads, no in-app purchases, and no login.

The reason is simple: get through Google Play review first and make sure the app is stable in the store. If people actually use it, then I can think about rewarded ads, removing ads, themes, or other paid extras.

I do not want to make the product heavy too early. For a Sudoku app, the most important thing is that people can open it and start playing without being annoyed.

### Google Play direction

- Free app
- No ads
- No personal data collection
- No account required
- No sensitive permissions
- Start with internal or closed testing

Privacy policy draft: `PRIVACY_POLICY.md`

Launch notes and next steps: `GOOGLE_PLAY_LAUNCH_PLAN.md`

### Progress

Right now I am mainly polishing the Android version and getting the first Google Play release path working.

Later I want to add things that actually help the game, like better stats, a shareable win screen, and more themes. No rush to pile on features. I would rather make it stable step by step.

## Build

Build the AAB:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundleRelease
```

Run tests:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest
```

AAB output:

```text
app/build/outputs/bundle/release/app-release.aab
```

