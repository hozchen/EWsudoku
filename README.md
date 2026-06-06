# Sudoku Pop

这是我做的一个数独 App，准备先上架到 Google Play。

目前还不是那种很复杂的产品，先把基础体验做好：打开能玩、题目正常、界面舒服一点、不要乱要权限，也不要一上来就塞广告。

## 截图

<p>
  <img src="docs/screenshots/home.png" width="220" alt="Sudoku Pop 首页" />
  <img src="docs/screenshots/game.png" width="220" alt="Sudoku Pop 游戏界面" />
  <img src="docs/screenshots/victory.png" width="220" alt="Sudoku Pop 挑战成功" />
</p>

## 现在有什么

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

## 我现在的想法

第一版先免费，不接广告，不接内购，也不做登录。

原因很简单：先过 Google Play 审核，先让应用稳定上线。等真的有人用了，再看要不要加激励广告、去广告内购、主题皮肤之类的东西。

我不想一开始就把产品做得很重。数独这种东西，最重要还是打开就能玩，别烦人。

## Google Play 方向

首版大概按这个方向来：

- 免费应用
- 不包含广告
- 不收集个人数据
- 不需要账号
- 不申请敏感权限
- 先走内部测试/封闭测试

隐私政策草稿在这里：

```text
PRIVACY_POLICY.md
```

上架资料和后续计划在这里：

```text
GOOGLE_PLAY_LAUNCH_PLAN.md
```

## 构建

生成 AAB：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundleRelease
```

跑测试：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest
```

AAB 位置：

```text
app/build/outputs/bundle/release/app-release.aab
```

## 进度

现在主要在打磨 Android 版本，先把 Google Play 的第一版跑通。

后面会继续补一些真正有用的小东西，比如更好的统计、胜利分享图、主题皮肤之类的。先不急着堆功能，慢慢把它做稳。
