package com.zjgsu.lkz.client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Each consumer owns its HTTP contract; no dependency on another service's Java code. */
@Component
public class RemoteApi {
    private final org.springframework.cloud.client.discovery.DiscoveryClient discovery;
    private final RestTemplate http;
    public RemoteApi(org.springframework.cloud.client.discovery.DiscoveryClient discovery, @Value("${clients.connect-timeout-ms}") int connect,
                     @Value("${clients.read-timeout-ms}") int read) {
        this.discovery = discovery;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connect); factory.setReadTimeout(read);
        http = new RestTemplate(factory);
    }
    public <T> T get(String service, String path, Class<T> type) { return call(service, path, null, type, false); }
    public <T> T post(String service, String path, Object body, Class<T> type) { return call(service, path, body, type, true); }
    private <T> T call(String service, String path, Object body, Class<T> type, boolean post) {
        // Query the current known instances for every operation; never keep a startup URL.
        var instances = discovery.getInstances(service);
        if (instances.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No available instance: " + service);
        var uri = instances.getFirst().getUri();
        String url = uri.toString() + path;
        org.slf4j.LoggerFactory.getLogger(getClass()).info("DISCOVERY {} -> {} {}", service, post ? "POST" : "GET", url);
        try {
            T result = post ? http.postForObject(url, body, type) : http.getForObject(url, type);
            if (result == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty dependency response");
            var attrs = (org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null && attrs.getResponse() != null)
                attrs.getResponse().setHeader("X-Discovery-" + service, uri.toString());
            return result;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dependency resource not found");
            if (e.getStatusCode().value() == 409)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Dependency business conflict");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Dependency rejected request", e);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dependency unavailable or timed out", e);
        }
    }
}
