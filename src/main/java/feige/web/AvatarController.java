package feige.web;

import feige.service.ProfileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
public class AvatarController {

    private final ProfileService profileService;

    public AvatarController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/api/avatars/{storedName}")
    public ResponseEntity<Resource> download(@PathVariable String storedName) {
        File file = profileService.requireCustomAvatar(storedName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .contentType(detectImageType(file))
                .contentLength(file.length())
                .body(new FileSystemResource(file));
    }

    /** 头像文件用 UUID 存盘、没有扩展名，只能按文件头判断类型。 */
    private static MediaType detectImageType(File file) {
        byte[] head = new byte[12];
        int read;
        try (InputStream input = new FileInputStream(file)) {
            read = input.read(head);
        } catch (IOException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        if (read >= 8 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            return MediaType.IMAGE_PNG;
        }
        if (read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return MediaType.IMAGE_JPEG;
        }
        if (read >= 6 && head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8') {
            return MediaType.IMAGE_GIF;
        }
        if (read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
