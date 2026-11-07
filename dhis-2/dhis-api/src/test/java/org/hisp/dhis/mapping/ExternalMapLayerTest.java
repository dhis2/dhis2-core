/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.mapping;

import static org.hisp.dhis.common.Locale.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.setting.ThreadUserSettings;
import org.hisp.dhis.translation.Translation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for the translated display properties of {@link ExternalMapLayer}. */
class ExternalMapLayerTest {

  private ExternalMapLayer layer;

  @BeforeEach
  void setUp() {
    ThreadUserSettings.clear();
    ThreadUserSettings.put(Map.of("keyDbLocale", Locale.FRENCH.toString()));

    layer = new ExternalMapLayer("Layer");
    layer.setAttribution("OpenStreetMap contributors");
    layer.setDescription("Base map layer");
  }

  @AfterEach
  void tearDown() {
    ThreadUserSettings.clear();
  }

  // -------------------------------------------------------------------------
  // getDisplayAttribution
  // -------------------------------------------------------------------------

  @Test
  void testGetDisplayAttributionWithoutTranslations() {
    assertEquals("OpenStreetMap contributors", layer.getDisplayAttribution());
  }

  @Test
  void testGetDisplayAttributionWithTranslationForCurrentLocale() {
    layer.setTranslations(
        Set.of(new Translation(of(Locale.FRENCH), "ATTRIBUTION", "Contributeurs OpenStreetMap")));

    assertEquals("Contributeurs OpenStreetMap", layer.getDisplayAttribution());
  }

  @Test
  void testGetDisplayAttributionWithTranslationForOtherLocaleOnly() {
    layer.setTranslations(
        Set.of(new Translation(of(Locale.GERMAN), "ATTRIBUTION", "OpenStreetMap-Mitwirkende")));

    assertEquals("OpenStreetMap contributors", layer.getDisplayAttribution());
  }

  @Test
  void testGetDisplayAttributionIgnoresTranslationOfOtherProperty() {
    layer.setTranslations(
        Set.of(new Translation(of(Locale.FRENCH), "DESCRIPTION", "Couche de fond de carte")));

    assertEquals("OpenStreetMap contributors", layer.getDisplayAttribution());
  }

  @Test
  void testGetDisplayAttributionWithEmptyTranslationValue() {
    layer.setTranslations(Set.of(new Translation(of(Locale.FRENCH), "ATTRIBUTION", "")));

    assertEquals("OpenStreetMap contributors", layer.getDisplayAttribution());
  }

  @Test
  void testGetDisplayAttributionTrimsDefaultWhenNoMatchingTranslation() {
    layer.setAttribution("  OpenStreetMap contributors  ");
    layer.setTranslations(
        Set.of(new Translation(of(Locale.GERMAN), "ATTRIBUTION", "OpenStreetMap-Mitwirkende")));

    assertEquals("OpenStreetMap contributors", layer.getDisplayAttribution());
  }

  @Test
  void testGetDisplayAttributionWhenAttributionIsNull() {
    layer.setAttribution(null);

    assertNull(layer.getDisplayAttribution());
  }

  @Test
  void testGetDisplayAttributionReflectsUpdatedTranslations() {
    layer.setTranslations(
        Set.of(new Translation(of(Locale.FRENCH), "ATTRIBUTION", "Contributeurs OpenStreetMap")));
    assertEquals("Contributeurs OpenStreetMap", layer.getDisplayAttribution());

    layer.setTranslations(
        Set.of(new Translation(of(Locale.FRENCH), "ATTRIBUTION", "Contributeurs")));
    assertEquals("Contributeurs", layer.getDisplayAttribution());
  }

  // -------------------------------------------------------------------------
  // getDisplayDescription
  // -------------------------------------------------------------------------

  @Test
  void testGetDisplayDescriptionWithoutTranslations() {
    assertEquals("Base map layer", layer.getDisplayDescription());
  }

  @Test
  void testGetDisplayDescriptionWithTranslationForCurrentLocale() {
    layer.setTranslations(
        Set.of(new Translation(of(Locale.FRENCH), "DESCRIPTION", "Couche de fond de carte")));

    assertEquals("Couche de fond de carte", layer.getDisplayDescription());
  }

  @Test
  void testGetDisplayDescriptionWithTranslationForOtherLocaleOnly() {
    layer.setTranslations(
        Set.of(new Translation(of(Locale.GERMAN), "DESCRIPTION", "Grundkartenebene")));

    assertEquals("Base map layer", layer.getDisplayDescription());
  }

  @Test
  void testGetDisplayDescriptionIgnoresTranslationOfOtherProperty() {
    layer.setTranslations(
        Set.of(new Translation(of(Locale.FRENCH), "ATTRIBUTION", "Contributeurs OpenStreetMap")));

    assertEquals("Base map layer", layer.getDisplayDescription());
  }

  @Test
  void testGetDisplayDescriptionWithEmptyTranslationValue() {
    layer.setTranslations(Set.of(new Translation(of(Locale.FRENCH), "DESCRIPTION", "")));

    assertEquals("Base map layer", layer.getDisplayDescription());
  }

  @Test
  void testGetDisplayDescriptionWhenDescriptionIsNull() {
    layer.setDescription(null);

    assertNull(layer.getDisplayDescription());
  }

  @Test
  void testDisplayAttributionAndDescriptionTranslatedIndependently() {
    layer.setTranslations(
        Set.of(
            new Translation(of(Locale.FRENCH), "ATTRIBUTION", "Contributeurs OpenStreetMap"),
            new Translation(of(Locale.FRENCH), "DESCRIPTION", "Couche de fond de carte")));

    assertEquals("Contributeurs OpenStreetMap", layer.getDisplayAttribution());
    assertEquals("Couche de fond de carte", layer.getDisplayDescription());
  }
}
