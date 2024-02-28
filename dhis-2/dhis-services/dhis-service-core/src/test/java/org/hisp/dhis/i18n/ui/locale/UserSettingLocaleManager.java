package org.hisp.dhis.i18n.ui.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Locale;
import org.apache.commons.lang3.LocaleUtils;
import org.hisp.dhis.i18n.ui.resourcebundle.ResourceBundleManager;
import org.hisp.dhis.user.DefaultUserSettingService;
import org.hisp.dhis.user.UserSettingKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSettingLocaleManagerTest {

  @Mock private DefaultUserSettingService userSettingService;
  @Mock private ResourceBundleManager resourceBundleManager;
  @InjectMocks private UserSettingLocaleManager manager;

  @Test
  @DisplayName("When a user ui locale of 'id' is retrieved then a valid Locale of 'in' is returned")
  void testValidIdLocale() {
    // given
    String validLocale = "id";
    when(userSettingService.getUserSetting(UserSettingKey.UI_LOCALE)).thenReturn(validLocale);

    // when
    Locale currentLocale = manager.getCurrentLocale();

    // then
    assertNotNull(currentLocale);
    assertEquals(LocaleUtils.toLocale("in"), currentLocale);
  }

  @Test
  @DisplayName(
      "When a user ui locale of 'id_ID' is retrieved then a valid Locale of 'in_ID' is returned")
  void testValidIdLocale2() {
    // given
    String validLocale = "id_ID";
    when(userSettingService.getUserSetting(UserSettingKey.UI_LOCALE)).thenReturn(validLocale);

    // when
    Locale currentLocale = manager.getCurrentLocale();

    // then
    assertNotNull(currentLocale);
    assertEquals(LocaleUtils.toLocale("in_ID"), currentLocale);
  }

  @Test
  @DisplayName("When a user ui locale of 'en' is retrieved then a valid Locale of 'en' is returned")
  void testDefaultLocale() {
    // given
    Locale validLocale = LocaleUtils.toLocale("en");
    when(userSettingService.getUserSetting(UserSettingKey.UI_LOCALE)).thenReturn(validLocale);

    // when
    Locale currentLocale = manager.getCurrentLocale();

    // then
    assertNotNull(currentLocale);
    assertEquals(LocaleUtils.toLocale("en"), currentLocale);
  }
}
