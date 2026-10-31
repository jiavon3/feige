package feige;

import feige.config.DataDirectory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class FeigeApplication {

    public static void main(String[] args) {
        File data = DataDirectory.resolve();
        if (!data.isDirectory() && !data.mkdirs()) {
            throw new IllegalStateException("无法创建数据目录: " + data.getAbsolutePath());
        }
        System.setProperty("feige.data-dir", data.getAbsolutePath());
        SpringApplication.run(FeigeApplication.class, args);
    }
}
