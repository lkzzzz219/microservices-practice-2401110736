package com.zjgsu.linkunze;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 运行状态验证接口。
 *
 * <p>本周仅用于验证应用可启动、可访问，不涉及任何业务逻辑。
 * 返回固定的项目名称与问候消息。</p>
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of(
                "project", "CampusTrade 校园二手交易平台",
                "message", "Hello from CampusTrade monolith! 应用启动成功。"
        );
    }
}
