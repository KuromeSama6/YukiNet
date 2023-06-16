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
        |- plugins
            |- Bedwars
                |- arenas.yml
                |- shop.yml
            |- Bedwars-4.0-SNAPSHOT.jar
        |- world
    |- bedwars.0.experience_mode
        |- plugins
            |- Bedwars
                |- shop.yml (Experience mode configuration)
    |- bedwars.1.solos
        |- plugins
            |- Bedwars
                |- arenas.yml (Solos configuration)
    
    |- lobby
        |- ...
|- static
|- config.yml
|- readme.md
```

When copying a template, content of directories are copied in the following order:
1. `.global` is copied.
2. Directory which name contains only one argument (seperated by period(.)), in this case `bedwars`, is copied.
3. The second argument in directories' names are parsed, and are copied in ascending order. Any arguments after the second are for remarks only and are skipped. Non-integers are skipped. In this case, first `bedwars.0.experience_mode`, then `bedwars.1.solos`.

When copying, files in later directories that have a conflicting names with files that already exist will overwrite the existing files.

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

### 3. Non-Static Server Configuration (template/**/.yuki.yml)

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

`int portMin`

Minimum port this template/group will use

For example, for the template `lobby` this is set to 10000, then the first lobby server, `lobby1` will be running on port
10000, the second on 10001, and so on.

`int portMax`

Maximum port this template/group will use.

If all ports within the [portMin, portMax] range is used, a server will not start. A warning will be generated in the console.