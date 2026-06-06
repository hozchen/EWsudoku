# Sudoku Pop

Sudoku Pop 是一款面向 Google Play 上架的轻量级数独游戏。当前目标是先完成低风险首版发布，再通过留存功能、用户反馈和后续版本逐步探索收入模式。

## 当前状态

- Android 包名：`com.ewstudio.sudokupop`
- 首版策略：免费、无广告、无内购、无账号登录
- 核心玩法：经典 9x9 数独
- 留存功能：每日挑战、连续挑战记录
- 本地功能：笔记、提示、撤销、成绩记录、主题和语言切换
- 隐私策略：首版不收集个人数据，不接入广告、统计或支付 SDK

## 上架策略

首版不要急着接广告或内购。先把应用通过 Google Play 内部测试/封闭测试，确认稳定性、商店资料、内容分级和数据安全表单都能顺利通过。

推荐节奏：

1. `1.0`：免费发布，无广告，验证审核和基础留存。
2. `1.1`：增强留存，例如成就、分享胜利图、更多统计。
3. `1.2`：考虑激励广告，例如额外提示或失败后继续。
4. `1.3`：考虑一次性去广告或主题内购。

## 构建

项目使用 Android Gradle 构建。生成 Google Play 推荐上传的 AAB：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundleRelease
```

运行单元测试：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest
```

生成文件位置：

```text
app/build/outputs/bundle/release/app-release.aab
```

## Google Play 上传提醒

- 上传 `.aab`，不要上传 debug APK。
- 首版“是否包含广告”选择“否”。
- 数据安全表单按“首版不收集个人数据”填写。
- 目标受众选择普通用户，不要声明专门面向儿童。
- 隐私政策可基于 `PRIVACY_POLICY.md` 发布成网页后填写链接。
- 如果 Google Play 提示签名问题，用 Android Studio 的 `Generate Signed App Bundle / APK` 生成正式签名 AAB。

## 仓库规则

不要提交以下内容：

- APK/AAB 打包产物
- 签名文件，如 `.jks`、`.keystore`
- `local.properties`
- `app/build/`、`.gradle/` 等构建缓存
- 本机个人配置，如 `.DS_Store`、`xcuserdata/`

这些内容已经在 `.gitignore` 中排除。

