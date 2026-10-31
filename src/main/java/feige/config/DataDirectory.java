package feige.config;

import java.io.File;
import java.net.URL;

public final class DataDirectory {

    private DataDirectory() {
    }

    /**
     * 从 jar 启动时返回 jar 所在目录下的 data。
     * 从目录（IDE、测试）启动时返回当前工作目录下的 data。
     */
    public static File resolve() {
        try {
            URL url = DataDirectory.class.getProtectionDomain().getCodeSource().getLocation();
            String external = url.toExternalForm();
            if (external.startsWith("jar:")) {
                external = external.substring("jar:".length());
            }
            int bang = external.indexOf('!');
            if (bang >= 0) {
                external = external.substring(0, bang);
            }
            File location = new File(new URL(external).toURI());
            if (location.isFile() && location.getName().endsWith(".jar")) {
                File parent = location.getParentFile();
                if (parent != null) {
                    return new File(parent, "data");
                }
            }
        } catch (Exception ignored) {
            // 无法识别代码位置时使用工作目录
        }
        return new File(System.getProperty("user.dir"), "data");
    }
}
