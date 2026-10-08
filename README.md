# Roboparty Joystick

**English** | [简体中文](README_cn.md)

Android-based robot remote-control joystick app with UDP/BLE dual-channel communication, gamepad input, real-time control, and OTA self-update.

## Features

### Core Control
- **Dual-channel communication**: UDP (low latency) and BLE (Bluetooth Low Energy)
- **Gamepad input**: physical gamepads supported (sticks, buttons, D-pad)
- **Touch control**: virtual joystick discs with touch-drag support
- **High-frequency sending**: 50Hz control packet rate (20ms/frame)
- **Button mapping**: A/B/X/Y, LB/RB/LT/RT, D-pad, stick press (L3/R3)
- **Emergency stop**: manual e-stop button with three-step confirmation protocol (request → accepted → received) and live status feedback (sending / waiting / confirmed / failed)

### Authentication & Security
- **Three-step authentication**: AuthRequest → Challenge → ChallengeResponse → Token
- **HMAC-SHA256 signing**: challenge-response authentication based on a pre-shared key
- **Automatic token renewal**: reconnects automatically when the token expires
- **Heartbeat keep-alive**: periodic heartbeat for connection status monitoring

### Skill Action System
- **Reliable delivery**: two-phase ACK mechanism (ACK1 = received, ACK2 = executed)
- **Single-action constraint**: only one action can run at a time
- **Predefined actions**: handshake, bow, wave, nod, dance, and more
- **Swipe-triggered**: swipe gestures in the skill library panel trigger actions

### OTA Self-Update
- **Version check**: query the latest version over the UDP protocol
- **HTTP download**: APK download with progress display and MD5 verification
- **System installer**: invokes the system installer via FileProvider
- **Independent config**: OTA server address is configurable separately

### UI
- **Main screen**: robot Lottie animation, posture switcher, dual joysticks, status bar, side control bar
- **Skill library**: expressions / actions / talents categories with swipe-triggered execution
- **Data panels**: BMS battery data (voltage, current, SOC, temperature), IMU attitude angles (pitch/roll/yaw), motor status
- **Settings panel**: network, authorization password, motion, voice, volume, about, OTA update, reset

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| UI framework | Jetpack Compose |
| Architecture | MVI (Model-View-Intent) |
| Dependency injection | Hilt |
| Serialization | kotlinx.serialization |
| Coroutines | Kotlin Coroutines + Flow |
| Min API | 28 (Android 9.0) |
| Target API | 34 (Android 14) |

## Project Structure

```
app/src/main/java/com/gamepad/controller/
├── data/
│   ├── config/
│   │   └── ConnectionConfig.kt          # Connection config (UDP/BLE/OTA/fail-safe)
│   ├── model/
│   │   ├── BatteryData.kt               # BMS battery data
│   │   ├── BleDevice.kt                 # BLE device info
│   │   ├── DeviceProfile.kt             # Gamepad device profile (axis/deadzone/button mapping)
│   │   ├── ImuData.kt                   # IMU sensor data
│   │   ├── MotorStatus.kt               # Motor status
│   │   ├── SkillAction.kt               # Skill action definitions
│   │   ├── enums/
│   │   │   ├── ConnectionStatus.kt      # Connection status enum
│   │   │   └── GamepadButton.kt         # Gamepad button enum
│   │   └── packet/
│   │       ├── AuthPacket.kt            # Authentication packet
│   │       ├── AxesState.kt             # Stick/trigger axis state
│   │       ├── ControlPacket.kt         # Control packet
│   │       ├── OtaPacket.kt             # OTA version check packet
│   │       ├── SkillActionPacket.kt     # Skill action packet
│   │       └── StopPacket.kt            # E-stop packet (three-step confirmation)
│   └── service/
│       ├── BleConnectionManager.kt      # BLE connection manager
│       ├── BleControlChannel.kt         # BLE control channel
│       ├── CryptoHelper.kt              # HMAC-SHA256 signing
│       ├── GamepadInputManager.kt       # Gamepad input manager
│       ├── OtaManager.kt                # OTA update manager
│       ├── ReliableActionSender.kt      # Reliable action sender
│       └── UdpControlChannel.kt         # UDP control channel
├── di/
│   ├── AppModule.kt                     # Application module
│   └── NetworkModule.kt                 # Network module
├── domain/
│   ├── model/
│   │   ├── ActionRequestState.kt        # Action request state
│   │   ├── OtaState.kt                  # OTA state
│   │   ├── RawInput.kt                  # Raw gamepad input
│   │   ├── UiIntent.kt                  # UI intents
│   │   └── UiState.kt                   # UI state
│   └── service/
│       ├── ControlChannel.kt            # Control channel interface
│       └── GamepadInputService.kt       # Gamepad input interface
└── presentation/
    ├── MainActivity.kt                  # Main Activity
    ├── RobotControlViewModel.kt         # Main ViewModel
    ├── components/
    │   ├── MainScreen.kt                # Main screen
    │   ├── RobotViewer.kt               # Robot Lottie animation
    │   ├── JoystickDisc.kt              # Virtual joystick
    │   ├── PostureSwitcher.kt           # Posture switcher
    │   ├── SideControlBar.kt            # Side control bar
    │   ├── SkillLibraryPanel.kt         # Skill library panel
    │   ├── NotificationPanel.kt         # Notification panel
    │   ├── ImuDataList.kt               # IMU data list
    │   ├── ImuDetailCard.kt             # IMU detail card
    │   ├── MotorStatusList.kt           # Motor status list
    │   ├── MotorDetailCard.kt           # Motor detail card
    │   ├── BleDeviceList.kt             # BLE device list
    │   ├── ConnectionDialog.kt          # Connection dialog
    │   ├── ConnectionModeSelector.kt    # Connection mode selector
    │   ├── SettingsPanel.kt             # Settings panel
    │   ├── FloatingBall.kt              # Floating ball
    │   ├── FloatingCard.kt              # Floating card
    │   ├── StatusBar.kt                 # Status bar
    │   ├── TopNavBar.kt                 # Top navigation bar
    │   └── settings/
    │       ├── NetworkSettings.kt       # Network settings
    │       ├── AuthPasswordSettings.kt  # Authorization password settings
    │       ├── MotionSettings.kt        # Motion settings
    │       ├── VoiceSettings.kt         # Voice settings
    │       ├── VolumeSettings.kt        # Volume settings
    │       ├── AboutSettings.kt         # About page
    │       └── OtaSettings.kt           # OTA update page
    └── theme/
        ├── Color.kt                     # Color definitions
        └── Theme.kt                     # Theme definitions
```

## Configuration

The configuration file is located at `app/src/main/assets/connection_config.json`:

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
  "ble": {
    "service_uuid": "0000fff0-0000-1000-8000-00805f9b34fb",
    "tx_characteristic_uuid": "0000fff1-0000-1000-8000-00805f9b34fb",
    "rx_characteristic_uuid": "0000fff2-0000-1000-8000-00805f9b34fb",
    "scan_timeout_ms": 10000,
    "mtu_size": 512
  },
  "ota": {
    "host": "10.43.21.32",
    "port": 9001,
    "timeout_ms": 5000,
    "http_base_url": "http://10.43.21.32:8080/releases"
  }
}
```

| Section | Description |
|---------|-------------|
| control | Control server address and port |
| controlParams | Control parameters (velocity, frequency) |
| failSafe | Fail-safe parameters (gamepad input timeout, stale packet threshold) |
| auth | Authentication, heartbeat, and reconnection parameters |
| ble | Bluetooth connection parameters (service/characteristic UUIDs, MTU) |
| ota | OTA update server configuration |

## Communication Protocol

### Control Packet

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
  "dpad_up": false,
  "dpad_down": false,
  "dpad_left": false,
  "dpad_right": false
}
```

### Skill Action

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

### E-Stop Protocol

Three-step confirmation protocol to ensure reliable delivery of the e-stop command:

```
Client → Server: { type: "stop", status: "request" }
Server → Client: { type: "stop", status: "accepted" }
Client → Server: { type: "stop", status: "received" }
```

UI status feedback: sending → waiting for confirmation → confirmed (or failed).

### Authentication Flow

```
Client → Server: { type: "auth_request", device_id: "xxx" }
Server → Client: { type: "challenge", challenge_code: "xxx" }
Client → Server: { type: "challenge_response", signature: "HMAC-SHA256(...)" }
Server → Client: { type: "auth_result", ok: true, token: "xxx" }
```

## Build & Run

### Requirements

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Build

```bash
./gradlew assembleDebug
```

### Install

```bash
./gradlew installDebug
```

## Permissions

| Permission | Purpose |
|------------|---------|
| INTERNET | UDP network communication |
| ACCESS_NETWORK_STATE | Network state detection |
| CHANGE_NETWORK_STATE | Network connection changes |
| ACCESS_WIFI_STATE | WiFi state detection |
| CHANGE_WIFI_STATE | WiFi connection changes |
| BLUETOOTH_SCAN | BLE device scanning |
| BLUETOOTH_CONNECT | BLE device connection |
| ACCESS_FINE_LOCATION | WiFi/BLE scanning |
| ACCESS_COARSE_LOCATION | Coarse location (BLE scanning compatibility) |
| VIBRATE | Gamepad vibration feedback |
| REQUEST_INSTALL_PACKAGES | OTA APK installation |

## License

[GPL-3.0-or-later](LICENSE)
