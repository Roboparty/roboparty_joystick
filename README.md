# Roboparty Joystick

基于 Android 的机器人遥控手柄应用，支持 UDP/BLE 双通道通信、手柄输入、实时控制和 OTA 自更新。

## 功能特性

### 核心控制
- **双通道通信**：UDP（低延迟）和 BLE（蓝牙低功耗）两种连接方式
- **手柄输入**：支持物理游戏手柄（摇杆、按键、十字键）
- **触屏操控**：虚拟摇杆圆盘，支持触屏拖拽控制
- **高频发送**：50Hz 控制包发送频率（20ms/帧）
- **急停保护**：手柄断开或后台自动触发急停

### 认证与安全
- **三步认证**：AuthRequest → Challenge → ChallengeResponse → Token
- **HMAC-SHA256 签名**：基于预共享密钥的挑战-响应认证
- **Token 自动续期**：Token 过期自动重连
- **心跳保活**：定期心跳检测连接状态

### 技能动作系统
- **可靠传输**：两阶段 ACK 确认机制（ACK1 收到确认 + ACK2 执行完成）
- **单动作约束**：同一时间只能执行一个动作
- **预定义动作**：握手、鞠躬、挥手、点头、跳舞等
- **滑动执行**：技能库面板支持滑动手势触发动作

### OTA 自更新
- **版本检查**：UDP 协议查询最新版本
- **HTTP 下载**：APK 文件下载，支持进度显示和 MD5 校验
- **系统安装**：通过 FileProvider 调用系统安装器
- **独立配置**：OTA 服务器地址可独立配置

### UI 界面
- **主屏幕**：机器人 Lottie 动画、姿态切换、双摇杆、状态栏
- **技能库**：表情/动作/才艺分类，滑动执行
- **通知面板**：BMS 电池数据展示（电压、电流、SOC、温度）
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

## 项目结构

```
app/src/main/java/com/gamepad/controller/
├── data/
│   ├── config/
│   │   └── ConnectionConfig.kt          # 连接配置（UDP/BLE/OTA）
│   ├── model/
│   │   ├── BatteryData.kt               # BMS 电池数据
│   │   ├── BleDevice.kt                 # BLE 设备信息
│   │   ├── ImuData.kt                   # IMU 传感器数据
│   │   ├── MotorStatus.kt               # 电机状态
│   │   ├── SkillAction.kt               # 技能动作定义
│   │   └── packet/
│   │       ├── AuthPacket.kt            # 认证数据包
│   │       ├── ControlPacket.kt         # 控制数据包
│   │       ├── OtaPacket.kt             # OTA 版本检查数据包
│   │       └── SkillActionPacket.kt     # 技能动作数据包
│   └── service/
│       ├── BleConnectionManager.kt      # BLE 连接管理
│       ├── BleControlChannel.kt         # BLE 控制通道
│       ├── CryptoHelper.kt              # HMAC-SHA256 签名
│       ├── GamepadInputManager.kt       # 手柄输入管理
│       ├── OtaManager.kt                # OTA 更新管理器
│       ├── ReliableActionSender.kt      # 可靠动作发送器
│       └── UdpControlChannel.kt         # UDP 控制通道
├── di/
│   ├── AppModule.kt                     # 应用模块
│   └── NetworkModule.kt                 # 网络模块
├── domain/
│   ├── model/
│   │   ├── ActionRequestState.kt        # 动作请求状态
│   │   ├── OtaState.kt                  # OTA 状态
│   │   ├── UiIntent.kt                  # UI 意图
│   │   └── UiState.kt                   # UI 状态
│   └── service/
│       ├── ControlChannel.kt            # 控制通道接口
│       └── GamepadInputService.kt       # 手柄输入接口
└── presentation/
    ├── RobotControlViewModel.kt         # 主 ViewModel
    └── components/
        ├── MainScreen.kt                # 主屏幕
        ├── JoystickDisc.kt              # 虚拟摇杆
        ├── PostureSwitcher.kt           # 姿态切换
        ├── SkillLibraryPanel.kt         # 技能库面板
        ├── NotificationPanel.kt         # 通知面板
        ├── SettingsPanel.kt             # 设置面板
        ├── FloatingCard.kt              # 悬浮卡片
        ├── StatusBar.kt                 # 状态栏
        ├── TopNavBar.kt                 # 顶部导航
        └── settings/
            ├── NetworkSettings.kt       # 网络设置
            ├── AuthPasswordSettings.kt  # 授权密码设置
            ├── MotionSettings.kt        # 运动设置
            ├── VoiceSettings.kt         # 语音设置
            ├── VolumeSettings.kt        # 音量设置
            ├── AboutSettings.kt         # 关于页面
            └── OtaSettings.kt           # OTA 更新页面
```

## 配置说明

配置文件位于 `app/src/main/assets/connection_config.json`：

```json
{
  "control": {
    "host": "10.43.21.32",
    "port": 9000
  },
  "controlParams": {
    "max_linear_velocity": 1.5,
    "max_angular_velocity": 3.0,
    "send_frequency_hz": 50
  },
  "auth": {
    "timeout_ms": 5000,
    "heartbeat_interval_ms": 500,
    "heartbeat_timeout_ms": 5000
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
| auth | 认证、心跳、重连参数 |
| ble | 蓝牙连接参数 |
| ota | OTA 更新服务器配置 |

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
  "dpad_up": false,
  "dpad_down": false,
  "dpad_left": false,
  "dpad_right": false
}
```

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

### 认证流程

```
客户端 → 服务器: { type: "auth_request", device_id: "xxx" }
服务器 → 客户端: { type: "challenge", challenge_code: "xxx" }
客户端 → 服务器: { type: "challenge_response", signature: "HMAC-SHA256(...)" }
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
| INTERNET | UDP 网络通信 |
| ACCESS_NETWORK_STATE | 网络状态检测 |
| BLUETOOTH_SCAN | BLE 设备扫描 |
| BLUETOOTH_CONNECT | BLE 设备连接 |
| ACCESS_FINE_LOCATION | WiFi/BLE 扫描 |
| VIBRATE | 手柄震动反馈 |
| REQUEST_INSTALL_PACKAGES | OTA APK 安装 |

## License

GPL-3.0 License
