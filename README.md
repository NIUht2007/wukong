# 甜气相机

甜系少女风拍照拼图 Android App。原生 Kotlin + Jetpack Compose，纯本地处理，无后端、无账号。

- 包名：`com.tianqi.camera` ｜ minSdk 26（Android 8.0+）
- 产品需求与设计规范见 `docs/tianqi-camera/`

## 构建与运行

```bash
./gradlew assembleDebug     # 编译 debug APK
./gradlew installDebug      # 安装到已连接的真机/模拟器
```

要求：JDK 17+，Android SDK（随 Android Studio 安装）。相机功能请用真机验证。

## 打包 release（APK / AAB）

1. 生成签名 keystore（只需一次，**不要提交到 git**）：

```bash
keytool -genkey -v -keystore tianqi.jks -keyalg RSA -keysize 2048 -validity 10000 -alias tianqi
```

2. 在项目根目录新建 `key.properties`（已被 .gitignore 排除）：

```properties
storeFile=tianqi.jks
storePassword=你的密码
keyAlias=tianqi
keyPassword=你的密码
```

3. 构建：

```bash
./gradlew assembleRelease   # APK → app/build/outputs/apk/release/
./gradlew bundleRelease     # AAB → app/build/outputs/bundle/release/
```

release 已开启 R8 混淆与资源压缩（规则在 `app/proguard-rules.pro`）。
没有 `key.properties` 时 release 构建回退 debug 签名，仅供本地测试，**不要上架**。

## 如何替换素材

| 素材 | 位置 | 说明 |
|---|---|---|
| 应用图标 | `app/src/main/res/drawable/ic_launcher_foreground.xml` + `ic_launcher_background.xml` | 当前为占位矢量图；正式 1024px PNG 到位后用 Android Studio 的 Image Asset 重新生成 |
| 启动屏 | `app/src/main/res/drawable/ic_splash.xml` + `res/values/themes.xml` | 甜粉底 + 白色相机 |
| 贴纸 | `StickerFactory.kt` | 当前 8 枚程序自绘占位；素材包到位后在 `stickerIds` 登记并改为从 assets 加载 PNG |
| 字体 | `ui/theme/Type.kt` | 把字体文件放入 `res/font/` 后替换 `FontFamily.Default`（授权文件一并存档） |
| 隐私政策 | `app/src/main/assets/privacy_policy.md` | 上架前请律师审核 |

## 如何新增拼图模板

只需改 `app/src/main/assets/templates.json`，加一条（坐标为画布比例值 0-1）：

```json
{
  "id": "2_new", "name": "新模板", "count": 2, "ratio": "1:1",
  "slots": [
    {"x": 0, "y": 0, "w": 0.5, "h": 1},
    {"x": 0.5, "y": 0, "w": 0.5, "h": 1}
  ]
}
```

不改任何代码，模板选择页会自动分组展示。

## 如何新增/微调滤镜

改 `app/src/main/java/com/tianqi/camera/model/FilterSpec.kt` 的 `SweetFilters.ALL`，
每款滤镜就是一组参数（饱和度/对比度/黑位抬升/RGB 增益偏移），注释里有每款的调色思路。

## 目录结构

```
app/src/main/java/com/tianqi/camera/
├── model/       数据模型（模板、滤镜、图层、美颜参数、编辑状态）
├── service/     业务能力（滤镜/拼图/美颜/图层渲染、草稿、作品、相册导出、人脸检测）
└── ui/
    ├── theme/       品牌主题（色板/圆角/排版，对应设计规范）
    ├── components/  通用组件（图层覆盖层、滤镜面板、隐私弹窗等）
    ├── navigation/  路由
    └── pages/       首页/拍照/模板选择/拼图编辑/照片编辑/导出/设置
```

## 隐私与合规

- 所有图像处理在本地完成，无任何上传；人脸检测用 ML Kit 离线模型
- 首次启动弹隐私政策（`assets/privacy_policy.md`），设置页可再次查看
- 崩溃日志只写本机 `files/crash/`
