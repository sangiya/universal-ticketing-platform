package com.ticketmesh.service;

import com.ticketmesh.model.UserSettings;
import com.ticketmesh.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-user preference settings. Settings rows are created lazily with the
 * platform defaults on first access and then updated in place.
 */
@Service
public class UserSettingsService {

    private final UserSettingsRepository settingsRepository;

    public UserSettingsService(UserSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Transactional
    public UserSettings getOrCreate(Long userId) {
        return settingsRepository.findByUserId(userId)
                .orElseGet(() -> settingsRepository.save(new UserSettings(userId)));
    }

    @Transactional
    public UserSettings update(Long userId, String theme, String language, String currency,
                               Boolean notifyEmail, Boolean notifySms, Boolean notifyPush,
                               Boolean notifyWhatsapp) {
        UserSettings settings = getOrCreate(userId);
        if (theme != null && !theme.isBlank()) {
            settings.setTheme(theme);
        }
        if (language != null && !language.isBlank()) {
            settings.setLanguage(language);
        }
        if (currency != null && !currency.isBlank()) {
            settings.setCurrency(currency);
        }
        if (notifyEmail != null) {
            settings.setNotifyEmail(notifyEmail);
        }
        if (notifySms != null) {
            settings.setNotifySms(notifySms);
        }
        if (notifyPush != null) {
            settings.setNotifyPush(notifyPush);
        }
        if (notifyWhatsapp != null) {
            settings.setNotifyWhatsapp(notifyWhatsapp);
        }
        settings.markUpdated();
        return settingsRepository.save(settings);
    }
}
