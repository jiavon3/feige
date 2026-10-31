package feige.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

@Component
public class StartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    private final Environment environment;
    private final String dataDir;

    public StartupLogger(Environment environment, @Value("${feige.data-dir}") String dataDir) {
        this.environment = environment;
        this.dataDir = dataDir;
    }

    @Override
    public void run(ApplicationArguments args) {
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        log.info("数据目录: {}", new File(dataDir).getAbsolutePath());
        log.info("本机访问: http://127.0.0.1:{}", port);
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback() || network.isPointToPoint()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address) {
                        log.info("局域网访问: http://{}:{}", address.getHostAddress(), port);
                    }
                }
            }
        } catch (Exception ignored) {
            // 没有网卡信息时只保留本机地址
        }
    }
}
