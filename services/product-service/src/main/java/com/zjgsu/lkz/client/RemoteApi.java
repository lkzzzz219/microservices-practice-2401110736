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
    private final RestTemplate http;
    public RemoteApi(@Value("${clients.connect-timeout-ms}") int connect,
                     @Value("${clients.read-timeout-ms}") int read) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connect); factory.setReadTimeout(read);
        http = new RestTemplate(factory);
    }
    public <T> T get(String url, Class<T> type) { return call(url, null, type, false); }
    public <T> T post(String url, Object body, Class<T> type) { return call(url, body, type, true); }
    private <T> T call(String url, Object body, Class<T> type, boolean post) {
        try {
            T result = post ? http.postForObject(url, body, type) : http.getForObject(url, type);
            if (result == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty dependency response");
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
