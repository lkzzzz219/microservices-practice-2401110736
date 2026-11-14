package com.zjgsu.lkz.web;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
@Component
public class InstanceHeaderFilter extends OncePerRequestFilter {
    private final String instance;
    public InstanceHeaderFilter(@Value("${INSTANCE_ID:${HOSTNAME:local}:${server.port:8082}}") String instance) { this.instance=instance; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        response.setHeader("X-Instance-Id",instance);
        logger.info("INSTANCE "+instance+" "+request.getMethod()+" "+request.getRequestURI());
        chain.doFilter(request,response);
    }
}
