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
import java.io.IOException;
import java.nio.file.Files;

@RestController
public class AvatarController {

    private final ProfileService profileService;

    public AvatarController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/api/avatars/{storedName}")
    public ResponseEntity<Resource> download(@PathVariable String storedName) throws IOException {
        File file = profileService.requireCustomAvatar(storedName);
        MediaType mediaType = MediaType.parseMediaType(guessImageType(file.getName()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .contentType(mediaType)
                .contentLength(file.length())
                .body(new FileSystemResource(file));
    }

    private static String guessImageType(String name) throws IOException {
        String lower = name == null ? "" : name.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        String probed = Files.probeContentType(new java.io.File(name).toPath());
        return probed != null ? probed : "application/octet-stream";
    }
}
