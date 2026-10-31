package feige.web;

import feige.domain.FeigeUser;
import feige.service.ChatService;
import feige.service.Download;
import feige.service.MessageFanout;
import feige.service.MessageView;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;

@RestController
public class FileController {

    private final ChatService chats;
    private final MessageFanout fanout;

    public FileController(ChatService chats, MessageFanout fanout) {
        this.chats = chats;
        this.fanout = fanout;
    }

    @PostMapping("/api/conversations/{id}/files")
    public MessageView upload(@AuthenticationPrincipal FeigeUser user,
                              @PathVariable("id") long conversationId,
                              @RequestParam(value = "file", required = false) MultipartFile file,
                              @RequestParam(value = "quoteMessageId", required = false) Long quoteMessageId)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw new feige.service.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, feige.service.FileStorage.EMPTY);
        }
        MessageView view = chats.saveUpload(user.getId(), conversationId, file.getOriginalFilename(),
                file.getInputStream(), quoteMessageId);
        fanout.deliver(view);
        return view;
    }

    @PostMapping("/api/conversations/{id}/images")
    public MessageView uploadImage(@AuthenticationPrincipal FeigeUser user,
                                   @PathVariable("id") long conversationId,
                                   @RequestParam(value = "file", required = false) MultipartFile file,
                                   @RequestParam(value = "quoteMessageId", required = false) Long quoteMessageId)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw new feige.service.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, feige.service.FileStorage.EMPTY);
        }
        MessageView view = chats.saveImage(user.getId(), conversationId, file.getOriginalFilename(),
                file.getContentType(), file.getInputStream(), quoteMessageId);
        fanout.deliver(view);
        return view;
    }

    @GetMapping("/api/files/{messageId}")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal FeigeUser user,
                                             @PathVariable long messageId) throws IOException {
        Download download = chats.requireDownload(user.getId(), messageId);
        String encoded = URLEncoder.encode(download.getOriginalName(), "UTF-8").replace("+", "%20");
        String disposition = (download.isInline() ? "inline" : "attachment") + "; filename*=UTF-8''" + encoded;
        MediaType mediaType = download.isInline()
                ? MediaType.parseMediaType(guessImageType(download.getOriginalName()))
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(mediaType)
                .contentLength(download.getFile().length())
                .body(new FileSystemResource(download.getFile()));
    }

    private static String guessImageType(String name) {
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
        return "image/jpeg";
    }
}
