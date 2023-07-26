# YukiNet Documentation
YukiNet is a simple "Cloud System" or auto-deployment for Spigot+Bungee networks.

Github: https://github.com/KuromeSama6/YukiNet

## Overview
YukiNet is a lightweight auto-deployment system , or so-called "Cloud System" for your Spigot+Bungeecord network. An auto-deployment system saves the hassle of repetitively starting each server by hand in a minigames server.

For example, if you need 20 bedwars servers, with each of them the same, it is best to use an auto-deployment system like YukiNet instead of going through all 20 servers and changing each of them everytime a change is made to a config file.

## How it works

YukiNet currently supports a single proxy and any amount of servers. One YukiNet instance should correspond to one proxy.

### Static vs. Templates

**Static**

A static server is a server where it is the only instance of a unique gamemode, and all its data are not deleted upon shutting down YukiNet. For example, if you would like a survival world within your minigames server, the survival server should be static, as 1) there is only one(1) survival server and 2) its world should be saved. If you are familiar with OOP languages, you may have already comprehended this concept.

Static servers goes in the `/static` directory. Each directory in said directory is seen and processed as a server. Your proxy MUST be static.

**Template**

Template acts as a "blueprint" for a server. It is used to create numerous instances of the same gamemode, where plugins and configurations are the same. When YukiNet starts, templates get copied into the `/live` directory depending on the amount specified in the config files, and each copy represents a individual server.

For example, if you need 20 bedwars servers, you could just make one(1) template for bedwars, and specify an amount of 20 in the configuration file. This would make 20 copies of the template during runtime.

Templates also supports hierarchical structure. For a given `/template` directory:
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

When copying a template, content of directories are copied into the `live` directory in the following order:
1. `.global` is copied.
2. Directories containing a `.yuki.yml` file are processed. Directories that do not contain said file are not processed.
3. If the `copyTree` (`List<String>`) is present in the `.yuki.yml` file, then the files specified in that list is copied in respective order. File paths are relative to the `template` directory and includes no leading slash.

    In the above example, `bedwars_solos` would copy the `bedwars` folder (which may contain, say, the Bedwars plugin), and then `bedwars_experience_mode` (which may contain configurations for experience mode bedwars, how much each resource is worth, etc.), and finally contents in `bedwars_solos` itself, which may contain spawns and bed positions for each team.
4. The contents of the folder itself is copied.

When copying, files in later directories that have a conflicting names with files that already exist will overwrite the existing files.

### Deployment and Hierarchy

YukiNet supports multi-machine infrastructure for your server. YukiNet supports
communication between multiple machines if you have a relatively large
network.

**Master and Deployment**

A master is the YukiNet instance where the proxy directory is located. YukiNet, as of
right now, supports only one proxy. In other "cloud system" solutions, this is
oftentimes called the "core" or something similiar.

A deployment is a YukiNet instance where it is on a machine other than the master.

Communication is made between the master and deployments through HTTP requests. There
is a section in `config.yml` that requires you to configure addressses and ports.

### Control

As of right now, YukiNet does not have support for interactive console input. Control
commands must be sent to the master's or to a deployment's HTTP service
endpoint. This is configured in `config.yml` in the `http.this` section.

To see a list of all available endpoints, sent a `GET` bodiless request to `/help`. For example:

```$ curl http://localhost:3982/help```

---

## Configuration Guide
**All paths in configuration requires a leading slash and no trailing slash unless otherwise specified.**
### 1. config.yml

`String proxyDir`

This is the directory where whatever your proxy (be it BungeeCord, Waterfall, etc.) installation is located.
YukiNet supports only one proxy server as of right now.

By default, the Poxy directory is created under `/static`. It is advised that you keep your proxy directory within the
static directory.

This is an entrypoint to the Bungeecord directory. For more configurations regarding Bungeecord, edit the `.yuki.yml`
in your bungeecord directory.

`boolean isDeployment`

Whether this instance of YukiNet is a deployment. If set to `false`, the this YukiNet instance
is a master. See the previous section for explanation.

`boolean restartServersOnStop`

Whether a server should be automatically restarted (in 10 seconds) after it stops.

`int serverStartInterval`

How long **in milliseconds** should YukiNet wait before starting the next server. 

`int portMin`

Minimum port that the servers may use. Port increments by 1 for each server automatically and skips occupied ports.

For example, a value of `12000` would make the first server that starts use port `12000`, the second server `12001`, and so on. if `12002` is occupied, then it is automatically skipped, and the next server uses `12003`.

`int expect`

How many deployments to expect messages from before rewriting proxy config and starting the server.

### HTTP

The `http` section. **Configuration in this section can be safely ignored if you do not have multiple machines.**

`ConfigurationSection this`

Defines the port that the HTTP service of this YukiNet instance will be ran on, hence
the name "this".

For best functionality, put the public IP here. Ignore this if you do not have multiple machines.

Node that the HTTP service for this YukiNet instance always runs on the wildcard IP,
regardless of what is put in `this.ip`. The `this.ip` field is what is sent to the master
(or deployments), and other YukiNet instances will use the ip in this field to connect
to this YukiNet instance.

`ConfigurationSection master`

Defines the address of the master's HTTP service.

If this YukiNet instance is the master, this will be ignored.

### Ident

Some servers may use a custom messaging solution between Bungeecord and Spigot instances (for example, YukiMessenger, which I
strongly suggest that you use). Such messaging solution may require a custom "id" or "ident" per 
Spigot instance in order for the proxy to establish which messaging connection belongs to 
which Spigot server.

Read the `.yuki-info.yml` file in the server's core jar's directory. Use the
`server-id` field as your server name.

Sample `.yuki-info.yml`:

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

Whether to enable YukiNet ident.

`String cmd`

The command to be executed when identing. All occurances of `{}` are replaced with
the server's id.

### 2. Proxy Configuration (static/proxy/.yuki.yml)

`String cmd`

Your java installation.

`String jarFile`

The name of your proxy's jar file. Relative to the proxy's directory.

`List<String> args`

This is a list of arguments that gets appended to `cmd` when starting the proxy.

`String configPath`

This is where your proxy's `/config.yml` file is. YukiNet will read this file and change server configurations.

This is defaulted to `/config.yml`.

`boolean writePriorities`

Whether this server will be written into the proxy's `config.yml`'s `priorities` list.

This is defaulted to `true`.

### 3. Non-Static Server Configuration (template/*/.yuki.yml)

`String id`
The name of this template/group. A number is appended onto the server's 

`String cmd`

Your java installation.

`String jarFile`

The name of your server's jar file. Relative to the server's directory.

`List<String> args`

This is a list of arguments that gets appended to `cmd` when starting the server.

**Do not use Bukkit's `--port` startup parameter. This parameter is already used to dynamically select a port for this server.**

`int count`

How many servers of this template is started.

`boolean randomCopy`

Whether upon server start, a random folder should be copied into the working directory of the server. Best for dynamically randomizing maps.

To copy, a `.random` directory must be present in the **template** directory of that group. A folder is selected by random from that directory, and copied using the same overwrite logic into the server's working directory.

**Note that this operation is done every time that server starts**.

`boolean writePriorities`

Whether this server will be written into the proxy's `config.yml`'s `priorities` list.

This is defaulted to `true`.
