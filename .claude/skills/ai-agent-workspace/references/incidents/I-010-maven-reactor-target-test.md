# I-010: Maven reactor 定向测试参数坑

## 故障签名

在只跑某个模块的指定测试时，如果命令写成：

```bash
mvn -pl ai-agent-infrastructure -Dtest=ToolNodeExecutorStrategyTest test
```

可能出现两个误导性问题。

## 坑点 1：未带 `-am` 使用旧 SNAPSHOT 依赖

`ai-agent-infrastructure` 依赖 `ai-agent-domain`。如果不带 `-am`，Maven 可能直接使用本地仓库中的旧 `ai-agent-domain` SNAPSHOT，而不是当前工作区源码。

表现可能是编译错误出现在无关文件，例如：

```text
EmailServiceImpl.java:[43,5] method does not override or implement a method from a supertype
```

这类错误不一定是当前源码真的不一致，可能只是 Maven 解析到了旧依赖。

## 坑点 2：带 `-am -Dtest=...` 后上游模块无同名测试失败

修正为：

```bash
mvn -pl ai-agent-infrastructure -am -Dtest=ToolNodeExecutorStrategyTest test
```

后，reactor 会包含上游模块。如果上游模块没有 `ToolNodeExecutorStrategyTest`，Surefire 默认会失败：

```text
No tests matching pattern "ToolNodeExecutorStrategyTest" were executed!
```

## 推荐命令

定向跑 infrastructure 测试时使用：

```bash
mvn -pl ai-agent-infrastructure -am -Dtest=ToolNodeExecutorStrategyTest -Dsurefire.failIfNoSpecifiedTests=false test
```

完整跑 infrastructure 及依赖：

```bash
mvn -pl ai-agent-infrastructure -am test -Dsurefire.failIfNoSpecifiedTests=false
```

## 判断原则

- 如果错误来自当前任务无关模块，先检查命令是否缺 `-am` 或缺 `-Dsurefire.failIfNoSpecifiedTests=false`。
- 不要为了通过当前任务验证而修改无关模块。
- 如果带推荐命令后仍失败，再按真实编译/测试失败处理。

