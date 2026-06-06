# Sudoku Pop

这是我做的一个数独 App，准备先上架到 Google Play。

This is a Sudoku app I am building, with Google Play as the first release target.

目前还不是那种很复杂的产品，先把基础体验做好：打开能玩、题目正常、界面舒服一点、不要乱要权限，也不要一上来就塞广告。

It is not trying to be a huge product from day one. The first goal is simple: make it playable, keep the puzzles solid, make the UI feel decent, avoid unnecessary permissions, and not throw ads at people immediately.

## 截图 / Screenshots

<p>
  <img src="docs/screenshots/home.png" width="220" alt="Sudoku Pop home screen" />
  <img src="docs/screenshots/game.png" width="220" alt="Sudoku Pop gameplay screen" />
  <img src="docs/screenshots/victory.png" width="220" alt="Sudoku Pop victory screen" />
</p>

## 现在有什么 / What's inside

- 经典 9x9 数独 / Classic 9x9 Sudoku
- 多个难度 / Multiple difficulty levels
- 每日挑战 / Daily challenge
- 连续挑战记录 / Daily streak tracking
- 笔记模式 / Notes mode
- 提示 / Hints
- 撤销 / Undo
- 成绩记录 / Best records
- 亮色/暗色主题 / Light and dark themes
- 简体中文、繁体中文、英文、日文 / Simplified Chinese, Traditional Chinese, English, and Japanese

Android 包名 / Android package name:

```text
com.ewstudio.sudokupop
```

## 我现在的想法 / Current thinking

第一版先免费，不接广告，不接内购，也不做登录。

The first version will be free, with no ads, no in-app purchases, and no login.

原因很简单：先过 Google Play 审核，先让应用稳定上线。等真的有人用了，再看要不要加激励广告、去广告内购、主题皮肤之类的东西。

The reason is simple: get through Google Play review first and make sure the app is stable in the store. If people actually use it, then I can think about rewarded ads, removing ads, themes, or other paid extras.

我不想一开始就把产品做得很重。数独这种东西，最重要还是打开就能玩，别烦人。

I do not want to make the product heavy too early. For a Sudoku app, the most important thing is that people can open it and start playing without being annoyed.

## Google Play 方向 / Google Play direction

首版大概按这个方向来：

The first release is planned around this:

- 免费应用 / Free app
- 不包含广告 / No ads
- 不收集个人数据 / No personal data collection
- 不需要账号 / No account required
- 不申请敏感权限 / No sensitive permissions
- 先走内部测试/封闭测试 / Start with internal or closed testing

隐私政策草稿在这里 / Privacy policy draft:

```text
PRIVACY_POLICY.md
```

上架资料和后续计划在这里 / Launch notes and next steps:

```text
GOOGLE_PLAY_LAUNCH_PLAN.md
```

## 构建 / Build

生成 AAB / Build the AAB:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundleRelease
```

跑测试 / Run tests:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest
```

AAB 位置 / AAB output:

```text
app/build/outputs/bundle/release/app-release.aab
```

## 进度 / Progress

现在主要在打磨 Android 版本，先把 Google Play 的第一版跑通。

Right now I am mainly polishing the Android version and getting the first Google Play release path working.

后面会继续补一些真正有用的小东西，比如更好的统计、胜利分享图、主题皮肤之类的。先不急着堆功能，慢慢把它做稳。

Later I want to add things that actually help the game, like better stats, a shareable win screen, and more themes. No rush to pile on features. I would rather make it stable step by step.

