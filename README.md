# 知享 - 校园知识库共享平台

## 技术栈
- 后端：Spring Boot 3.5 + Spring Security + JPA + MySQL + WebSocket
- 前端：Vue 3 + TypeScript + Element Plus
- 其他：JWT双Token认证、RBAC权限控制、积分激励体系

## 功能模块
- 用户认证：JWT双Token无感刷新、登录失败锁定、邮箱验证
- 文档管理：上传、审核、下载、积分结算
- 实时通知：WebSocket推送审核结果
- 权限控制：基于角色的方法级鉴权
- AI搜索：基于向量数据库的智能文档检索

## 运行说明
1. 导入数据库脚本（位于 `knowledge_platform-backend/src/main/resources/db/`）
2. 修改 `application.yml` 中的数据库配置（将密码占位符改为真实密码）
3. 启动 Spring Boot 项目
4. 前端 `npm install` → `npm run dev`

## 配置说明
运行前请修改以下配置：
- `application.yml` 中的数据库密码
- `application.yml` 中的邮箱授权码
- `application.yml` 中的AI API密钥（可选，用于智能搜索功能）
