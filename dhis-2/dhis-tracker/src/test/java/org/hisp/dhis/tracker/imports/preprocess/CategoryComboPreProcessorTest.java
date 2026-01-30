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
package org.hisp.dhis.tracker.imports.preprocess;

import static org.hisp.dhis.test.TestBase.createCategoryCombo;
import static org.hisp.dhis.test.TestBase.createCategoryOptionCombo;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Enrico Colasante
 */
class CategoryComboPreProcessorTest {

  private static final String PROGRAM_STAGE_WITH_REGISTRATION = "PROGRAM_STAGE_WITH_REGISTRATION";

  private static final String PROGRAM_WITH_REGISTRATION = "PROGRAM_WITH_REGISTRATION";

  private TrackerPreheat preheat;

  private CategoryComboPreProcessor preprocessor;

  @BeforeEach
  void setUp() {
    preheat = mock(TrackerPreheat.class);

    this.preprocessor = new CategoryComboPreProcessor();
  }

  @Test
  void testEventWithOnlyCOsIsEnhancedWithAOC() {

    TrackerIdSchemeParams identifierParams =
        TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Program program = createProgram('A');
    CategoryCombo categoryCombo = createCategoryCombo('A');
    program.setCategoryCombo(categoryCombo);
    Set<MetadataIdentifier> categoryOptions =
        Set.of(MetadataIdentifier.ofUid("123"), MetadataIdentifier.ofUid("235"));
    TrackerEvent event =
        completeTrackerEvent()
            .program(MetadataIdentifier.ofUid(program))
            .attributeCategoryOptions(categoryOptions)
            .build();
    when(preheat.getProgram(event.getProgram())).thenReturn(program);
    CategoryOptionCombo categoryOptionCombo = createCategoryOptionCombo('A');
    when(preheat.getCategoryOptionComboIdentifier(categoryCombo, categoryOptions))
        .thenReturn(identifierParams.toMetadataIdentifier(categoryOptionCombo));

    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackerEvents(Collections.singletonList(event))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    assertEquals(
        MetadataIdentifier.ofCode(categoryOptionCombo),
        bundle.getEvents().get(0).getAttributeOptionCombo());
    assertEquals(categoryOptions, bundle.getEvents().get(0).getAttributeCategoryOptions());
  }

  @Test
  void testEventWithOnlyCOsIsNotEnhancedWithAOCIfItCantBeFound() {

    TrackerIdSchemeParams identifierParams =
        TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Program program = createProgram('A');
    CategoryCombo categoryCombo = createCategoryCombo('A');
    program.setCategoryCombo(categoryCombo);
    TrackerEvent event =
        completeTrackerEvent()
            .program(MetadataIdentifier.ofUid(program))
            .attributeCategoryOptions(
                Set.of(MetadataIdentifier.ofUid("123"), MetadataIdentifier.ofUid("235")))
            .build();
    when(preheat.getProgram(event.getProgram())).thenReturn(program);
    when(preheat.getCategoryOptionComboIdentifier(
            categoryCombo, event.getAttributeCategoryOptions()))
        .thenReturn(MetadataIdentifier.EMPTY_CODE);

    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackerEvents(Collections.singletonList(event))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    assertEquals(
        MetadataIdentifier.EMPTY_CODE, bundle.getEvents().get(0).getAttributeOptionCombo());
    assertEquals(
        Set.of(MetadataIdentifier.ofUid("123"), MetadataIdentifier.ofUid("235")),
        bundle.getEvents().get(0).getAttributeCategoryOptions());
  }

  @Test
  void testEventWithOnlyCOsIsNotEnhancedWithAOCIfProgramCantBeFound() {

    TrackerIdSchemeParams identifierParams =
        TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Program program = createProgram('A');
    CategoryCombo categoryCombo = createCategoryCombo('A');
    program.setCategoryCombo(categoryCombo);
    TrackerEvent event =
        completeTrackerEvent()
            .program(MetadataIdentifier.ofUid(program))
            .attributeCategoryOptions(
                Set.of(MetadataIdentifier.ofUid("123"), MetadataIdentifier.ofUid("235")))
            .build();

    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackerEvents(Collections.singletonList(event))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    assertEquals(MetadataIdentifier.EMPTY_UID, bundle.getEvents().get(0).getAttributeOptionCombo());
    assertEquals(
        Set.of(MetadataIdentifier.ofUid("123"), MetadataIdentifier.ofUid("235")),
        bundle.getEvents().get(0).getAttributeCategoryOptions());
  }

  @Test
  void testEventWithAOCAndCOsIsNotEnhancedWithAOC() {

    TrackerIdSchemeParams identifierParams =
        TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Program program = createProgram('A');
    CategoryCombo categoryCombo = createCategoryCombo('A');
    program.setCategoryCombo(categoryCombo);
    TrackerEvent event =
        completeTrackerEvent()
            .program(MetadataIdentifier.ofUid(program))
            .attributeOptionCombo(MetadataIdentifier.ofCode("9871"))
            .attributeCategoryOptions(
                Set.of(MetadataIdentifier.ofUid("123"), MetadataIdentifier.ofUid("235")))
            .build();
    when(preheat.getProgram(event.getProgram())).thenReturn(program);

    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackerEvents(Collections.singletonList(event))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    assertEquals(
        MetadataIdentifier.ofCode("9871"), bundle.getEvents().get(0).getAttributeOptionCombo());
    assertEquals(
        Set.of(MetadataIdentifier.ofUid("123"), MetadataIdentifier.ofUid("235")),
        bundle.getEvents().get(0).getAttributeCategoryOptions());
  }

  @Test
  void testEventWithOnlyAOCIsLeftUnchanged() {

    TrackerIdSchemeParams identifierParams =
        TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Program program = createProgram('A');
    CategoryCombo categoryCombo = createCategoryCombo('A');
    program.setCategoryCombo(categoryCombo);
    TrackerEvent event =
        completeTrackerEvent()
            .program(MetadataIdentifier.ofUid(program))
            .attributeOptionCombo(MetadataIdentifier.ofCode("9871"))
            .build();
    when(preheat.getProgram(event.getProgram())).thenReturn(program);

    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackerEvents(Collections.singletonList(event))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    assertEquals(
        MetadataIdentifier.ofCode("9871"), bundle.getEvents().get(0).getAttributeOptionCombo());
  }

  @Test
  void testEventWithNoAOCAndNoCOsIsNotEnhancedWithAOC() {

    TrackerIdSchemeParams identifierParams =
        TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme(TrackerIdSchemeParam.CODE)
            .build();
    when(preheat.getIdSchemes()).thenReturn(identifierParams);

    Program program = createProgram('A');
    CategoryCombo categoryCombo = createCategoryCombo('A');
    program.setCategoryCombo(categoryCombo);
    TrackerEvent event = completeTrackerEvent().program(MetadataIdentifier.ofUid(program)).build();
    when(preheat.getProgram(event.getProgram())).thenReturn(program);

    TrackerBundle bundle =
        TrackerBundle.builder()
            .trackerEvents(Collections.singletonList(event))
            .preheat(preheat)
            .build();

    preprocessor.process(bundle);

    assertEquals(MetadataIdentifier.EMPTY_UID, bundle.getEvents().get(0).getAttributeOptionCombo());
    assertTrue(bundle.getEvents().get(0).getAttributeCategoryOptions().isEmpty());
  }

  private TrackerEvent.TrackerEventBuilder completeTrackerEvent() {
    return TrackerEvent.builder()
        .event(UID.generate())
        .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE_WITH_REGISTRATION))
        .program(MetadataIdentifier.ofUid(PROGRAM_WITH_REGISTRATION))
        .attributeOptionCombo(MetadataIdentifier.EMPTY_UID);
  }
}
