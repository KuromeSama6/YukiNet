# YukiNet Built-In Documentation
YukiNet is a simple "Cloud System" or auto-deployment for Spigot+Bungee networks.

Github: https://github.com/KuromeSama6/YukiNet

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

This gets ran when the proxy is started.

`List<String> `

This is a list of arguments that gets appended to `cmd` when starting the proxy.

`String configPath`

This is where your proxy's `/config.yml` file is. YukiNet will read this file and change server configurations.

This is defaulted to `/config.yml`.

### 3. Non-Static Server Configuration (template/**/.yuki.yml)

`String id`
The name of this template/group. A number is appended onto the server's 

`String cmd`

This gets ran when the server is started. This is ran within the server's directory.

**Do not use Bukkit's `--port` startup parameter. This parameter is already used to dynamically select a port for this server.**

`List<String>`

This is a list of arguments that gets appended to `cmd` when starting the server.

`int count`

How many servers of this template is started.

`int portMin`

Minimum port this template/group will use

For example, for the template `lobby` this is set to 10000, then the first lobby server, `lobby1` will be running on port
10000, the second on 10001, and so on.

`int portMax`

Maximum port this template/group will use.

If all ports within the [portMin, portMax] range is used, a server will not start. A warning will be generated in the console.