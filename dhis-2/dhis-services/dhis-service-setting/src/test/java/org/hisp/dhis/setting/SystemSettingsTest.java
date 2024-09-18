package org.hisp.dhis.setting;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemSettingsTest {

  @Test
  void testIsConfidential() {
    List<String> confidential = List.of("keyEmailPassword", "keyRemoteInstancePassword", "recaptchaSite", "recaptchaSecret");
    confidential.forEach( key ->
    assertTrue( SystemSettings.isConfidential(key)));

    assertFalse(SystemSettings.isConfidential("keyEmailHostName"));
  }
}
