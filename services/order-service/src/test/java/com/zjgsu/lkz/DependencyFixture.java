package com.zjgsu.lkz;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
/** Real loopback HTTP fixture, not a mock of the consumer's service or repository. */
final class DependencyFixture {
    static final HttpServer server;
    static volatile int status=200;
    static volatile long delay=0;
    static volatile String body="{}";
    static {
        try {
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/",e->{
                try {
                    if (delay>0) Thread.sleep(delay);
                    byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
                    e.getResponseHeaders().set("Content-Type","application/json");
                    e.sendResponseHeaders(status,bytes.length);
                    e.getResponseBody().write(bytes);
                } catch (Exception ignored) { } finally { e.close(); }
            });
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool(r -> { Thread t=new Thread(r); t.setDaemon(true); return t; }));
            server.start();
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    static String url() { return "http://127.0.0.1:"+server.getAddress().getPort(); }
}
