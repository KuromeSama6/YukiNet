# YukiNet 文档
YukiNet是一个简单的”云系统”或自动部署，用于Spigot+Bungee网络。

Github: https://github.com/KuromeSama6/YukiNet

## Overview
YukiNet是一个轻量级的自动部署系统，或所谓的“云系统”，用于您的Spigot+Bungeecord网络。自动部署系统省去了在迷你游戏服务器中手动重复启动每个服务器的麻烦。

例如，如果您需要 20 个 bedwars 服务器，每个服务器都相同，最好使用像 YukiNet 这样的自动部署系统，而不是遍历所有 20 个服务器，并在每次更改配置文件时更改每个服务器。

## 运作方式

YukiNet 目前支持单个代理和任意数量的服务器。一个 YukiNet 实例应该对应一个代理。

### 静态与模板

**静态**

静态服务器是独特游戏模式的唯一实例，其所有数据在关闭 YukiNet 时不会被删除。例如，如果你想要在你的迷你游戏服务器中有一个生存世界，生存服务器应该是静态的，因为1）只有一（1）个生存服务器，2）它的世界应该被保存。如果你熟悉OOP语言，你可能已经理解了这个概念。

静态服务器位于`/static`目录中。所述目录中的每个目录都被视为服务器并进行处理。您的代理必须是静态的。

**模板**

模板充当服务器的`蓝图`。它用于创建同一游戏模式的大量实例，其中插件和配置是相同的。当 YukiNet 启动时，模板会根据配置文件中指定的数量复制到`/live`目录中，每个副本代表一个单独的服务器。

例如，如果您需要 20 个 bedwars 服务器，您可以只为 bedwars 创建一 （1） 个模板，并在配置文件中指定 20 个数量。这将在运行时创建模板的 20 个副本。

模板还支持分层结构。对于给定的`/template`目录：
```
|- template
    |- .global
        |- plugins
            |- Anticheat
            |- Core
            |- Anticheat-1.0.jar
            |- Core-1.0.jar
        |- spigot.jar
        |- server.properties
        |- spigot.yml
    |- bedwars
        |- .yuki.yml
        |- plugins
            |- Bedwars
                |- arenas.yml
                |- shop.yml
            |- Bedwars-4.0-SNAPSHOT.jar
        |- world
    |- bedwars_experience_mode (copyTree: [bedwars])
        |- .yuki.yml
        |- plugins
            |- Bedwars
                |- shop.yml (Experience mode configuration)
    |- bedwars_solos
        |- .yuki.yml (copyTree: [bedwars, bedwars_experience_mode])
        |- plugins
            |- Bedwars
                |- arenas.yml (Solos configuration)
    
    |- lobby
        |- ...
|- static
|- config.yml
|- readme.md
```

复制模板时，目录的内容将按以下顺序复制到`live`目录中：
1. 复制`.global`。
2. 处理包含`.yuki.yml`文件的目录。不包含上述文件的目录不会被处理。
3. 如果`copyTree`（`List`<String>）存在于`.yuki.yml`文件中，则该列表中指定的文件将按相应的顺序复制。文件路径相对于`template`目录，不包含前导斜杠。

在上面的例子中，`bedwars_solos`将复制`bedwars`文件夹（其中可能包含，例如，Bedwars 插件），然后是`bedwars_experience_mode`（可能包含体验模式bedwars的配置，每个资源的价值等），最后是`bedwars_solos`本身的内容，其中可能包含每个团队的生成和床位。
4. 复制文件夹本身的内容。

复制时，更高目录中名称与已存在的文件冲突的文件将

### 部署和层次结构

YukiNet 支持服务器的多机基础设施。YukiNet支持
多台计算机之间的通信（如果具有相对较大的
网络。

**主控和部署**

master 是代理目录所在的 YukiNet 实例。YukiNet，截至
目前，仅支持一个代理。在其他“云系统”解决方案中，这是
通常称为“核心”或类似的东西。

部署是一个 YukiNet 实例，它位于主服务器以外的计算机上。

主服务器和部署之间通过 HTTP 请求进行通信。那里
是“config.yml”中的一个部分，需要您配置地址和端口。

### 控制

截至目前，YukiNet 不支持交互式控制台输入。控制
命令必须发送到主服务器或部署的 HTTP 服务
端点。这是在“http.this”部分的“config.yml”中配置的。

若要查看所有可用终结点的列表，请向“/help”发送“GET”无实体请求。例如：

```$ curl http://localhost:3982/help```

---

## 配置指南
**除非另有说明，否则配置中的所有路径都需要前导斜杠，并且没有尾部斜杠。
### 1.config.yml

`String proxyDir`

这是您的代理（无论是 BungeeCord、Waterfall 等）安装所在的目录。
截至目前，YukiNet仅支持一个代理服务器。

默认情况下，Poxy 目录在“/static”下创建。建议您将代理目录保存在
static 目录。

这是 Bungeecord 目录的入口点。有关 Bungeecord 的更多配置，请编辑“.yuki.yml”
在您的 bungeecord 目录中。

`boolean isDeployment`

此 YukiNet 实例是否为部署。如果设置为 'false'，则此 YukiNet 实例
是高手。有关说明，请参阅上一节。

`boolean restartServersOnStop`

服务器停止后是否应自动重新启动（10 秒内）。

`int serverStartInterval`

YukiNet 在启动下一台服务器之前应该等待多长时间（以毫秒为单位）。

`int portMin`

服务器可以使用的最小端口。每个服务器的端口自动递增 1，并跳过占用的端口。

例如，值“12000”将使启动的第一台服务器使用端口“12000”，第二台服务器使用端口“12001”，依此类推。如果“12002”被占用，则会自动跳过它，下一个服务器使用“12003”。

`int expect`

在重写代理配置和启动服务器之前，预期来自多少个部署的消息。

### HTTP协议

“http”部分。**如果您没有多台计算机，则可以安全地忽略本节中的配置。

“ConfigurationSection this”

定义运行此 YukiNet 实例的 HTTP 服务的端口，因此
名称“this”。

为了获得最佳功能，请在此处放置公共 IP。如果您没有多台计算机，请忽略此设置。

此 YukiNet 实例的 HTTP 服务始终在通配符 IP 上运行的节点，
不管“this.ip”中放了什么。“this.ip”字段是发送给主服务器的内容
（或部署），其他 YukiNet 实例将使用此字段中的 ip 进行连接
添加到此 YukiNet 实例。

`ConfigurationSection 主机`

定义主服务器的 HTTP 服务的地址。

如果此 YukiNet 实例是主实例，则将忽略此实例。

### 身份

某些服务器可能会在 Bungeecord 和 Spigot 实例之间使用自定义消息传递解决方案（例如，YukiMessenger，我
强烈建议您使用）。此类消息传递解决方案可能需要自定义的`id`或`ident`，每个
Spigot 实例，以便代理建立属于哪个消息传递连接
哪个 Spigot 服务器。

读取服务器核心 jar 目录中的`.yuki-info.yml`文件。使用
`server-id`字段作为您的服务器名称。

示例`.yuki-info.yml`：

```yaml
DO-NOT-CHANGE-THIS-FILE: DO-NOT-CHANGE-THIS-FILE
CHANGES-ARE-NOT-SAVED: This file is regenerated each time the server starts. Any data is not saved.
generation-time: '2023-06-22T18:26:24.025'
generation-timestamp: 1687429584025
server-id: lobby1
group-id: lobby
port: 10000
is-static: false
```

`boolean enable`

是否启用 YukiNet 标识。

`String cmd`

缩进时要执行的命令。所有出现的“{}”都替换为
服务器的 ID。

### 2. Proxy Configuration (static/proxy/.yuki.yml)

`String cmd`

您的 java 安装。

`String jarFile`

代理的 jar 文件的名称。相对于代理的目录。

`List<String> args`

这是启动代理时附加到`cmd`的参数列表。

`String configPath`

这是代理的`/config.yml`文件所在的位置。YukiNet 将读取此文件并更改服务器配置。

默认值为 `/config.yml`.

`boolean writePriorities`

此服务器是否会写入代理的`config.yml`的`priorities`列表中。

默认值为`true`。

### 3.非静态服务器配置 （template/*/.yuki.yml）

`String id`
此模板/组的名称。一个数字被附加到服务器的

`String cmd`

您的 java 安装。

`String jarFile`

服务器的 jar 文件的名称。相对于服务器的目录。

`List<String> args`

这是启动服务器时附加到`cmd`的参数列表。

** 不要使用 Bukkit 的 `--port` 启动参数。此参数已用于动态选择此服务器的端口。

`int count`

启动了此模板的服务器数。

`boolean randomCopy`

服务器启动时，是否应将随机文件夹复制到服务器的工作目录中。最适合动态随机化地图。

若要复制，该组的 **template** 目录中必须存在`.random`目录。从该目录中随机选择一个文件夹，并使用相同的覆盖逻辑复制到服务器的工作目录中。

**请注意，此操作是在每次服务器启动时完成的**。

`boolean writePriorities`

此服务器是否会写入代理的`config.yml”的`优先级”列表中。

默认值为`true”。
