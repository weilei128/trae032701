# 个人收支记账系统 API 接口文档

## 基础信息

- 基础URL: `http://localhost:8080`
- 响应格式: JSON
- 统一响应格式:
```json
{
    "code": 200,
    "msg": "成功",
    "data": {}
}
```

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 用户未登录 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

---

## 用户模块

### 1. 用户注册

**接口地址**: `POST /api/user/register`

**请求参数**:
```json
{
    "username": "13800138000",
    "password": "123456",
    "nickname": "小明"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名，手机号或邮箱格式 |
| password | String | 是 | 密码，6-20位 |
| nickname | String | 否 | 昵称，最长20字符 |

**响应示例**:
```json
{
    "code": 200,
    "msg": "注册成功",
    "data": {
        "id": "1712345678901",
        "username": "13800138000",
        "nickname": "小明"
    }
}
```

### 2. 用户登录

**接口地址**: `POST /api/user/login`

**请求参数**:
```json
{
    "username": "13800138000",
    "password": "123456"
}
```

**响应示例**:
```json
{
    "code": 200,
    "msg": "登录成功",
    "data": {
        "token": "abc123def456..."
    }
}
```

### 3. 获取用户信息

**接口地址**: `GET /api/user/info`

**请求头**:
```
Authorization: {token}
```

**响应示例**:
```json
{
    "code": 200,
    "msg": "成功",
    "data": {
        "id": "1712345678901",
        "username": "13800138000",
        "nickname": "小明",
        "createTime": "2024-01-01 10:00:00"
    }
}
```

### 4. 修改密码

**接口地址**: `POST /api/user/changePassword`

**请求头**:
```
Authorization: {token}
```

**请求参数**:
```json
{
    "oldPassword": "123456",
    "newPassword": "654321"
}
```

**响应示例**:
```json
{
    "code": 200,
    "msg": "密码修改成功",
    "data": null
}
```

### 5. 登出

**接口地址**: `POST /api/user/logout`

**请求头**:
```
Authorization: {token}
```

**响应示例**:
```json
{
    "code": 200,
    "msg": "登出成功",
    "data": null
}
```

---

## 记账模块

### 1. 添加收支记录

**接口地址**: `POST /api/record/add`

**请求头**:
```
Authorization: {token}
```

**请求参数**:
```json
{
    "amount": 100.50,
    "type": "支出",
    "category": "餐饮",
    "remark": "午餐"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| amount | Double | 是 | 金额，必须为正数 |
| type | String | 是 | 类型：收入/支出 |
| category | String | 是 | 分类，需为预设值 |
| remark | String | 否 | 备注，最长200字符 |

**预设分类**:
- 收入: 薪资、奖金、投资收益、兼职、红包、其他收入
- 支出: 餐饮、购物、交通、娱乐、医疗、教育、住房、通讯、水电、其他支出

**响应示例**:
```json
{
    "code": 200,
    "msg": "添加成功",
    "data": {
        "id": "1712345678902",
        "amount": 100.50,
        "type": "支出",
        "category": "餐饮",
        "remark": "午餐",
        "createTime": "2024-01-01 12:00:00"
    }
}
```

### 2. 查询收支记录

**接口地址**: `GET /api/record/list`

**请求头**:
```
Authorization: {token}
```

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| startDate | String | 否 | 开始日期，格式：yyyy-MM-dd |
| endDate | String | 否 | 结束日期，格式：yyyy-MM-dd |
| type | String | 否 | 类型：收入/支出 |
| category | String | 否 | 分类 |
| pageNum | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页条数，默认10 |

**响应示例**:
```json
{
    "code": 200,
    "msg": "成功",
    "data": {
        "list": [
            {
                "id": "1712345678902",
                "amount": 100.50,
                "type": "支出",
                "category": "餐饮",
                "remark": "午餐",
                "createTime": "2024-01-01 12:00:00"
            }
        ],
        "total": 100,
        "pageNum": 1,
        "pageSize": 10,
        "pages": 10
    }
}
```

### 3. 获取单条记录

**接口地址**: `GET /api/record/{id}`

**请求头**:
```
Authorization: {token}
```

**响应示例**:
```json
{
    "code": 200,
    "msg": "成功",
    "data": {
        "id": "1712345678902",
        "amount": 100.50,
        "type": "支出",
        "category": "餐饮",
        "remark": "午餐",
        "createTime": "2024-01-01 12:00:00"
    }
}
```

### 4. 更新收支记录

**接口地址**: `PUT /api/record/{id}`

**请求头**:
```
Authorization: {token}
```

**请求参数**:
```json
{
    "amount": 150.00,
    "type": "支出",
    "category": "餐饮",
    "remark": "午餐加饮料"
}
```

**响应示例**:
```json
{
    "code": 200,
    "msg": "更新成功",
    "data": {
        "id": "1712345678902",
        "amount": 150.00,
        "type": "支出",
        "category": "餐饮",
        "remark": "午餐加饮料",
        "createTime": "2024-01-01 12:00:00"
    }
}
```

### 5. 删除收支记录

**接口地址**: `DELETE /api/record/{id}`

**请求头**:
```
Authorization: {token}
```

**响应示例**:
```json
{
    "code": 200,
    "msg": "删除成功",
    "data": null
}
```

---

## 统计模块

### 1. 概览统计

**接口地址**: `GET /api/statistics/overview`

**请求头**:
```
Authorization: {token}
```

**响应示例**:
```json
{
    "code": 200,
    "msg": "成功",
    "data": {
        "totalIncome": 10000.00,
        "totalExpense": 5000.00,
        "totalBalance": 5000.00,
        "monthIncome": 3000.00,
        "monthExpense": 2000.00,
        "monthBalance": 1000.00,
        "recordCount": 50
    }
}
```

### 2. 按周统计

**接口地址**: `GET /api/statistics/weekly`

**请求头**:
```
Authorization: {token}
```

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| year | Integer | 是 | 年份 |
| week | Integer | 是 | 周数（1-53） |

**响应示例**:
```json
{
    "code": 200,
    "msg": "成功",
    "data": {
        "year": 2024,
        "week": 1,
        "totalIncome": 1000.00,
        "totalExpense": 500.00,
        "balance": 500.00
    }
}
```

### 3. 按月统计

**接口地址**: `GET /api/statistics/monthly`

**请求头**:
```
Authorization: {token}
```

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| year | Integer | 是 | 年份 |
| month | Integer | 是 | 月份（1-12） |

**响应示例**:
```json
{
    "code": 200,
    "msg": "成功",
    "data": {
        "year": 2024,
        "month": 1,
        "totalIncome": 5000.00,
        "totalExpense": 3000.00,
        "balance": 2000.00
    }
}
```

### 4. 按分类统计

**接口地址**: `GET /api/statistics/category`

**请求头**:
```
Authorization: {token}
```

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 是 | 类型：收入/支出 |
| year | Integer | 否 | 年份 |
| month | Integer | 否 | 月份 |

**响应示例**:
```json
{
    "code": 200,
    "msg": "成功",
    "data": {
        "type": "支出",
        "totalAmount": 3000.00,
        "categories": [
            {
                "category": "餐饮",
                "amount": 1500.00,
                "percentage": 50.00
            },
            {
                "category": "购物",
                "amount": 900.00,
                "percentage": 30.00
            },
            {
                "category": "交通",
                "amount": 600.00,
                "percentage": 20.00
            }
        ]
    }
}
```
