/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.system.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
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
}
