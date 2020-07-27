function FindProxyForURL(url, host) {
    if (url === "https://1.2.3.5") {
        return "DIRECT"
    }
    return "PROXY proxyPacHost.org:8888; PROXY proxyPacSecondHost.org:8888";
}