# OTA 自更新功能设计文档

## 1. 概述

为手柄 App 添加 OTA（Over-The-Air）自更新功能，支持从服务器检查新版本、下载 APK 并完成安装更新。

### 1.1 功能目标

- 通过 UDP 协议向**可配置的 OTA 服务器**查询最新版本信息
- 通过 HTTP 下载 APK 文件（支持进度显示和 MD5 校验）
- 调用系统安装器完成 APK 安装
- UI 集成到设置面板中
- OTA 服务器地址独立配置，与控制服务器解耦

### 1.2 技术选型

| 模块 | 方案 | 说明 |
|------|------|------|
| 版本检查 | UDP | 独立 UDP socket，可配置服务器地址 |
| APK 下载 | HTTP | `HttpURLConnection`，无需额外依赖 |
| 文件校验 | MD5 | 下载完成后校验完整性 |
| APK 安装 | PackageInstaller | Android API 28+ 标准方式 |
| UI 框架 | Jetpack Compose | 与现有 UI 保持一致 |

### 1.3 OTA 服务器配置

OTA 服务器地址通过 `connection_config.json` 独立配置，与控制服务器解耦：

```json
{
  "ota": {
    "host": "10.43.21.32",
    "port": 9001,
    "timeout_ms": 5000,
    "http_base_url": "http://10.43.21.32:8080/releases"
  }
}
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| host | string | "10.43.21.32" | OTA 版本检查服务器 IP |
| port | int | 9001 | OTA 版本检查服务器端口 |
| timeout_ms | long | 5000 | 版本检查超时时间（毫秒） |
| http_base_url | string | "" | APK 下载 HTTP 基础地址（空则使用 host:8080） |

**设计说明**：
- 版本检查使用 UDP 协议，发送到 `ota.host:ota.port`
- APK 下载使用 HTTP 协议，地址从服务器响应的 `download_url` 获取
- 若服务器未返回 `download_url`，则使用 `http_base_url` 拼接
- OTA 服务器与控制服务器可以是同一台，也可以是不同服务器

---

## 2. 整体流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        OTA 更新流程                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  用户                                                          │
│    │                                                            │
│    ▼                                                            │
│  点击"检查更新"                                                 │
│    │                                                            │
│    ▼                                                            │
│  ┌─────────────────┐     UDP 版本检查请求     ┌──────────────┐ │
│  │  OtaManager     │ ──────────────────────→ │   服务器      │ │
│  │                 │                          │              │ │
│  │                 │ ◀────────────────────── │              │ │
│  └─────────────────┘   版本检查响应           └──────────────┘ │
│    │                                                            │
│    ├── 无更新 → 显示"已是最新版本"                              │
│    │                                                            │
│    └── 有更新 → 显示版本信息 + 更新日志                        │
│         │                                                       │
│         ▼                                                       │
│       点击"下载并安装"                                          │
│         │                                                       │
│         ▼                                                       │
│  ┌─────────────────┐     HTTP GET              ┌──────────────┐│
│  │  OtaManager     │ ──────────────────────→ │  文件服务器   ││
│  │  (下载进度)     │ ◀────────────────────── │              ││
│  └─────────────────┘   APK 文件分块传输        └──────────────┘│
│    │                                                            │
│    ▼                                                            │
│  MD5 校验                                                      │
│    │                                                            │
│    ├── 校验失败 → 提示重试                                      │
│    │                                                            │
│    └── 校验成功 → 调用系统安装器                                │
│         │                                                       │
│         ▼                                                       │
│       完成更新                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 通信协议

### 3.1 版本检查请求（客户端 → 服务器）

```json
{
    "type": "ota_version_check",
    "current_version": "1.0.0",
    "device_id": "android_gamepad_001",
    "timestamp": 1723024800000
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| type | string | 固定值 `"ota_version_check"` |
| current_version | string | 当前 App 版本号（`versionName`） |
| device_id | string | 设备唯一标识 |
| timestamp | long | 请求时间戳（毫秒） |

### 3.2 版本检查响应（服务器 → 客户端）

```json
{
    "type": "ota_version_check_response",
    "has_update": true,
    "latest_version": "1.1.0",
    "download_url": "http://10.43.21.32:8080/releases/app-v1.1.0.apk",
    "file_size": 15728640,
    "md5": "a1b2c3d4e5f6...",
    "changelog": "- 修复了控制延迟问题\n- 新增语音控制功能\n- 优化 UI 交互体验"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| type | string | 固定值 `"ota_version_check_response"` |
| has_update | boolean | 是否有新版本 |
| latest_version | string | 最新版本号（有更新时必填） |
| download_url | string | APK 下载地址（有更新时必填） |
| file_size | long | 文件大小（字节） |
| md5 | string | 文件 MD5 校验值 |
| changelog | string | 更新日志（支持 `\n` 换行） |

---

## 4. 数据模型

### 4.1 OtaPacket.kt

```kotlin
@Serializable
data class OtaVersionCheckRequest(
    @SerialName("type") val type: String = "ota_version_check",
    @SerialName("current_version") val currentVersion: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class OtaVersionCheckResponse(
    @SerialName("type") val type: String,
    @SerialName("has_update") val hasUpdate: Boolean,
    @SerialName("latest_version") val latestVersion: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    @SerialName("md5") val md5: String? = null,
    @SerialName("changelog") val changelog: String? = null
)
```

### 4.2 OtaConfig.kt（OTA 服务器配置）

```kotlin
/**
 * OTA 配置 — 从 connection_config.json 加载
 *
 * 控制 OTA 版本检查和下载的参数
 */
@Serializable
data class OtaConfig(
    /** OTA 版本检查服务器 IP */
    @SerialName("host") val host: String = "10.43.21.32",
    /** OTA 版本检查服务器端口 */
    @SerialName("port") val port: Int = 9001,
    /** 版本检查超时时间（毫秒） */
    @SerialName("timeout_ms") val timeoutMs: Long = 5000,
    /** APK 下载 HTTP 基础地址（空则使用服务器响应的 download_url） */
    @SerialName("http_base_url") val httpBaseUrl: String = ""
)
```

**在 ConnectionConfig 中添加**：

```kotlin
@Serializable
data class ConnectionConfig(
    val control: ControlConfig = ControlConfig(),
    val controlParams: ControlParamsConfig = ControlParamsConfig(),
    val failSafe: FailSafeConfig = FailSafeConfig(),
    val auth: AuthConfig = AuthConfig(),
    val ble: BleConfig = BleConfig(),
    val ota: OtaConfig = OtaConfig()  // 新增
)
```

### 4.3 connection_config.json 示例

```json
{
  "control": {
    "host": "10.43.21.32",
    "port": 9000
  },
  "ota": {
    "host": "10.43.21.32",
    "port": 9001,
    "timeout_ms": 5000,
    "http_base_url": "http://10.43.21.32:8080/releases"
  }
}
```

### 4.4 OtaState.kt

```kotlin
data class OtaState(
    val currentVersion: String = "1.0.0",
    val isChecking: Boolean = false,
    val hasUpdate: Boolean = false,
    val latestVersion: String? = null,
    val downloadUrl: String? = null,
    val fileSize: Long? = null,
    val md5: String? = null,
    val changelog: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,      // 0.0 ~ 1.0
    val downloadError: String? = null,
    val isDownloadComplete: Boolean = false
)
```

---

## 5. 核心模块设计

### 5.1 OtaManager

负责 OTA 全流程的核心管理器，使用独立的 UDP socket 进行版本检查。

```
OtaManager
├── checkForUpdate()        // UDP 版本检查（独立 socket）
├── downloadApk(url, file)  // HTTP 下载
├── verifyMd5(file, md5)    // MD5 校验
├── installApk(file)        // 调用系统安装器
└── otaState: StateFlow     // 状态暴露
```

#### 5.1.1 版本检查

```kotlin
suspend fun checkForUpdate(): OtaVersionCheckResponse? {
    // 1. 从配置读取 ota.host 和 ota.port
    // 2. 创建临时 UDP socket（独立于控制通道）
    // 3. 序列化 OtaVersionCheckRequest
    // 4. 发送到 ota.host:ota.port
    // 5. 等待响应（ota.timeout_ms 超时）
    // 6. 解析 OtaVersionCheckResponse
    // 7. 更新 otaState
    // 8. 关闭临时 socket
}
```

#### 5.1.2 HTTP 下载

```kotlin
suspend fun downloadApk(url: String, outputFile: File) {
    // 1. 打开 HTTP 连接
    // 2. 分块读取（8KB buffer）
    // 3. 写入本地文件
    // 4. 计算下载进度并更新 otaState
    // 5. 计算 MD5
    // 6. 校验文件完整性
}
```

#### 5.1.3 APK 安装

```kotlin
fun installApk(apkFile: File) {
    // 1. 通过 FileProvider 获取 content:// URI
    // 2. 创建 ACTION_VIEW intent
    // 3. 设置 FLAG_GRANT_READ_URI_PERMISSION
    // 4. 启动安装界面
}
```

### 5.2 与控制通道解耦

**设计决策**：OTA 版本检查使用独立的 UDP socket，不复用控制通道的 socket。

**原因**：
- 控制通道可能未连接（用户可能在未连接机器人时检查更新）
- OTA 检查不应影响控制通道的状态
- OTA 服务器地址可能与控制服务器不同
- 避免控制通道的认证状态干扰 OTA 流程

```kotlin
class OtaManager(
    private val context: Context,
    private val config: ConnectionConfig  // 包含 ota 配置
) {
    private val otaHost = config.ota.host
    private val otaPort = config.ota.port
    private val otaTimeoutMs = config.ota.timeoutMs

    suspend fun checkForUpdate(): OtaVersionCheckResponse? {
        return withContext(Dispatchers.IO) {
            val socket = DatagramSocket()
            try {
                socket.soTimeout = otaTimeoutMs.toInt()
                val request = OtaVersionCheckRequest(
                    currentVersion = BuildConfig.VERSION_NAME,
                    deviceId = getDeviceId()
                )
                val json = Json.encodeToString(request)
                val data = json.toByteArray()
                val packet = DatagramPacket(
                    data, data.size,
                    InetAddress.getByName(otaHost), otaPort
                )
                socket.send(packet)

                val buffer = ByteArray(4096)
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)

                val responseJson = String(response.data, 0, response.length)
                Json.decodeFromString<OtaVersionCheckResponse>(responseJson)
            } finally {
                socket.close()
            }
        }
    }
}
```

---

## 6. UI 设计

### 6.1 设置面板导航

```
┌──────────────┐
│  ← 设置      │
│              │
│ ◉ 网络       │
│ ○ 授权密码   │
│ ○ 运动       │
│ ○ 语音       │
│ ○ 音量       │
│ ○ 关于       │
│ ○ OTA更新    │  ← 新增
│ ○ 还原       │
└──────────────┘
```

### 6.2 OTA 更新页面

```
┌────────────────────────────────────────────────────────┐
│                                                        │
│  固件更新                                              │
│                                                        │
│  当前版本：v1.0.0                                      │
│                                                        │
│  ┌──────────────────────────────────────────────────┐ │
│  │              [ 检查更新 ]                        │ │
│  └──────────────────────────────────────────────────┘ │
│                                                        │
│  ────────── 发现新版本 v1.1.0 ──────────             │
│                                                        │
│  更新内容：                                            │
│  · 修复了控制延迟问题                                  │
│  · 新增语音控制功能                                    │
│  · 优化 UI 交互体验                                    │
│                                                        │
│  文件大小：15.2 MB                                     │
│                                                        │
│  ┌──────────────────────────────────────────────────┐ │
│  │              [ 下载并安装 ]                      │ │
│  └──────────────────────────────────────────────────┘ │
│                                                        │
│  ████████████████████░░░░░░░░░░  65%  (9.9MB/15.2MB) │
│                                                        │
│  ┌──────────────────────────────────────────────────┐ │
│  │              [ 安装更新 ]                        │ │
│  └──────────────────────────────────────────────────┘ │
│  （仅在下载完成后显示）                                │
│                                                        │
└────────────────────────────────────────────────────────┘
```

### 6.3 状态流转

```
初始状态
  │
  ▼
点击"检查更新" → isChecking = true
  │
  ├── 无更新 → hasUpdate = false → 显示"已是最新版本"
  │
  └── 有更新 → hasUpdate = true
       │        显示版本信息 + 更新日志
       ▼
     点击"下载并安装" → isDownloading = true
       │
       ├── 下载失败 → downloadError = "错误信息"
       │                显示"重试"按钮
       │
       └── 下载成功 → isDownloadComplete = true
            │          MD5 校验
            ▼
          点击"安装更新"
            │
            ▼
          系统安装界面
```

---

## 7. 文件清单

### 7.1 新增文件

| 文件路径 | 说明 |
|----------|------|
| `app/src/main/java/.../data/model/packet/OtaPacket.kt` | OTA 请求/响应数据包 |
| `app/src/main/java/.../domain/model/OtaState.kt` | OTA 状态模型 |
| `app/src/main/java/.../data/service/OtaManager.kt` | OTA 核心管理器 |
| `app/src/main/java/.../presentation/components/settings/OtaSettings.kt` | OTA 设置页面 UI |
| `app/src/main/res/xml/file_paths.xml` | FileProvider 路径配置 |

### 7.2 修改文件

| 文件路径 | 修改内容 |
|----------|----------|
| `ConnectionConfig.kt` | 添加 `OtaConfig` 数据类和 `ota` 字段 |
| `connection_config.json` | 添加 `ota` 配置段 |
| `AuthPacket.kt` | `ServerMessage` 添加 OTA 响应字段 |
| `ControlChannel.kt` | 接口添加 `sendOtaVersionCheck()` |
| `UdpControlChannel.kt` | 实现版本检查收发 + 回调 |
| `BleControlChannel.kt` | 空实现 |
| `RobotControlViewModel.kt` | 集成 OtaManager，暴露状态 |
| `UiState.kt` | 添加 `otaState` 字段 |
| `SettingsPanel.kt` | 添加"OTA更新"导航项 |
| `AndroidManifest.xml` | 添加权限 + FileProvider |

---

## 8. 权限与配置

### 8.1 AndroidManifest.xml

```xml
<!-- APK 安装权限 (API 26+) -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

<!-- FileProvider 声明 -->
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### 8.2 file_paths.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path
        name="ota_updates"
        path="updates/" />
</paths>
```

---

## 9. 执行顺序

| 步骤 | 文件 | 说明 |
|------|------|------|
| 1 | `OtaPacket.kt` | 创建数据包模型 |
| 2 | `OtaState.kt` | 创建状态模型 |
| 3 | `ConnectionConfig.kt` | 添加 OtaConfig 数据类 |
| 4 | `connection_config.json` | 添加 ota 配置段 |
| 5 | `AuthPacket.kt` | 扩展 ServerMessage |
| 6 | `ControlChannel.kt` | 接口添加方法 |
| 7 | `UdpControlChannel.kt` | 实现版本检查 |
| 8 | `BleControlChannel.kt` | 空实现 |
| 9 | `OtaManager.kt` | 核心下载和安装逻辑 |
| 10 | `UiState.kt` | 添加 otaState |
| 11 | `RobotControlViewModel.kt` | 集成 OtaManager |
| 12 | `OtaSettings.kt` | OTA 页面 UI |
| 13 | `SettingsPanel.kt` | 添加导航项 |
| 14 | `AndroidManifest.xml` | 权限和 FileProvider |
| 15 | `file_paths.xml` | FileProvider 路径 |
| 16 | 编译验证 | `./gradlew assembleDebug` |

---

## 10. 注意事项

### 10.1 安全性

- APK 下载地址必须使用 HTTPS（生产环境）
- MD5 校验防止文件篡改
- FileProvider 限制文件访问范围

### 10.2 兼容性

- 最低支持 API 28（`minSdk = 28`）
- `REQUEST_INSTALL_PACKAGES` 权限需要用户手动授权
- 部分设备可能需要开启"允许安装未知来源应用"

### 10.3 错误处理

- 网络超时：版本检查 3 秒超时，下载支持断点重试
- 文件校验：MD5 不匹配时提示重新下载
- 权限拒绝：引导用户开启安装权限

### 10.4 存储

- APK 临时存储在 `context.getExternalFilesDir("updates")`
- 安装完成后可选择清理临时文件
- 需要足够的存储空间（检查 `file_size`）
