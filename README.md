# AI Study - 智能学习辅助平台

AI Study 是一个基于 SSM (Spring + Spring MVC + MyBatis) 架构开发的智能学习辅助 Web 应用。它集成了阿里云语音识别（ASR）、自然语言处理（NLP）和知识图谱可视化技术，旨在帮助用户高效管理学习资料，将语音和文档转化为结构化的知识笔记。

## 🚀 主要功能

-   **用户系统**：
    -   支持用户注册与登录。
    -   安全的会话管理，集成全局登录拦截器 (LoginInterceptor)，统一处理 API 和页面的访问权限。
    -   优化 AJAX 请求的会话超时处理，防止操作静默失败。

-   **语音转写**：
    -   **短语音转写**：支持 60 秒内的语音文件实时转写。
    -   **长语音转写**：支持大文件上传，后台异步处理，并提供任务状态轮询与进度展示。

-   **资料解析**：
    -   支持上传 PDF、Word 等文档资料。
    -   自动提取文本内容并生成笔记。

-   **智能笔记**：
    -   基于转写和解析结果自动生成学习笔记。
    -   支持笔记的查看、编辑和删除。

-   **知识图谱**：
    -   **嵌入式视图**：知识图谱直接集成在控制台首页，无需跳转即可预览。
    -   **词频权重**：节点大小根据关键词出现的频率动态调整，直观展示重点知识。
    -   利用 NLP 技术提取笔记中的关键词与实体。
    -   使用 D3.js 可视化展示知识点之间的关联图谱。
    -   支持节点拖拽与缩放交互。

## 🛠 技术栈

### 后端
-   **核心框架**: Spring 5.3, Spring MVC 5.3
-   **持久层**: MyBatis 3.5
-   **数据库**: MySQL 8.0 / 5.7
-   **构建工具**: Maven
-   **云服务集成**:
    -   阿里云智能语音交互 (NlsClient)
    -   阿里云 OSS (对象存储)

### 前端
-   **模板引擎**: Thymeleaf
-   **UI 框架**: Bootstrap 5
-   **可视化**: D3.js v7
-   **交互**: Fetch API, SweetAlert2

## 📋 环境要求

-   **JDK**: 1.8+
-   **Maven**: 3.6+
-   **Tomcat**: 9.0+
-   **MySQL**: 5.7+

## ⚙️ 配置说明

### 1. 数据库配置
项目启动时会自动运行 `schema.sql` 初始化数据库表结构。请确保 MySQL 服务已启动，并创建好对应的数据库。

修改 `src/main/resources/application.properties` 中的数据库连接信息：
```properties
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/kgraph?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
jdbc.username=root
jdbc.password=liutaotao0202
```

### 2. 阿里云服务配置
在项目根目录下创建 `.env` 文件（参考 `.env.example`），填入你的阿里云密钥信息：

```properties
ALIYUN_APP_KEY=your_app_key
ALIYUN_ACCESS_KEY_ID=your_access_key_id
ALIYUN_ACCESS_KEY_SECRET=your_access_key_secret
ALIYUN_REGION=cn-shanghai
ALIYUN_OSS_ENDPOINT=oss-cn-shanghai.aliyuncs.com
ALIYUN_OSS_BUCKET=your_bucket_name
```

## 📦 安装与运行

1.  **克隆项目**
    ```bash
    git clone <repository_url>
    cd AI-Study
    ```

2.  **编译打包**
    ```bash
    mvn clean package -DskipTests
    ```

3.  **部署到 Tomcat**
    -   将 `target/kgraph-0.1.0.war` 复制到 Tomcat 的 `webapps` 目录下。
    -   或者在 IDEA 中配置 Tomcat Server，Artifact 选择 `kgraph:war exploded`。

4.  **访问应用**
    -   启动 Tomcat。
    -   浏览器访问：`http://localhost:8080/` (具体路径取决于 Tomcat 配置的 Context Path)。

## 📂 项目结构

```
AI-Study/
├── src/
│   ├── main/
│   │   ├── java/com/study/kgraph/
│   │   │   ├── config/       # 配置类 (Env加载, WebConfig, Interceptor配置)
│   │   │   ├── controller/   # 控制器 (Web层)
│   │   │   ├── entity/       # 实体类
│   │   │   ├── interceptor/  # 拦截器 (登录检查等)
│   │   │   ├── mapper/       # MyBatis Mapper接口
│   │   │   └── service/      # 业务逻辑层
│   │   ├── resources/
│   │   │   ├── mapper/       # MyBatis XML文件
│   │   │   ├── spring/       # Spring 配置文件
│   │   │   ├── static/       # 静态资源 (CSS, JS)
│   │   │   ├── templates/    # Thymeleaf 模板页面
│   │   │   ├── application.properties # 数据库配置
│   │   │   └── schema.sql    # 数据库初始化脚本
│   │   └── webapp/
│   │       └── WEB-INF/
│   │           └── web.xml   # Web 应用部署描述符
└── pom.xml                   # Maven 依赖配置
```

## 📝 待办事项 / 开发计划

- [ ] 优化 NLP 关键词提取算法。
- [ ] 增加用户头像上传功能。
- [ ] 支持更多格式的文档解析。
- [ ] 添加笔记分类与标签管理。

---
**注意**: 本项目仅供学习交流使用。
