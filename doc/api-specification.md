# 手柄控制器服务器 API 接口规范

## 概述

本文档定义了手柄控制器客户端与服务器之间的 UDP 通信接口规范。

- **协议**: UDP
- **默认端口**: 9000
- **编码**: UTF-8 JSON
- **通信模式**: 请求-响应（认证）、单向推送（控制/心跳）

---

## 1. 认证接口

### 1.1 认证请求 (AuthRequest)

**方向**: 客户端 → 服务器

```json
{
  "type": "auth_request",
  "device_id": "android-a1b2c3d4",
  "device_name": "Xbox Controller",
  "timestamp": 1699999999999
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 是 | 固定值 "auth_request" |
| device_id | String | 是 | 设备唯一标识（客户端生成并持久化） |
| device_name | String | 否 | 设备名称（用于日志显示） |
| timestamp | Long | 是 | 请求时间戳（毫秒） |

### 1.2 挑战码下发 (Challenge)

**方向**: 服务器 → 客户端

```json
{
  "type": "challenge",
  "challenge_code": "e4d9f3b2a1c876543210fedcba987654",
  "timestamp": 1699999999015
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 是 | 固定值 "challenge" |
| challenge_code | String | 是 | 随机挑战码（建议 32 字符十六进制） |
| timestamp | Long | 是 | 下发时间戳（毫秒） |

### 1.3 挑战响应 (ChallengeResponse)

**方向**: 客户端 → 服务器

```json
{
  "type": "challenge_response",
  "device_id": "android-a1b2c3d4",
  "signature": "b3a8f2c1d4e56789012345678901234567890abcdef..."
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 是 | 固定值 "challenge_response" |
| device_id | String | 是 | 设备唯一标识 |
| signature | String | 是 | HMAC-SHA256 签名（十六进制小写） |

**签名算法**:
```
输入: "{device_id}:{challenge_code}"
密钥: 预共享密钥
算法: HMAC-SHA256
输出: 十六进制小写字符串
```

### 1.4 认证结果 (AuthResult)

**方向**: 服务器 → 客户端

**成功响应**:
```json
{
  "type": "auth_result",
  "ok": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_at": 1700003599999,
  "reason": ""
}
```

**失败响应**:
```json
{
  "type": "auth_result",
  "ok": false,
  "token": "",
  "expires_at": 0,
  "reason": "Invalid signature"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 是 | 固定值 "auth_result" |
| ok | Boolean | 是 | 认证是否成功 |
| token | String | 是 | 会话令牌（成功时非空） |
| expires_at | Long | 是 | Token 过期时间戳（毫秒） |
| reason | String | 否 | 失败原因（成功时为空字符串） |

---

## 2. 控制接口

### 2.1 控制指令 (ControlPacket)

**方向**: 客户端 → 服务器
**推荐频率**: 50Hz（每 20ms）
**频率范围**: 30Hz ~ 100Hz

**周期选择依据**:
| 频率 | 周期 | 延迟感知 | 带宽消耗 | 适用场景 |
|------|------|----------|----------|----------|
| 30Hz | 33ms | 可感知 | 84 kbps | 低带宽环境 |
| **50Hz** | **20ms** | **良好** | **140 kbps** | **通用推荐** |
| 60Hz | 16ms | 优秀 | 168 kbps | 高响应需求 |
| 100Hz | 10ms | 极佳 | 280 kbps | 竞技/专业场景 |

**为什么选择 50Hz**:
- ✅ 人眼对延迟的感知阈值约 20ms
- ✅ 游戏手柄标准刷新率（Xbox/PS 为 50-60Hz）
- ✅ 机器人控制需要实时响应
- ✅ 带宽消耗适中（140 kbps）
- ✅ 与显示器刷新率（60Hz）匹配

```json
{
  "type": "control",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "timestamp": 1699999999999,
  "sequence": 1,
  "left_stick_x": 0.5,
  "left_stick_y": -0.3,
  "right_stick_x": 0.0,
  "right_stick_y": 0.0,
  "btn_a": true,
  "btn_b": false,
  "btn_x": false,
  "btn_y": false,
  "dpad_up": false,
  "dpad_down": false,
  "dpad_left": false,
  "dpad_right": false
}
```

| 字段 | 类型 | 范围 | 必填 | 说明 |
|------|------|------|------|------|
| type | String | "control" | 是 | 数据包类型 |
| token | String | - | 是 | 认证令牌 |
| timestamp | Long | 毫秒 | 是 | 发送时间戳 |
| sequence | Long | 单调递增 | 是 | 序列号 |
| left_stick_x | Float | [-1.0, 1.0] | 是 | 左摇杆水平（右为正） |
| left_stick_y | Float | [-1.0, 1.0] | 是 | 左摇杆垂直（下为正） |
| right_stick_x | Float | [-1.0, 1.0] | 是 | 右摇杆水平（右为正） |
| right_stick_y | Float | [-1.0, 1.0] | 是 | 右摇杆垂直（下为正） |
| btn_a | Boolean | true/false | 是 | A 按钮状态 |
| btn_b | Boolean | true/false | 是 | B 按钮状态 |
| btn_x | Boolean | true/false | 是 | X 按钮状态 |
| btn_y | Boolean | true/false | 是 | Y 按钮状态 |
| dpad_up | Boolean | true/false | 是 | 方向键上 |
| dpad_down | Boolean | true/false | 是 | 方向键下 |
| dpad_left | Boolean | true/false | 是 | 方向键左 |
| dpad_right | Boolean | true/false | 是 | 方向键右 |

---

## 3. 心跳接口

### 3.1 心跳请求 (Heartbeat)

**方向**: 客户端 → 服务器
**推荐频率**: 2Hz（每 500ms）
**频率范围**: 1Hz ~ 5Hz

**周期选择依据**:
| 频率 | 周期 | 断线检测时间 | 带宽消耗 | 适用场景 |
|------|------|--------------|----------|----------|
| 1Hz | 1000ms | 5-10秒 | 1.6 kbps | 非实时应用 |
| **2Hz** | **500ms** | **2.5-5秒** | **3.2 kbps** | **通用推荐** |
| 5Hz | 200ms | 1-2秒 | 8 kbps | 高可靠性需求 |

**为什么选择 2Hz**:
- ✅ UDP 可能丢包，2Hz 提供冗余（2次/秒）
- ✅ 断线检测时间 2.5-5秒，用户体验良好
- ✅ 带宽消耗极小（3.2 kbps）
- ✅ 工业控制场景通常要求 2-3 秒内检测故障
- ✅ 与心跳超时配置（5秒）配合，允许 2-3 次丢包

```json
{
  "type": "heartbeat",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "timestamp": 1699999999999
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 是 | 固定值 "heartbeat" |
| token | String | 是 | 认证令牌 |
| timestamp | Long | 是 | 发送时间戳（毫秒） |

### 3.2 心跳响应 (HeartbeatResponse)

**方向**: 服务器 → 客户端（可选）

```json
{
  "type": "heartbeat",
  "timestamp": 1699999999999
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 是 | 固定值 "heartbeat" |
| timestamp | Long | 是 | 服务器时间戳（毫秒） |

---

## 4. 服务器实现要求

### 4.1 认证流程

```
1. 接收 AuthRequest
2. 生成随机挑战码（32 字符十六进制）
3. 下发 Challenge
4. 接收 ChallengeResponse
5. 验证签名:
   expected = HMAC-SHA256(device_id + ":" + challenge_code, SECRET_KEY)
   valid = (expected == signature)
6. 返回 AuthResult
7. 生成并返回 Token（签名验证成功时）
```

### 4.2 Token 管理

- **格式**: 建议使用 JWT（JSON Web Token）
- **内容**: 包含 device_id、过期时间等声明
- **验证**: 每次接收控制指令时验证 Token 有效性

### 4.3 控制指令处理

```
1. 接收 ControlPacket
2. 验证 Token 有效性
3. 检查 Token 是否过期
4. 解析控制指令
5. 执行相应动作（驱动电机等）
6. 返回执行结果（可选）
```

### 4.4 心跳处理

```
1. 接收 Heartbeat
2. 更新设备最后活跃时间
3. 返回 HeartbeatResponse（可选）
4. 超时检测: 超过 5 秒未收到心跳，判定设备离线
```

---

## 5. 错误处理

### 5.1 认证失败

```json
{
  "type": "auth_result",
  "ok": false,
  "token": "",
  "expires_at": 0,
  "reason": "Invalid signature"
}
```

常见失败原因:
- `Invalid signature`: 签名验证失败
- `Device not allowed`: 设备未注册
- `Rate limit exceeded`: 请求过于频繁

### 5.2 Token 无效

服务器应静默丢弃无效 Token 的控制指令，或返回错误响应:

```json
{
  "type": "error",
  "code": "INVALID_TOKEN",
  "message": "Token is invalid or expired"
}
```

### 5.3 超时处理

- **认证超时**: 客户端等待 5 秒无响应，判定超时
- **心跳超时**: 客户端 5 秒未收到心跳响应，判定连接丢失
- **指令超时**: 服务器可选实现，丢弃过期指令（timestamp 超过 300ms）

---

## 6. 安全建议

### 6.1 密钥管理

- **开发环境**: 使用配置文件或环境变量
- **生产环境**: 使用硬件安全模块（HSM）或密钥管理服务

### 6.2 Token 安全

- **有效期**: 建议 1 小时
- **刷新**: 过期前 1 分钟自动刷新
- **存储**: 客户端使用 SharedPreferences（加密）

### 6.3 防重放攻击

- 挑战码应具有时效性（建议 30 秒过期）
- 每次认证使用新的挑战码
- Token 包含设备绑定信息

---

## 7. 配置参数

### 7.1 客户端配置 (connection_config.json)

```json
{
  "control": {
    "host": "10.43.21.32",
    "port": 9000,
    "send_frequency_hz": 50
  },
  "auth": {
    "timeout_ms": 5000,
    "retry_count": 3,
    "heartbeat_interval_ms": 500,
    "heartbeat_timeout_ms": 5000,
    "reconnect_delay_ms": 2000,
    "reconnect_max_count": 5
  }
}
```

### 7.2 服务器配置建议

```yaml
server:
  host: 0.0.0.0
  port: 9000
  max_clients: 10

auth:
  secret_key: "your-secret-key"
  challenge_length: 32
  challenge_expiry_sec: 30
  token_expiry_sec: 3600

timeout:
  heartbeat_sec: 5
  control_packet_ms: 300
```

---

## 8. 周期性报文配置总结

### 8.1 推荐周期配置

| 报文类型 | 推荐频率 | 周期 | 配置项 | 说明 |
|----------|----------|------|--------|------|
| **ControlPacket** | 50Hz | 20ms | `send_frequency_hz: 50` | 控制指令 |
| **Heartbeat** | 2Hz | 500ms | `heartbeat_interval_ms: 500` | 心跳保活 |

### 8.2 超时配置

| 超时类型 | 推荐值 | 配置项 | 说明 |
|----------|--------|--------|------|
| **认证超时** | 5000ms | `timeout_ms: 5000` | 单步认证等待 |
| **心跳超时** | 5000ms | `heartbeat_timeout_ms: 5000` | 断线判定 |
| **指令过期** | 300ms | `stale_packet_ms: 300` | 服务器端丢弃旧指令 |

### 8.3 重连配置

| 参数 | 推荐值 | 配置项 | 说明 |
|------|--------|--------|------|
| **重连延迟** | 2000ms | `reconnect_delay_ms: 2000` | 重连等待时间 |
| **最大重连次数** | 5次 | `reconnect_max_count: 5` | 超过后判定失败 |

### 8.4 带宽计算

| 报文类型 | 频率 | 单包大小 | 带宽消耗 |
|----------|------|----------|----------|
| ControlPacket | 50Hz | ~350 bytes | **140 kbps** |
| Heartbeat | 2Hz | ~200 bytes | **3.2 kbps** |
| **总计** | - | - | **143.2 kbps** |

### 8.5 断线检测时间

```
心跳超时 = heartbeat_timeout_ms = 5000ms
心跳间隔 = heartbeat_interval_ms = 500ms

允许丢包次数 = 超时间隔 / 心跳间隔 = 5000 / 500 = 10 次

断线检测时间范围:
- 最快: 5000ms (连续 10 次丢包)
- 最慢: 5500ms (最后一次心跳刚好到达)
```

---

## 9. 测试用例

### 8.1 认证成功

```
客户端 → AuthRequest {device_id: "android-abc123"}
服务器 → Challenge {challenge_code: "random123"}
客户端 → ChallengeResponse {device_id: "android-abc123", signature: "hmac..."}
服务器 → AuthResult {ok: true, token: "jwt..."}
```

### 8.2 认证失败（签名错误）

```
客户端 → AuthRequest {device_id: "android-abc123"}
服务器 → Challenge {challenge_code: "random123"}
客户端 → ChallengeResponse {device_id: "android-abc123", signature: "wrong..."}
服务器 → AuthResult {ok: false, reason: "Invalid signature"}
```

### 8.3 Token 过期

```
客户端 → ControlPacket {token: "expired-jwt...", ...}
服务器 → 丢弃或返回 {type: "error", code: "INVALID_TOKEN"}
```

---

## 9. 遥测数据接口

### 9.1 电机状态 (motors)

**方向**: 服务器 → 客户端
**频率**: 10Hz（每 100ms）

```json
{
  "type": "motors",
  "timestamp": 1699999999999,
  "left_speed": 100.0,
  "left_current": 1.5,
  "left_temp": 35.0,
  "left_encoder": 12345,
  "right_speed": 100.0,
  "right_current": 1.5,
  "right_temp": 35.0,
  "right_encoder": 12345,
  "motor_error": 0
}
```

**电机字段说明**:

| 字段 | 类型 | 单位 | 说明 |
|------|------|------|------|
| type | String | - | 固定值 "motors" |
| timestamp | Long | 毫秒 | 时间戳 |
| left_speed | Float | RPM | 左电机转速 |
| left_current | Float | A | 左电机电流 |
| left_temp | Float | °C | 左电机温度 |
| left_encoder | Long | - | 左电机编码器值 |
| right_speed | Float | RPM | 右电机转速 |
| right_current | Float | A | 右电机电流 |
| right_temp | Float | °C | 右电机温度 |
| right_encoder | Long | - | 右电机编码器值 |
| motor_error | Int | - | 电机错误码（0=无错误） |

**电机错误码定义**:

| 错误码 | 十六进制 | 含义 | 处理建议 |
|--------|----------|------|----------|
| 0 | 0x0000 | 无错误 | - |
| 1 | 0x0001 | 过温保护 | 降低负载，等待冷却 |
| 2 | 0x0002 | 过流保护 | 检查负载，重启电机 |
| 3 | 0x0003 | 过压保护 | 检查电源 |
| 4 | 0x0004 | 欠压保护 | 检查电池 |
| 5 | 0x0005 | 堵转保护 | 清除障碍物 |
| 6 | 0x0006 | 编码器故障 | 检查编码器连接 |
| 7 | 0x0007 | 通信超时 | 检查通信线缆 |
| 8 | 0x0008 | 驱动器故障 | 更换驱动器 |

### 9.2 IMU 数据 (imu)

**方向**: 服务器 → 客户端
**频率**: 50Hz（每 20ms）

```json
{
  "type": "imu",
  "timestamp": 1699999999999,
  "accel_x": 0.1,
  "accel_y": 0.1,
  "accel_z": 9.8,
  "gyro_x": 0.5,
  "gyro_y": 0.5,
  "gyro_z": 0.2,
  "roll": 2.0,
  "pitch": 2.0,
  "yaw": 180.0,
  "mag_x": 25.0,
  "mag_y": -10.0,
  "mag_z": 40.0,
  "imu_error": 0
}
```

**IMU 字段说明**:

| 字段 | 类型 | 单位 | 说明 |
|------|------|------|------|
| type | String | - | 固定值 "imu" |
| timestamp | Long | 毫秒 | 时间戳 |
| accel_x | Float | m/s² | X 轴加速度 |
| accel_y | Float | m/s² | Y 轴加速度 |
| accel_z | Float | m/s² | Z 轴加速度 |
| gyro_x | Float | °/s | X 轴角速度 |
| gyro_y | Float | °/s | Y 轴角速度 |
| gyro_z | Float | °/s | Z 轴角速度 |
| roll | Float | 度 | 横滚角 |
| pitch | Float | 度 | 俯仰角 |
| yaw | Float | 度 | 偏航角 |
| mag_x | Float | uT | X 轴磁场 |
| mag_y | Float | uT | Y 轴磁场 |
| mag_z | Float | uT | Z 轴磁场 |
| imu_error | Int | - | IMU 错误码（0=无错误） |

---

## 10. BLE 蓝牙通信协议

### 10.1 BLE 服务定义

```
Service UUID:        0000fff0-0000-1000-8000-00805f9b34fb
├── TX Characteristic: 0000fff1-0000-1000-8000-00805f9b34fb  (客户端写入 → 机器人)
└── RX Characteristic: 0000fff2-0000-1000-8000-00805f9b34fb  (机器人通知 → 客户端)
```

### 10.2 BLE 连接流程

```
1. 扫描 BLE 设备（按 Service UUID 过滤）
2. 选择目标设备并连接
3. 发现服务和特征
4. 订阅 RX Characteristic 通知
5. 协商 MTU（建议 512 字节）
6. 执行三步认证（与 UDP 相同）
7. 进入正常通信模式
```

### 10.3 BLE 数据格式

BLE 传输使用与 UDP 相同的 JSON 格式，但需要分包处理。

**单包格式**（数据 ≤ MTU-3 字节）：
```
直接发送 JSON 字符串的 UTF-8 字节
```

**分包格式**（数据 > MTU-3 字节）：
```
| 包头 (2字节) | 包序号 (1字节) | 总包数 (1字节) | 数据 (变长) |
| 0xAA 0x55   | 0x00~0xFF     | 0x00~0xFF     | JSON 字节   |
```

### 10.4 BLE 配置参数

```json
{
  "ble": {
    "service_uuid": "0000fff0-0000-1000-8000-00805f9b34fb",
    "tx_characteristic_uuid": "0000fff1-0000-1000-8000-00805f9b34fb",
    "rx_characteristic_uuid": "0000fff2-0000-1000-8000-00805f9b34fb",
    "scan_timeout_ms": 10000,
    "mtu_size": 512
  }
}
```

### 10.5 BLE vs UDP 对比

| 特性 | UDP | BLE |
|------|-----|-----|
| 传输距离 | 局域网（WiFi） | 10-50 米 |
| 连接方式 | 需要知道 IP 地址 | 自动扫描发现 |
| 传输速率 | 高（>1Mbps） | 中（~100Kbps） |
| 功耗 | 高 | 低 |
| 延迟 | 低（<10ms） | 中（~30ms） |
| 适用场景 | 固定网络环境 | 快速配对、移动场景 |

---

## 11. 附录

### 9.1 HMAC-SHA256 签名示例

**Python**:
```python
import hmac
import hashlib

def compute_signature(device_id: str, challenge_code: str, secret: str) -> str:
    data = f"{device_id}:{challenge_code}"
    return hmac.new(secret.encode(), data.encode(), hashlib.sha256).hexdigest()
```

**JavaScript**:
```javascript
const crypto = require('crypto');

function computeSignature(deviceId, challengeCode, secret) {
    const data = `${deviceId}:${challengeCode}`;
    return crypto.createHmac('sha256', secret).update(data).digest('hex');
}
```

### 9.2 JWT Token 示例

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "device_id": "android-abc123",
    "iat": 1699999999,
    "exp": 1700003599
  }
}
```

---

**文档版本**: v1.1
**更新日期**: 2026-07-27
**更新内容**: 新增 BLE 蓝牙通信协议
