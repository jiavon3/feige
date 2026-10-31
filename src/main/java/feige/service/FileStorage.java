package feige.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

@Component
public class FileStorage {

    public static final long MAX_BYTES = 100L * 1024L * 1024L;
    public static final String TOO_BIG = "文件不能超过 100MB";
    public static final String EMPTY = "不能上传空文件";
    public static final String MISSING = "文件已不存在";
    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private final File directory;

    public FileStorage(@Value("${feige.data-dir}") String dataDir) {
        this.directory = new File(dataDir, "files");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IllegalStateException("无法创建文件目录: " + directory.getAbsolutePath());
        }
    }

    public File directory() {
        return directory;
    }

    public String save(InputStream input) {
        String stored = UUID.randomUUID().toString();
        File target = new File(directory, stored);
        try {
            Files.copy(input, target.toPath());
        } catch (IOException ex) {
            target.delete();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败");
        }
        long length = target.length();
        if (length <= 0) {
            target.delete();
            throw new ApiException(HttpStatus.BAD_REQUEST, EMPTY);
        }
        if (length > MAX_BYTES) {
            target.delete();
            throw new ApiException(HttpStatus.BAD_REQUEST, TOO_BIG);
        }
        return stored;
    }

    public File require(String storedName) {
        if (storedName == null || !storedName.matches(UUID_PATTERN)) {
            throw new ApiException(HttpStatus.NOT_FOUND, MISSING);
        }
        File file = new File(directory, storedName);
        try {
            String root = directory.getCanonicalPath();
            String path = file.getCanonicalPath();
            if (!path.startsWith(root + File.separator)) {
                throw new ApiException(HttpStatus.NOT_FOUND, MISSING);
            }
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, MISSING);
        }
        if (!file.isFile()) {
            throw new ApiException(HttpStatus.NOT_FOUND, MISSING);
        }
        return file;
    }

    public void deleteQuietly(String storedName) {
        if (storedName == null || !storedName.matches(UUID_PATTERN)) {
            return;
        }
        new File(directory, storedName).delete();
    }

    public void deleteAll() {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile()) {
                file.delete();
            }
        }
    }
}
