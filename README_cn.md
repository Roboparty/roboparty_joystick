<a id="top"></a>

<p align="center">
  <a href="README.md">English</a> &nbsp; / &nbsp; <strong>简体中文</strong>
</p>

<a href="https://github.com/Roboparty/roboparty_joystick/releases/latest">
  <img src="assets/hero.jpg" alt="Roboparty Joystick 控制界面" width="100%">
</a>

<h1 align="center">Roboparty Joystick</h1>

<p align="center">
  面向 Roboparty 机器人的 Android 控制端。<br>
  物理手柄、触屏摇杆、WSS/JSON 机器人链路、遥测和技能动作集中在一个控制台中。
</p>

<p align="center">
  <a href="https://github.com/Roboparty/roboparty_joystick/actions/workflows/build-apk.yml"><img src="https://img.shields.io/github/actions/workflow/status/Roboparty/roboparty_joystick/build-apk.yml?branch=main&style=flat-square&label=%E6%9E%84%E5%BB%BA" alt="构建"></a>
  <a href="https://github.com/Roboparty/roboparty_joystick/releases/latest"><img src="https://img.shields.io/github/v/release/Roboparty/roboparty_joystick?display_name=tag&sort=semver&style=flat-square&label=%E6%9C%80%E6%96%B0%E7%89%88%E6%9C%AC" alt="最新版本"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0--or--later-2563EB?style=flat-square" alt="许可证"></a>
  <a href="obtainium://add/https://github.com/Roboparty/roboparty_joystick"><img src="https://img.shields.io/badge/Get%20it%20on-Obtainium-7C3AED?style=flat-square&logo=android&logoColor=white" alt="Get it on Obtainium"></a>
  <a href="https://github.com/Roboparty/roboparty_joystick/releases/latest"><img src="https://img.shields.io/badge/%E4%B8%8B%E8%BD%BD-%E6%9C%80%E6%96%B0%20APK-16A34A?style=flat-square&logo=github&logoColor=white" alt="下载最新 APK"></a>
</p>

---

## 不被黑盒限制的控制台

**Roboparty Joystick 是面向真实机器人的 Android 操作控制台。** 它把 WSS/JSON 机器人链路、手柄输入、触屏控制、遥测、技能动作和带锁存的软件停止流程集中到一个界面中。

<table width="100%">
<thead>
<tr><th align="left" width="33%">控制</th><th align="left" width="33%">连接</th><th align="left" width="34%">运行</th></tr>
</thead>
<tbody>
<tr><td valign="top">双虚拟摇杆、物理手柄、姿态切换和 50 Hz 控制包发送。</td><td valign="top">WSS/JSON 是 rp_server 的传输方式。认证使用配对密钥的 ECDSA 挑战（Android Keystore）。</td><td valign="top">电池、IMU 和电机状态面板，可靠技能动作，软件停止锁存和 OTA 更新。</td></tr>
</tbody>
</table>

<p align="center"><sub>让操作员在同一个视图里看到机器人状态和下一步动作。</sub></p>

## 发布与更新

点击上方 Obtainium 徽章，或打开最新 GitHub Release。带版本标签的 CI 构建会发布签名 `app-release.apk` 和 SHA-256 校验文件。`KEYSTORE_BASE64` 必须保存持久化的 release 签名密钥，确保后续版本可以正常升级。

## 功能特性

### 核心控制
- **控制通道**：WSS/JSON（`/ws-json`），带显式控制租约
- **手柄输入**：支持物理游戏手柄（摇杆、按键、十字键）
- **触屏操控**：虚拟摇杆圆盘，支持触屏拖拽控制
- **高频发送**：50Hz 控制包发送频率（20ms/帧）
- **按键映射**：A/B/X/Y、LB/RB/LT/RT、十字键、摇杆按下（L3/R3）
- **急停保护**：手动急停按钮，三步确认协议（request → accepted → received），状态实时反馈（发送中/等待确认/已确认/失败）

### 认证与安全
- **三步认证**：AuthRequest → Challenge → ChallengeResponse → Token
- **挑战-响应认证**：WSS/JSON 使用 Android Keystore 的 ECDSA-P256，由已配对 App 公钥验签
- **密钥存储**：密钥在「设置 → 授权与绑定」中设置，使用 Android Keystore 加密，APK 内不再硬编码
- **SN 绑定**：认证前读取 `GET /identity`（SN 来自 TUI）；SN 变化会拒绝连接，需清除绑定后重新配对
- **一次性配对码**：机器人运行 `rp-server-pair code`，App 在「授权与绑定」输入 6 位码登记 App 公钥（EC P-256，Keystore 私钥）
- **Token 自动续期**：Token 过期自动重连
- **心跳保活**：定期心跳检测连接状态

### 技能动作系统
- **可靠传输**：两阶段 ACK 确认机制（ACK1 收到确认 + ACK2 执行完成）
- **单动作约束**：同一时间只能执行一个动作
- **预定义动作**：握手、鞠躬、挥手、点头、跳舞等
- **滑动执行**：技能库面板支持滑动手势触发动作

### OTA 自更新
- **版本检查**：通过独立的 OTA 服务 UDP 查询最新版本
- **HTTP 下载**：APK 文件下载，支持进度显示和 MD5 校验
- **系统安装**：通过 FileProvider 调用系统安装器
- **独立配置**：OTA 服务器地址可独立配置

### UI 界面
- **主屏幕**：机器人 Lottie 动画、姿态切换、双摇杆、状态栏、侧边控制栏
- **技能库**：表情/动作/才艺分类，滑动执行
- **数据面板**：BMS 电池数据（电压、电流、SOC、温度）、IMU 姿态角（俯仰/横滚/偏航）、电机状态
- **设置面板**：网络、授权密码、运动、语音、音量、关于、OTA 更新、还原

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose |
| 架构 | MVI（Model-View-Intent） |
| 依赖注入 | Hilt |
| 序列化 | kotlinx.serialization |
| 协程 | Kotlin Coroutines + Flow |
| 最低 API | 28（Android 9.0） |
| 目标 API | 34（Android 14） |

<!--
## 项目结构

```
app/src/main/java/com/gamepad/controller/
├── data/
│   ├── config/
│   │   └── ConnectionConfig.kt          # 连接配置（WSS/OTA/失效保护）
│   ├── model/
│   │   ├── BatteryData.kt               # BMS 电池数据
│   │   ├── DeviceProfile.kt             # 手柄设备配置（轴映射/死区/按键映射）
│   │   ├── ImuData.kt                   # IMU 传感器数据
│   │   ├── MotorStatus.kt               # 电机状态
│   │   ├── SkillAction.kt               # 技能动作定义
│   │   ├── enums/
│   │   │   ├── ConnectionStatus.kt      # 连接状态枚举
│   │   │   └── GamepadButton.kt         # 手柄按键枚举
│   │   └── packet/
│   │       ├── AxesState.kt             # 摇杆/扳机轴状态
│   │       ├── ControlPacket.kt         # 控制数据包
│   │       ├── OtaPacket.kt             # OTA 版本检查数据包
│   │       ├── SkillActionPacket.kt     # 技能动作数据包
│   └── service/
│       ├── GamepadInputManager.kt       # 手柄输入管理
│       ├── OtaManager.kt                # OTA 更新管理器
│       ├── ReliableActionSender.kt      # 可靠动作发送器
├── di/
│   ├── AppModule.kt                     # 应用模块
│   └── NetworkModule.kt                 # 网络模块
├── domain/
│   ├── model/
│   │   ├── ActionRequestState.kt        # 动作请求状态
│   │   ├── OtaState.kt                  # OTA 状态
│   │   ├── RawInput.kt                  # 手柄原始输入
│   │   ├── UiIntent.kt                  # UI 意图
│   │   └── UiState.kt                   # UI 状态
│   └── service/
│       ├── ControlChannel.kt            # 控制通道接口
│       └── GamepadInputService.kt       # 手柄输入接口
└── presentation/
    ├── MainActivity.kt                  # 主 Activity
    ├── RobotControlViewModel.kt         # 主 ViewModel
    ├── components/
    │   ├── MainScreen.kt                # 主屏幕
    │   ├── RobotViewer.kt               # 机器人 Lottie 动画
    │   ├── JoystickDisc.kt              # 虚拟摇杆
    │   ├── PostureSwitcher.kt           # 姿态切换
    │   ├── SideControlBar.kt            # 侧边控制栏
    │   ├── SkillLibraryPanel.kt         # 技能库面板
    │   ├── NotificationPanel.kt         # 通知面板
    │   ├── ImuDataList.kt               # IMU 数据列表
    │   ├── ImuDetailCard.kt             # IMU 详情卡片
    │   ├── MotorStatusList.kt           # 电机状态列表
    │   ├── MotorDetailCard.kt           # 电机详情卡片
    │   ├── ConnectionDialog.kt          # 连接对话框
    │   ├── SettingsPanel.kt             # 设置面板
    │   ├── FloatingBall.kt              # 悬浮球
    │   ├── FloatingCard.kt              # 悬浮卡片
    │   ├── StatusBar.kt                 # 状态栏
    │   ├── TopNavBar.kt                 # 顶部导航
    │   └── settings/
    │       ├── NetworkSettings.kt       # 网络设置
    │       ├── AuthPasswordSettings.kt  # 授权密码设置
    │       ├── MotionSettings.kt        # 运动设置
    │       ├── VoiceSettings.kt         # 语音设置
    │       ├── VolumeSettings.kt        # 音量设置
    │       ├── AboutSettings.kt         # 关于页面
    │       └── OtaSettings.kt           # OTA 更新页面
    └── theme/
        ├── Color.kt                     # 颜色定义
        └── Theme.kt                     # 主题定义
```

-->

## 配置说明

配置文件位于 `app/src/main/assets/connection_config.json`：

```json
{
  "control": {
    "host": "10.43.21.32",
    "port": 9000,
    "tls": false
  },
  "controlParams": {
    "max_linear_velocity": 1.5,
    "max_angular_velocity": 3.0,
    "send_frequency_hz": 50
  },
  "failSafe": {
    "gamepad_timeout_ms": 500,
    "stale_packet_ms": 300
  },
  "auth": {
    "timeout_ms": 5000,
    "retry_count": 3,
    "heartbeat_interval_ms": 500,
    "heartbeat_timeout_ms": 5000,
    "reconnect_delay_ms": 2000,
    "reconnect_max_count": 5
  },
  "ota": {
    "host": "10.43.21.32",
    "port": 9001,
    "timeout_ms": 5000,
    "http_base_url": "http://10.43.21.32:8080/releases"
  }
}
```

| 配置段 | 说明 |
|--------|------|
| control | 控制服务器地址和端口 |
| controlParams | 控制参数（速度、频率） |
| failSafe | 失效保护参数（手柄输入超时、数据包过期阈值） |
| auth | 认证、心跳、重连参数 |
| ota | OTA 更新服务器配置 |

> rp_server 只启用 `/ws-json`。`tls=false` 时使用 `ws://`，`tls=true` 时使用 `wss://` 并校验 `/identity.tlsSpkiSha256`。

## 通信协议

### 控制包

```json
{
  "type": "control",
  "token": "xxx",
  "timestamp": 1723024800000,
  "sequence": 1,
  "left_stick_x": 0.5,
  "left_stick_y": -0.3,
  "right_stick_x": 0.0,
  "right_stick_y": 0.0,
  "btn_a": false,
  "btn_b": false,
  "btn_x": false,
  "btn_y": false,
  "btn_lb": false,
  "btn_rb": false,
  "btn_lt": false,
  "btn_rt": false,
  "lt": 0.0,
  "rt": 0.0,
  "dpad_up": false,
  "dpad_down": false,
  "dpad_left": false,
  "dpad_right": false
}
```

`lt`/`rt` 为 `[0,1]` 的模拟扳机，rp_server 会映射为负的 `AXIS_LT/RT` 用于转向；`btn_lt`/`btn_rt` 作为数字量兜底。

### 技能动作

```json
{
  "type": "skill_action",
  "action_id": "handshake",
  "request_id": "uuid-xxx",
  "sequence": 1,
  "timestamp": 1723024800000,
  "token": "xxx"
}
```

### 软件停止协议

三步确认协议，确保急停指令可靠送达：

```
客户端 → 服务器: { type: "stop", status: "request" }
服务器 → 客户端: { type: "stop", status: "accepted" }
客户端 → 服务器: { type: "stop", status: "received" }
```

服务端会锁存这次软件停止，直到显式清除前拒绝新的 `lease.acquire`：

```
客户端 → 服务器: { type: "stop", status: "clear" }
服务器 → 客户端: { type: "stop", status: "cleared" }
```

清除后需要重新申请控制权。

UI 状态反馈：发送中 → 等待确认 → 已确认（或确认失败）。

### 认证流程

```
客户端 → 服务器: { type: "auth_request", device_id: "xxx" }
服务器 → 客户端: { type: "challenge", challenge_code: "xxx" }
客户端 → 服务器: { type: "challenge_response", signature: "ECDSA-P256(...)" }
服务器 → 客户端: { type: "auth_result", ok: true, token: "xxx" }
```

## 构建与运行

### 环境要求

- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 34

### 构建

```bash
./gradlew assembleDebug
```

### 安装

```bash
./gradlew installDebug
```

## 权限说明

| 权限 | 用途 |
|------|------|
| INTERNET | WSS 网络通信 |
| ACCESS_NETWORK_STATE | 网络状态检测 |
| CHANGE_NETWORK_STATE | 网络连接变更 |
| ACCESS_WIFI_STATE | WiFi 状态检测 |
| CHANGE_WIFI_STATE | WiFi 连接变更 |
| ACCESS_FINE_LOCATION | WiFi 扫描 |
| ACCESS_COARSE_LOCATION | 粗略定位（WiFi 扫描兼容） |
| VIBRATE | 手柄震动反馈 |
| REQUEST_INSTALL_PACKAGES | OTA APK 安装 |

## License

[GPL-3.0-or-later](LICENSE)
