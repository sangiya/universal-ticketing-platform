package com.ticketmesh.service;

import com.ticketmesh.model.UserSettings;
import com.ticketmesh.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSettingsServiceTest {

    private UserSettingsRepository settingsRepository;
    private UserSettingsService settingsService;

    @BeforeEach
    void setUp() {
        settingsRepository = mock(UserSettingsRepository.class);
        settingsService = new UserSettingsService(settingsRepository);
        when(settingsRepository.save(any(UserSettings.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getOrCreate_createsWithDefaultsWhenMissing() {
        when(settingsRepository.findByUserId(5L)).thenReturn(Optional.empty());

        UserSettings settings = settingsService.getOrCreate(5L);

        assertEquals(5L, settings.getUserId());
        assertEquals("LIGHT", settings.getTheme());
        assertEquals("en", settings.getLanguage());
        assertEquals("LKR", settings.getCurrency());
        assertTrue(settings.isNotifyEmail());
        assertFalse(settings.isNotifySms());
        assertTrue(settings.isNotifyPush());
        assertFalse(settings.isNotifyWhatsapp());
        verify(settingsRepository).save(any(UserSettings.class));
    }

    @Test
    void getOrCreate_returnsExistingWhenPresent() {
        UserSettings existing = new UserSettings(5L);
        when(settingsRepository.findByUserId(5L)).thenReturn(Optional.of(existing));

        UserSettings settings = settingsService.getOrCreate(5L);

        assertEquals(existing, settings);
        verify(settingsRepository, org.mockito.Mockito.never())
                .save(any(UserSettings.class));
    }

    @Test
    void update_persistsChanges() {
        UserSettings existing = new UserSettings(5L);
        when(settingsRepository.findByUserId(5L)).thenReturn(Optional.of(existing));

        UserSettings updated = settingsService.update(
                5L, "DARK", "si", "USD", false, true, false, true);

        assertEquals("DARK", updated.getTheme());
        assertEquals("si", updated.getLanguage());
        assertEquals("USD", updated.getCurrency());
        assertFalse(updated.isNotifyEmail());
        assertTrue(updated.isNotifySms());
        assertFalse(updated.isNotifyPush());
        assertTrue(updated.isNotifyWhatsapp());
        verify(settingsRepository).save(existing);
    }

    @Test
    void update_preservesDefaultsWhenFieldsAbsent() {
        UserSettings existing = new UserSettings(5L);
        when(settingsRepository.findByUserId(5L)).thenReturn(Optional.of(existing));

        UserSettings updated = settingsService.update(5L, null, null, null, null, null, null, null);

        assertEquals("LIGHT", updated.getTheme());
        assertEquals("en", updated.getLanguage());
        assertEquals("LKR", updated.getCurrency());
        verify(settingsRepository).save(existing);
    }
}
