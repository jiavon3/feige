package feige.service;

import feige.domain.UserAccount;
import feige.repo.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfileService {

    public static final long MAX_AVATAR_BYTES = 2L * 1024L * 1024L;
    public static final String AVATAR_TOO_BIG = "头像不能超过 2MB";
    public static final String AVATAR_EMPTY = "不能上传空头像";
    public static final String AVATAR_TYPE_INVALID = "仅支持 jpeg/png/gif/webp 格式头像";
    public static final String PASSWORD_WRONG = "当前密码错误";
    public static final String PASSWORD_SHORT = "新密码不能少于 6 位";
    public static final String PASSWORD_LONG = "新密码不能超过 64 位";
    public static final String NICKNAME_TOO_LONG = "昵称不能超过 30 个字符";

    private static final String BUILTIN_PREFIX = "builtin:";
    private static final String CUSTOM_PREFIX = "custom:";
    private static final Set<String> ALLOWED_IMAGE_TYPES = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp")));
    private static final List<String> BUILTIN_AVATARS = Collections.unmodifiableList(
            Arrays.asList(
                    "avatar1.png", "avatar2.png", "avatar3.png", "avatar4.png",
                    "avatar5.png", "avatar6.png", "avatar7.png", "avatar8.png",
                    "avatar9.png", "avatar10.png", "avatar11.png", "avatar12.png",
                    "avatar13.png", "avatar14.png", "avatar15.png", "avatar16.png"));
    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final File directory;

    public ProfileService(UserAccountRepository users,
                          PasswordEncoder passwordEncoder,
                          @Value("${feige.data-dir}") String dataDir) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.directory = new File(dataDir, "avatars");
    }

    @PostConstruct
    public void init() {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IllegalStateException("无法创建头像目录: " + directory.getAbsolutePath());
        }
    }

    public List<String> listBuiltins() {
        return BUILTIN_AVATARS;
    }

    public UserAccount updateNickname(Long userId, String nickname) {
        UserAccount account = users.findById(userId).orElseThrow();
        account.setNickname(normalizeNickname(nickname));
        return users.save(account);
    }

    public UserAccount selectBuiltinAvatar(Long userId, String name) {
        name = normalizeBuiltinName(name);
        if (!BUILTIN_AVATARS.contains(name)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不存在的内置头像");
        }
        UserAccount account = users.findById(userId).orElseThrow();
        deleteOldCustomAvatar(account);
        account.setAvatar(BUILTIN_PREFIX + name);
        return users.save(account);
    }

    public UserAccount uploadCustomAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, AVATAR_EMPTY);
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, AVATAR_TOO_BIG);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, AVATAR_TYPE_INVALID);
        }
        UserAccount account = users.findById(userId).orElseThrow();
        deleteOldCustomAvatar(account);
        String stored = save(file);
        account.setAvatar(CUSTOM_PREFIX + stored);
        return users.save(account);
    }

    public UserAccount updateProfile(Long userId, String nickname, String avatar) {
        UserAccount account = users.findById(userId).orElseThrow();
        if (nickname != null) {
            account.setNickname(normalizeNickname(nickname));
        }
        if (avatar != null) {
            if (avatar.startsWith(BUILTIN_PREFIX)) {
                String name = normalizeBuiltinName(avatar.substring(BUILTIN_PREFIX.length()));
                if (!BUILTIN_AVATARS.contains(name)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "不存在的内置头像");
                }
                deleteOldCustomAvatar(account);
                account.setAvatar(BUILTIN_PREFIX + name);
            } else {
                throw new ApiException(HttpStatus.BAD_REQUEST, "头像格式错误");
            }
        }
        return users.save(account);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        UserAccount account = users.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, PASSWORD_WRONG);
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, PASSWORD_SHORT);
        }
        if (newPassword.length() > 64) {
            throw new ApiException(HttpStatus.BAD_REQUEST, PASSWORD_LONG);
        }
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(account);
    }

    public String resolveAvatarUrl(String avatarToken) {
        if (avatarToken == null) {
            return null;
        }
        if (avatarToken.startsWith(BUILTIN_PREFIX)) {
            return "/avatars/" + normalizeBuiltinName(avatarToken.substring(BUILTIN_PREFIX.length()));
        }
        if (avatarToken.startsWith(CUSTOM_PREFIX)) {
            return "/api/avatars/" + avatarToken.substring(CUSTOM_PREFIX.length());
        }
        return null;
    }

    /** Older letter SVGs map onto the new illustrated PNG set. */
    private static String normalizeBuiltinName(String name) {
        if (name != null && name.matches("avatar[1-8]\\.svg")) {
            return name.substring(0, name.length() - 4) + ".png";
        }
        return name;
    }

    public File requireCustomAvatar(String storedName) {
        if (storedName == null || !storedName.matches(UUID_PATTERN)) {
            throw new ApiException(HttpStatus.NOT_FOUND, FileStorage.MISSING);
        }
        File file = new File(directory, storedName);
        try {
            String root = directory.getCanonicalPath();
            String path = file.getCanonicalPath();
            if (!path.startsWith(root + File.separator)) {
                throw new ApiException(HttpStatus.NOT_FOUND, FileStorage.MISSING);
            }
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, FileStorage.MISSING);
        }
        if (!file.isFile()) {
            throw new ApiException(HttpStatus.NOT_FOUND, FileStorage.MISSING);
        }
        return file;
    }

    private String normalizeNickname(String nickname) {
        String trimmed = nickname == null ? null : nickname.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 30) {
            throw new ApiException(HttpStatus.BAD_REQUEST, NICKNAME_TOO_LONG);
        }
        return trimmed;
    }

    private void deleteOldCustomAvatar(UserAccount account) {
        String old = account.getAvatar();
        if (old != null && old.startsWith(CUSTOM_PREFIX)) {
            String stored = old.substring(CUSTOM_PREFIX.length());
            new File(directory, stored).delete();
        }
    }

    private String save(MultipartFile file) {
        String stored = UUID.randomUUID().toString();
        File target = new File(directory, stored);
        try {
            Files.copy(file.getInputStream(), target.toPath());
        } catch (IOException ex) {
            target.delete();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "头像保存失败");
        }
        long length = target.length();
        if (length <= 0) {
            target.delete();
            throw new ApiException(HttpStatus.BAD_REQUEST, AVATAR_EMPTY);
        }
        if (length > MAX_AVATAR_BYTES) {
            target.delete();
            throw new ApiException(HttpStatus.BAD_REQUEST, AVATAR_TOO_BIG);
        }
        return stored;
    }
}
