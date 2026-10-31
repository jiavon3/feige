package feige.service;

import feige.domain.AppSetting;
import feige.repo.AppSettingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThemeService {

    public static final String CLASSIC = "classic";
    public static final String SIGNAL = "signal";
    public static final String UNKNOWN = "主题不存在";
    private static final String KEY = "theme";

    private final AppSettingRepository settings;

    public ThemeService(AppSettingRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public String current() {
        return settings.findById(KEY).map(AppSetting::getSettingValue).orElse(CLASSIC);
    }

    @Transactional
    public String update(String theme) {
        if (!CLASSIC.equals(theme) && !SIGNAL.equals(theme)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, UNKNOWN);
        }
        AppSetting setting = settings.findById(KEY).orElseGet(AppSetting::new);
        setting.setSettingKey(KEY);
        setting.setSettingValue(theme);
        settings.save(setting);
        return theme;
    }
}
