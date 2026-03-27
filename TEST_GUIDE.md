# 个人收支记账系统 - 测试操作步骤

## 一、环境准备

### 1. 系统要求
- JDK 11 或更高版本
- Maven 3.6+

### 2. 启动项目

```bash
# 进入项目目录
cd personal-account-book

# 编译项目
mvn clean package -DskipTests

# 运行项目
java -jar target/personal-account-book-1.0.0.jar
```

或者使用 Maven 直接运行：
```bash
mvn spring-boot:run
```

### 3. 访问系统
启动成功后，打开浏览器访问：`http://localhost:8080`

---

## 二、功能测试步骤

### 测试1：用户注册

1. 打开浏览器访问 `http://localhost:8080`
2. 点击"没有账号？立即注册"链接
3. 填写注册信息：
   - 用户名：`13800138000`（手机号格式）
   - 密码：`123456`
   - 昵称：`测试用户`
4. 点击"注册"按钮
5. 预期结果：提示"注册成功，请登录"，自动跳转到登录页面

**测试邮箱注册**：
1. 用户名：`test@example.com`
2. 密码：`123456`
3. 预期结果：注册成功

**测试异常情况**：
- 重复注册同一用户名：应提示"用户名已存在"
- 密码少于6位：应提示"密码长度必须在6-20位之间"
- 用户名格式错误：应提示"用户名必须是手机号或邮箱格式"

---

### 测试2：用户登录

1. 在登录页面输入：
   - 用户名：`13800138000`
   - 密码：`123456`
2. 点击"登录"按钮
3. 预期结果：提示"登录成功"，跳转到首页，显示用户昵称

**测试异常情况**：
- 用户名不存在：应提示"用户不存在"
- 密码错误：应提示"密码错误"

---

### 测试3：添加收支记录

1. 登录成功后，点击右下角"+"按钮
2. 选择类型：默认为"支出"
3. 输入金额：`35.50`
4. 选择分类：点击"餐饮"标签
5. 输入备注：`午餐`
6. 点击"保存"按钮
7. 预期结果：提示"添加成功"，记录出现在列表中

**测试收入记录**：
1. 点击"+"按钮
2. 切换类型为"收入"
3. 输入金额：`5000`
4. 选择分类：`薪资`
5. 输入备注：`月工资`
6. 点击"保存"
7. 预期结果：添加成功，显示为绿色收入记录

**测试异常情况**：
- 金额为空：应提示"金额不能为空"
- 金额为负数：应提示"金额必须为正数"
- 未选择分类：应提示"请选择分类"

---

### 测试4：查询收支记录

1. 点击底部"记账"标签
2. 查看记录列表，应显示所有记录
3. 使用筛选功能：
   - 选择开始日期
   - 选择结束日期
   - 选择类型（收入/支出）
   - 选择分类
4. 点击"筛选"按钮
5. 预期结果：列表显示符合条件的记录

**测试分页**：
1. 添加超过10条记录
2. 查看列表底部是否出现分页按钮
3. 点击"下一页"查看更多记录

---

### 测试5：修改收支记录

1. 在记录列表中，找到要修改的记录
2. 点击"编辑"按钮
3. 修改金额：`40.00`
4. 修改备注：`午餐加饮料`
5. 点击"保存"
6. 预期结果：提示"更新成功"，记录信息已更新

---

### 测试6：删除收支记录

1. 在记录列表中，找到要删除的记录
2. 点击"删除"按钮
3. 在确认对话框中点击"确定"
4. 预期结果：提示"删除成功"，记录从列表中消失

---

### 测试7：统计功能

1. 点击底部"统计"标签
2. 查看"本月收支"卡片：
   - 应显示本月收入总额
   - 应显示本月支出总额
   - 应显示本月结余
3. 查看"支出分类占比"：
   - 应显示各支出分类的金额和占比
   - 应有进度条显示占比
4. 查看"收入分类占比"：
   - 应显示各收入分类的金额和占比

---

### 测试8：首页概览

1. 点击底部"首页"标签
2. 查看概览卡片：
   - 本月收入
   - 本月支出
   - 本月结余
   - 记录总数
3. 查看"最近记录"列表，应显示最近5条记录

---

### 测试9：修改密码

1. 点击底部"我的"标签
2. 在"修改密码"表单中：
   - 输入旧密码：`123456`
   - 输入新密码：`654321`
3. 点击"修改密码"按钮
4. 预期结果：提示"密码修改成功"
5. 退出登录，使用新密码重新登录验证

**测试异常情况**：
- 旧密码错误：应提示"旧密码错误"
- 新密码少于6位：应提示"新密码长度必须在6-20位之间"

---

### 测试10：退出登录

1. 点击顶部右侧"退出"链接
2. 或在"我的"页面点击"退出登录"按钮
3. 预期结果：跳转到登录页面，token被清除

---

## 三、API接口测试（使用curl或Postman）

### 1. 注册
```bash
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"13900139000","password":"123456","nickname":"API测试用户"}'
```

### 2. 登录
```bash
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"13900139000","password":"123456"}'
```
返回的token保存下来，后续请求需要使用。

### 3. 添加记录
```bash
curl -X POST http://localhost:8080/api/record/add \
  -H "Content-Type: application/json" \
  -H "Authorization: {token}" \
  -d '{"amount":100,"type":"支出","category":"餐饮","remark":"测试记录"}'
```

### 4. 查询记录
```bash
curl -X GET "http://localhost:8080/api/record/list?pageNum=1&pageSize=10" \
  -H "Authorization: {token}"
```

### 5. 统计概览
```bash
curl -X GET http://localhost:8080/api/statistics/overview \
  -H "Authorization: {token}"
```

### 6. 按月统计
```bash
curl -X GET "http://localhost:8080/api/statistics/monthly?year=2024&month=1" \
  -H "Authorization: {token}"
```

### 7. 分类统计
```bash
curl -X GET "http://localhost:8080/api/statistics/category?type=支出" \
  -H "Authorization: {token}"
```

---

## 四、数据存储验证

系统使用CSV文件存储数据，数据文件位于项目根目录的 `data` 文件夹中：

- `users.csv` - 用户数据
- `records.csv` - 记账记录数据

可以直接打开这些文件查看存储的数据格式。

---

## 五、常见问题排查

### 1. 端口被占用
如果8080端口被占用，可以修改 `application.yml` 中的端口号：
```yaml
server:
  port: 8081
```

### 2. 数据文件权限问题
确保程序对 `data` 目录有读写权限。

### 3. 登录状态丢失
系统使用内存存储token，重启服务后需要重新登录。

---

## 六、项目目录结构

```
personal-account-book/
├── pom.xml                          # Maven配置文件
├── API_DOC.md                       # API接口文档
├── TEST_GUIDE.md                    # 测试操作步骤
├── src/
│   └── main/
│       ├── java/com/account/
│       │   ├── AccountBookApplication.java    # 启动类
│       │   ├── common/              # 公共模块
│       │   │   ├── Result.java      # 统一响应格式
│       │   │   ├── BusinessException.java
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   ├── PageResult.java
│       │   │   └── ResultCode.java
│       │   ├── config/              # 配置类
│       │   │   ├── CsvProperties.java
│       │   │   └── WebConfig.java
│       │   ├── constant/            # 常量
│       │   │   └── RecordConstant.java
│       │   ├── controller/          # 控制器
│       │   │   ├── UserController.java
│       │   │   ├── RecordController.java
│       │   │   └── StatisticsController.java
│       │   ├── dto/                 # 数据传输对象
│       │   │   ├── RegisterRequest.java
│       │   │   ├── LoginRequest.java
│       │   │   ├── ChangePasswordRequest.java
│       │   │   ├── AddRecordRequest.java
│       │   │   ├── UpdateRecordRequest.java
│       │   │   └── QueryRecordRequest.java
│       │   ├── entity/              # 实体类
│       │   │   ├── User.java
│       │   │   └── Record.java
│       │   ├── service/             # 服务类
│       │   │   ├── UserService.java
│       │   │   ├── RecordService.java
│       │   │   └── StatisticsService.java
│       │   └── util/                # 工具类
│       │       └── CsvStorageUtil.java
│       └── resources/
│           ├── application.yml      # 配置文件
│           └── static/
│               └── index.html       # H5前端页面
└── data/                            # 数据目录（运行时自动创建）
    ├── users.csv
    └── records.csv
```
