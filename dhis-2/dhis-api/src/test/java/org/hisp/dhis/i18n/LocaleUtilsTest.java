/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import org.hisp.dhis.i18n.locale.LocaleUtils;
import org.junit.jupiter.api.Test;

class LocaleUtilsTest {

  @Test
  void testGetLocaleFallbacks() {
    Locale l1 = new Locale("en", "UK", "en");
    Locale l2 = new Locale("en", "UK");
    Locale l3 = new Locale("en");
    List<String> locales = LocaleUtils.getLocaleFallbacks(l1);
    assertEquals(3, locales.size());
    assertTrue(locales.contains("en_UK_en"));
    assertTrue(locales.contains("en_UK"));
    assertTrue(locales.contains("en_UK"));
    assertEquals(2, LocaleUtils.getLocaleFallbacks(l2).size());
    assertEquals(1, LocaleUtils.getLocaleFallbacks(l3).size());
  }

  @Test
  void testGetLocaleFallbacksWithScript() {
    // Cyrillic version
    Locale cyrlLocale =
        new Locale.Builder().setLanguage("uz").setRegion("UZ").setScript("Cyrl").build();

    List<String> cyrlFallbacks = LocaleUtils.getLocaleFallbacks(cyrlLocale);

    assertTrue(cyrlFallbacks.contains("uz"));
    assertTrue(cyrlFallbacks.contains("uz_UZ"));
    assertTrue(cyrlFallbacks.contains("uz_Cyrl"));
    assertTrue(cyrlFallbacks.contains("uz_UZ_Cyrl"));
    assertTrue(cyrlFallbacks.contains("uz_Cyrl_UZ"));

    // Latin version
    Locale latnLocale =
        new Locale.Builder().setLanguage("uz").setRegion("UZ").setScript("Latn").build();

    List<String> latnFallbacks = LocaleUtils.getLocaleFallbacks(latnLocale);

    assertTrue(latnFallbacks.contains("uz"));
    assertTrue(latnFallbacks.contains("uz_UZ"));
    assertTrue(latnFallbacks.contains("uz_Latn"));
    assertTrue(latnFallbacks.contains("uz_UZ_Latn"));
    assertTrue(latnFallbacks.contains("uz_Latn_UZ"));
  }

  @Test
  void testGetLocaleFallbacksForChineseScripts() {
    // Traditional Chinese (Taiwan)
    Locale zhHantTW =
        new Locale.Builder().setLanguage("zh").setRegion("TW").setScript("Hant").build();

    List<String> fallbacksHant = LocaleUtils.getLocaleFallbacks(zhHantTW);

    assertTrue(fallbacksHant.contains("zh"));
    assertTrue(fallbacksHant.contains("zh_TW"));
    assertTrue(fallbacksHant.contains("zh_Hant"));
    assertTrue(fallbacksHant.contains("zh_TW_Hant"));
    assertTrue(fallbacksHant.contains("zh_Hant_TW"));

    // Simplified Chinese (China)
    Locale zhHansCN =
        new Locale.Builder().setLanguage("zh").setRegion("CN").setScript("Hans").build();

    List<String> fallbacksHans = LocaleUtils.getLocaleFallbacks(zhHansCN);

    assertTrue(fallbacksHans.contains("zh"));
    assertTrue(fallbacksHans.contains("zh_CN"));
    assertTrue(fallbacksHans.contains("zh_Hans"));
    assertTrue(fallbacksHans.contains("zh_CN_Hans"));
    assertTrue(fallbacksHans.contains("zh_Hans_CN"));
  }

  @Test
  void testGetLocaleFallbacksForMongolianScripts() {
    // Mongolian Cyrillic (used in Mongolia)
    Locale mnCyrlMN =
        new Locale.Builder().setLanguage("mn").setRegion("MN").setScript("Cyrl").build();

    List<String> fallbacksCyrl = LocaleUtils.getLocaleFallbacks(mnCyrlMN);

    assertTrue(fallbacksCyrl.contains("mn"));
    assertTrue(fallbacksCyrl.contains("mn_MN"));
    assertTrue(fallbacksCyrl.contains("mn_Cyrl"));
    assertTrue(fallbacksCyrl.contains("mn_MN_Cyrl"));
    assertTrue(fallbacksCyrl.contains("mn_Cyrl_MN"));

    // Traditional Mongolian script (used in China)
    Locale mnMongCN =
        new Locale.Builder().setLanguage("mn").setRegion("CN").setScript("Mong").build();

    List<String> fallbacksMong = LocaleUtils.getLocaleFallbacks(mnMongCN);

    assertTrue(fallbacksMong.contains("mn"));
    assertTrue(fallbacksMong.contains("mn_CN"));
    assertTrue(fallbacksMong.contains("mn_Mong"));
    assertTrue(fallbacksMong.contains("mn_CN_Mong"));
    assertTrue(fallbacksMong.contains("mn_Mong_CN"));
  }
}
