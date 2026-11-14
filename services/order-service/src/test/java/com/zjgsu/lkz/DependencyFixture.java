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
    static volatile Integer productStatus=null;
    static volatile long productDelay=0;
    static final java.util.concurrent.atomic.AtomicInteger productCalls=new java.util.concurrent.atomic.AtomicInteger();
    static {
        try {
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/",e->{
                try {
                    boolean product=e.getRequestURI().getPath().startsWith("/api/items/");
                    if(product) productCalls.incrementAndGet();
                    long wait=product ? Math.max(delay,productDelay) : delay;
                    if (wait>0) Thread.sleep(wait);
                    byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
                    e.getResponseHeaders().set("Content-Type","application/json");
                    e.sendResponseHeaders(product && productStatus!=null ? productStatus : status,bytes.length);
                    e.getResponseBody().write(bytes);
                } catch (Exception ignored) { } finally { e.close(); }
            });
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool(r -> { Thread t=new Thread(r); t.setDaemon(true); return t; }));
            server.start();
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    static String url() { return "http://127.0.0.1:"+server.getAddress().getPort(); }
}
