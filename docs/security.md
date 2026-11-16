# CampusTrade 认证与授权（Week 09）

## 认证流程

客户端只使用宿主机 `8080` 网关入口。`POST /api/auth/login` 路由到 user-service，用户名与密码在用户服务校验。`auth_accounts` 表保存用户名、用户 ID、角色和密码哈希，不保存原始密码；密码使用 PBKDF2-HMAC-SHA256、随机 16 字节盐、600000 次迭代、256 位结果。登录失败统一返回 401，未知用户也执行一次密码哈希比较。登录成功返回 RS256 JWT，包含 `sub`、`username`、`roles`、`iss`、`iat`、`exp`，有效期 1800 秒。

RSA 2048 位密钥由 `python scripts/auth-env.py` 在本地生成，只保存于忽略的 `.env`。私钥仅注入 user-service；网关及下游只取得公钥。不要提交 `.env`、真实密码、私钥或登录 Token。可提交的请求示例为 `Authorization: Bearer <JWT_FROM_LOGIN>`。脚本不打印 Token，实际登录响应里的 Token 应在截图前遮盖。

网关和三个下游服务都使用 Spring Security Resource Server 验证签名、RS256 算法、签发者 `campustrade-user-service` 及有效期。网关从已验证的 `roles` claim 映射 `ROLE_USER` / `ROLE_ADMIN`。HTTP API 使用无状态 Bearer 认证，不用 Cookie 会话；因此禁用表单登录、Basic 和 CSRF。

## 路由与权限

Compose 部署的三条路由直接配置在根目录 `compose.yaml` 的 gateway-service / `SPRING_APPLICATION_JSON` 中；独立启动默认路由位于网关 application.yml，两处使用相同的服务名目标和路径。

| 网关路径 | 服务名目标 | 权限 |
| --- | --- | --- |
| `GET /actuator/health` | 网关自身 | 无需登录 |
| `POST /api/auth/login` | `lb://user-service` | 无需登录 |
| `/api/auth/me` | `lb://user-service` | USER / ADMIN |
| `/api/users/**` | `lb://user-service` | 登录；创建用户仅 ADMIN |
| `/api/admin/users` | `lb://user-service` | 仅 ADMIN |
| `/api/items/**` | `lb://product-service`（两个实例） | 登录；发布者必须是本人或 ADMIN |
| `/api/orders/**` | `lb://order-service` | 本人订单或 ADMIN |
| `/api/items/{id}/reserve`、`release`、`sell` | 网关禁止外部调用 | 由订单服务在内部调用 |

无登录凭据或无效 JWT 返回 401；有效 USER Token 访问 ADMIN 接口返回 403；ADMIN 可以访问管理接口，响应 DTO 不包含密码哈希。普通用户不能通过请求中的 `buyerId` / `sellerId` 操作别人的身份。管理员保留管理权限。

## 身份请求头与防绕过

`IdentityHeadersFilter` 删除客户端的 `X-User-Id`、`X-User-Roles`、`X-Username`、`X-Role`、`X-Roles`、`X-Authenticated-User`，使用验证后的 JWT 重写 `X-User-Id` 和 `X-User-Roles`。匿名登录请求也会清理身份头。`/api/auth/me` 同时返回 JWT 身份及收到的转发头，供脚本断言覆盖行为。

下游授权始终使用自行验签后的 JWT，不以请求头作为认证依据。用户服务在内部仍对 ADMIN 路径进行角色校验。原有 RestTemplate 和 OpenFeign 从当前已认证的 SecurityContext 透传 Bearer Token，商品服务在校验卖家时继续透传；不接受客户端自行声明的角色。

根 compose 仅发布网关业务端口，不发布用户、商品、商品第二实例、订单和数据库端口。Nacos 管理端口仍只绑定 `127.0.0.1`。外部客户端不能通过原 8081/8082/8083/8084 端口直接请求下游；脚本还从容器内部直连用户服务，验证无 Token 时仍返回 401。控制 Docker 守护进程或具有主机管理员权限的人属于部署管理员，网络不以隔离这类管理员为目标。

## 账号与本地配置

| 用户名 | 角色 | 本地测试密码 |
| --- | --- | --- |
| `student` | USER | `.env` 的 `AUTH_USER_PASSWORD` |
| `admin` | ADMIN | `.env` 的 `AUTH_ADMIN_PASSWORD` |

执行 `python scripts/auth-env.py` 自动生成随机密码和独立随机盐的哈希。Compose 仅向用户服务传递哈希；测试脚本从本机 `.env` 读取密码。已有数据库账号不会在重启时被覆盖，因此修改测试密码必须同时通过正式密码变更流程更新数据库哈希；不要只改 `.env` 后期待已有密码自动重置。

本次实现满足课堂实践的登录和访问控制；尚未实现 Token 吊销、刷新令牌、密钥轮换、登录限速和 HTTPS 终止。对外部署时应在 TLS 入口运行并补齐对应运维策略。

## 验证

根目录执行 `bash scripts/auth-test.sh`（Windows 使用 Git Bash），也可执行 `python scripts/auth-test.py`。脚本验证路由、登录成功及失败、401/403、管理员访问、伪造头覆盖、无 Token 伪造头、篡改 Token、过期 Token、错误签发者、买家身份冒用、真实下单与取消，以及未发布下游端口和下游独立验签。失败返回非零；每次使用独立商品数据，创建的订单在成功路径会取消，不清空数据库或删除卷。
