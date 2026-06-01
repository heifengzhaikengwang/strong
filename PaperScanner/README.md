# 试卷扫描王 (Paper Scanner)

一个类似夸克扫描王的Android试卷扫描应用，可以快速扫描纸质试卷并进行增强处理。

## 功能特性

- 📷 **相机拍摄**: 使用CameraX实现流畅的相机预览和拍摄
- 📐 **A4比例裁剪框**: 默认显示A4纸张比例的矩形框
- ✋ **可拖动调整**: 矩形框的四个顶点可以自由拖动调整大小
- ✨ **图片增强**: 自动对扫描内容进行增强处理
  - 灰度化处理
  - 高斯模糊
  - 锐化
  - 自适应阈值二值化
- 📖 **多张连拍**: 支持连续拍摄多页试卷
- 💾 **保存与分享**: 支持保存到相册和多图分享
- 📁 **历史记录**: 自动保存扫描历史

## 参考项目

本项目的图片增强算法参考了 [OpenCV Document Scanner](https://github.com/andrewdcampbell/OpenCV-Document-Scanner) 项目。

## 技术栈

- **语言**: Kotlin
- **最低SDK**: Android 5.0 (API 21)
- **目标SDK**: Android 14 (API 34)
- **相机**: CameraX
- **图像处理**: OpenCV Android SDK
- **架构**: MVVM + ViewModel

## 依赖库

- CameraX (相机功能)
- OpenCV 4.9.0 (图像处理)
- Glide (图片加载)
- Material Design 3 (UI组件)
- Kotlin Coroutines (异步处理)
- AndroidX Lifecycle (ViewModel/LiveData)

## 构建说明

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK Platform 34

### 构建步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd PaperScanner
   ```

2. **配置Android SDK**
   确保已安装Android SDK Platform 34和构建工具。

3. **打开项目**
   在Android Studio中打开项目根目录。

4. **同步Gradle**
   点击 "File" -> "Sync Project with Gradle Files"

5. **构建Debug APK**
   ```bash
   ./gradlew assembleDebug
   ```
   或在Android Studio中点击 "Build" -> "Build Bundle(s) / APK(s)" -> "Build APK(s)"

6. **安装运行**
   生成的APK位于: `app/build/outputs/apk/debug/app-debug.apk`

### 手动下载Gradle Wrapper（可选）

如果网络较慢，可以手动下载Gradle Wrapper：
```bash
mkdir -p gradle/wrapper
wget https://services.gradle.org/distributions/gradle-8.4-bin.zip -O gradle/wrapper/gradle-wrapper.jar
```

## 项目结构

```
app/src/main/
├── java/com/paperscanner/app/
│   ├── scanner/
│   │   └── ImageProcessor.kt       # 图片处理核心算法
│   ├── ui/
│   │   ├── MainActivity.kt         # 主界面
│   │   ├── ScannerActivity.kt      # 扫描/相机界面
│   │   ├── PreviewActivity.kt      # 预览界面
│   │   ├── adapter/                # RecyclerView适配器
│   │   └── widget/
│   │       └── CropOverlayView.kt  # 可拖动裁剪框组件
│   ├── util/
│   │   ├── FileUtils.kt            # 文件操作工具
│   │   └── ImageUtils.kt           # 图片处理工具
│   └── viewmodel/
│       └── ScanViewModel.kt        # 扫描数据管理
└── res/
    ├── layout/                     # 布局文件
    ├── drawable/                   # 图标资源
    └── values/                    # 字符串、颜色、主题
```

## 使用说明

1. **开始扫描**: 点击主界面的"开始扫描"按钮
2. **调整裁剪框**: 拖动白色矩形框的四个橙色角点，调整到A4纸张边缘
3. **拍摄**: 点击底部中间的拍照按钮
4. **继续拍摄**: 可继续拍摄更多页面
5. **完成扫描**: 点击"完成扫描"按钮进入预览界面
6. **保存分享**: 在预览界面可以删除单页、继续拍摄、保存到相册或分享

## 算法说明

图片增强流程：
1. **裁剪**: 根据用户选择的矩形框进行透视变换裁剪
2. **灰度化**: 将彩色图片转换为灰度图
3. **高斯模糊**: 使用7x7的高斯核进行模糊，减少噪点
4. **锐化**: 使用锐化卷积核增强边缘细节
5. **自适应阈值**: 使用21x15的自适应高斯阈值进行二值化，得到清晰的黑白文档效果

## License

MIT License

## 致谢

- 图片增强算法参考 [OpenCV Document Scanner](https://github.com/andrewdcampbell/OpenCV-Document-Scanner)
