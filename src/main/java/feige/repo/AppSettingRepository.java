package feige.repo;

import feige.domain.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
