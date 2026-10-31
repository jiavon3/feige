package feige.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "app_settings")
public class AppSetting {

    @Id
    @Column(name = "setting_key", nullable = false, length = 32)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 32)
    private String settingValue;

    public AppSetting() {
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }
}
