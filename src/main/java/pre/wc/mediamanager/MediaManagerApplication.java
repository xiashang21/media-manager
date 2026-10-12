package pre.wc.mediamanager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("pre.wc.mediamanager.mapper")
public class MediaManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaManagerApplication.class, args);
    }

}
