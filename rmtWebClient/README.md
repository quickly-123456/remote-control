# BACKEND API DOCUMENT（后端 API 文档）

---

## 1. 用户注册（Registration）

**接口地址：**
```
POST http://185.128.227.222:5558/api/register
```

**请求参数：**
```json
{
  "phone": "852113236789",
  "password": "mypa11word",
  "super_id": "admin_sat"
}
```

**返回示例：**
```json
{
  "message": "未知错误",
  "result": 1
}
```

**Result 说明：**

| result | 含义         |
|--------|--------------|
| 0      | 注册成功     |
| 1      | 手机号已存在 |
| 2      | 未知异常     |

**curl 示例：**
```bash
curl -X POST \
  http://185.128.227.222:5558/api/register \
  -H "Content-Type: application/json" \
  -d '{ "phone": "852123456789", "password": "mypassword", "super_id": "admin_sat" }'
```

---

## 2. 用户登录（Login）

**接口地址：**
```
POST http://185.128.227.222:5558/api/login
```

**请求参数：**
```json
{
  "phone": "852123456789",
  "password": "mypassword"
}
```

**返回示例：**
```json
{
  "message": "登录成功！",
  "result": 0,
  "user": "{\"id\":7,\"password\":\"password\",\"phone\":\"852123456789\",\"status\":0}"
}
```

**Result 说明：**

| result | 含义                                   |
|--------|----------------------------------------|
| 0      | 登录成功                               |
| 1      | 未注册手机号                           |
| 2      | 其他原因（status=1:用户被封，2:已删除）|

**curl 示例：**
```bash
curl -X POST \
  http://185.128.227.222:5558/api/login \
  -H "Content-Type: application/json" \
  -d '{ "phone": "852123456789", "password": "mypassword" }'
```

---

## 3. 重置密码（Reset Password）

**接口地址：**
```
POST http://185.128.227.222:5558/api/resetpassword
```

**请求参数：**
```json
{
  "phone": "852123456789",
  "new_password": "123456789"
}
```

**返回示例：**
```json
{
  "message": "密码修改成功!",
  "result": 0
}
```

**Result 说明：**

| result | 含义         |
|--------|--------------|
| 0      | 成功         |
| 1      | 失败（异常） |

---

## 4. 修改密码（Update Password）

**接口地址：**
```
POST http://185.128.227.222:5558/api/updatepassword
```

**请求参数：**
```json
{
  "phone": "852123456789",
  "old_password": "123456789",
  "new_password": "pass1wds"
}
```

**返回示例：**
```json
{
  "message": "操作失败!",
  "result": 1
}
```

**Result 说明：**

| result | 含义         |
|--------|--------------|
| 0      | 成功         |
| 1      | 当前密码错误 |
| 2      | 其他异常     |

---

## 5. 获取权限（Get Permissions）

**接口地址：**
```
POST http://185.128.227.222:5558/api/getpermissions
```

**请求参数：**
```json
{
  "phone": "852123456789"
}
```

**返回示例：**
```json
{
  "message": "",
  "permissions": "{\"camera\":0,\"file_access\":1,\"microphone\":1,\"remote_input\":0,\"screen\":1}",
  "result": 0
}
```

**Result 说明：**

| result | 含义         |
|--------|--------------|
| 0      | 成功         |
| 1      | 失败（异常） |

---

## 6. 设置权限（Set Permissions）

**接口地址：**
```
POST http://185.128.227.222:5558/api/setpermissions
```

**请求参数：**
```json
{
  "phone": "852123456789",
  "permissions": "{\"camera\":0,\"file_access\":1,\"microphone\":1,\"remote_input\":0,\"screen\":1}"
}
```

**返回示例：**
```json
{
  "message": "",
  "permissions": "{\"camera\":0,\"file_access\":1,\"microphone\":1,\"remote_input\":0,\"screen\":1}",
  "result": 0
}
```

**Result 说明：**

| result | 含义         |
|--------|--------------|
| 0      | 成功         |
| 1      | 失败（异常） |

---

> **说明：**  
> - 所有接口均为 `application/json` 格式。  
> - 权限字段说明：`camera`（摄像头），`file_access`（文件访问），`microphone`（麦克风），`remote_input`（远程输入），`screen`（屏幕）。

---

# Binary Message 协议说明

- **int 类型为小端（little endian）4字节**
- **string 类型为 4 字节长度 + 字符串内容（utf-8）**
- **byte array 类型为 4 字节长度 + 二进制内容**

## 信令命名约定

- `CS_` 前缀：Client → Server（客户端发给服务端）
- `SC_` 前缀：Server → Client（服务端发给客户端）

每个 `CS_xxxx` 信令，服务端会返回对应的 `SC_xxxx` 信令。

---

## 1. CS_USER (255)

**客户端 → 服务端：用户登录/注册请求**

| 字段类型 | 字节数      | 描述         | 示例值         |
|----------|-------------|--------------|---------------|
| int      | 4           | 信令         | 255 (CS_USER) |
| string   | 4+N         | 手机号       | "12345678"    |
| string   | 4+N         | Super ID     | "admin_test"  |

**内存示例（十进制）：**

```
255 0 0 0         // 信令
8 0 0 0           // 手机号长度
49 50 51 52 53 54 55 56 // "12345678"
10 0 0 0          // Super ID 长度
97 100 109 ...    // "admin_test"
```

---

## 2. SC_USER (256)

**服务端 → 客户端：用户登录/注册响应**

| 字段类型 | 字节数 | 描述   | 示例值    |
|----------|--------|--------|-----------|
| int      | 4      | 信令   | 256       |

---

## 3. CS_VUE (257)

**客户端 → 服务端：请求成员列表**

| 字段类型 | 字节数      | 描述     | 示例值         |
|----------|-------------|----------|---------------|
| int      | 4           | 信令     | 257 (CS_VUE)  |
| string   | 4+N         | Super ID | "admin_test"  |

---

## 4. SC_VUE (258)

**服务端 → 客户端：返回成员列表**

| 字段类型 | 字节数      | 描述           | 示例值         |
|----------|-------------|----------------|---------------|
| int      | 4           | 信令           | 258 (SC_VUE)  |
| int      | 4           | 成员数量       | 2             |
| string   | 4+N         | 手机号1        | "123456789"   |
| int      | 4           | 是否在线1      | 1（在线）      |
| string   | 4+N         | 手机号2        | "123456743"   |
| int      | 4           | 是否在线2      | 0（离线）      |
| ...      | ...         | 更多成员       | ...           |

---

## 5. CS_SCREEN (259)

**客户端 → 服务端：发送屏幕截图**

| 字段类型 | 字节数      | 描述         | 示例值         |
|----------|-------------|--------------|---------------|
| int      | 4           | 信令         | 259 (CS_SCREEN)|
| int      | 4           | 时间戳       | 251230201     |
| byte[]   | 4+N         | Webp图片数据 | 二进制        |

---

## 6. SC_SCREEN (260)

**服务端 → 客户端：返回屏幕截图**

### Android Java Client

| 字段类型 | 字节数      | 描述         | 示例值         |
|----------|-------------|--------------|---------------|
| int      | 4           | 信令         | 260 (SC_SCREEN)|
| int      | 4           | 延迟(ms)     | 100           |
| byte[]   | 4+N         | Webp图片数据 | 二进制        |

### Web Vue 和 Android Vue

| 字段类型 | 字节数      | 描述         | 示例值         |
|----------|-------------|--------------|---------------|
| int      | 4           | 信令         | 260 (SC_SCREEN)|
| string   | 4+N         | 发送者手机号 | "123456789"   |
| byte[]   | 4+N         | Webp图片数据 | 二进制        |

---

## 其他说明

- **所有 int 均为小端序（little endian）**
- **所有 string 均为 4 字节长度 + utf-8 字符串内容**
- **所有 byte array 均为 4 字节长度 + 二进制内容**

---

## Node & Yarn 版本

```bash
node -v
v22.17.1

yarn -v
1.22.19
```

