package pre.wc.mediamanager.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 配置读取演示接口（第10题）。
 * 使用 @Value 注入 app.name / app.version，并通过 GET /api/v1/info 返回。
 */
@RestController
@RequestMapping("/api/v1")
public class InfoController {

    @Value("${app.name:media-platform}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of("name", appName, "version", appVersion);
    }
}
