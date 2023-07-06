/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.tracker.imports.programrule.executor.event;

import static org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue.error;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1301;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetMandatoryFieldExecutorTest extends DhisConvenienceTest {
  private static final String ACTIVE_EVENT_ID = "EventUid";

  private static final String COMPLETED_EVENT_ID = "CompletedEventUid";

  private static final String DATA_ELEMENT_ID = "DataElementId";

  private static final String DATA_ELEMENT_VALUE = "1.0";

  private static final String RULE_UID = "Rule uid";

  private static ProgramStage programStage;

  private static DataElement dataElement;

  private final SetMandatoryFieldExecutor executor =
      new SetMandatoryFieldExecutor(RULE_UID, DATA_ELEMENT_ID);

  private TrackerBundle bundle;

  @Mock private TrackerPreheat preheat;

  @BeforeEach
  void setUpTest() {
    programStage = createProgramStage('A', 0);
    programStage.setValidationStrategy(ValidationStrategy.ON_UPDATE_AND_INSERT);
    dataElement = createDataElement('A');
    dataElement.setUid(DATA_ELEMENT_ID);
    ProgramStageDataElement programStageDataElementA =
        createProgramStageDataElement(programStage, dataElement, 0);
    programStage.setProgramStageDataElements(Set.of(programStageDataElementA));

    bundle = TrackerBundle.builder().build();
    bundle.setPreheat(preheat);
  }

  @Test
  void shouldReturnNoErrorWhenMandatoryFieldIsPresentForEnrollment() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);
    bundle.setEvents(List.of(getEventWithMandatoryValueSet()));

    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, getEventWithMandatoryValueSet());

    assertTrue(error.isEmpty());
  }

  @Test
  void shouldReturnNoErrorWhenMandatoryFieldIsPresentForEnrollmentsUsingIdSchemeCode() {
    TrackerIdSchemeParams idSchemes =
        TrackerIdSchemeParams.builder().dataElementIdScheme(TrackerIdSchemeParam.CODE).build();
    when(preheat.getIdSchemes()).thenReturn(idSchemes);
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);
    bundle.setEvents(List.of(getEventWithMandatoryValueSet(idSchemes)));

    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, getEventWithMandatoryValueSet(idSchemes));

    assertTrue(error.isEmpty());
  }

  @Test
  void testValidateWithErrorMandatoryFieldsForEvents() {
    when(preheat.getIdSchemes()).thenReturn(TrackerIdSchemeParams.builder().build());
    when(preheat.getDataElement(DATA_ELEMENT_ID)).thenReturn(dataElement);
    when(preheat.getProgramStage(MetadataIdentifier.ofUid(programStage))).thenReturn(programStage);
    bundle.setEvents(List.of(getEventWithMandatoryValueSet(), getEventWithMandatoryValueNOTSet()));

    Optional<ProgramRuleIssue> error =
        executor.executeRuleAction(bundle, getEventWithMandatoryValueSet());
    assertTrue(error.isEmpty());

    error = executor.executeRuleAction(bundle, getEventWithMandatoryValueNOTSet());

    assertFalse(error.isEmpty());
    assertEquals(error(RULE_UID, E1301, dataElement.getUid()), error.get());
  }

  private Event getEventWithMandatoryValueSet(TrackerIdSchemeParams idSchemes) {
    return Event.builder()
        .event(ACTIVE_EVENT_ID)
        .status(EventStatus.ACTIVE)
        .programStage(idSchemes.toMetadataIdentifier(programStage))
        .dataValues(getActiveEventDataValues(idSchemes))
        .build();
  }

  private Event getEventWithMandatoryValueSet() {
    return Event.builder()
        .event(ACTIVE_EVENT_ID)
        .status(EventStatus.ACTIVE)
        .programStage(MetadataIdentifier.ofUid(programStage))
        .dataValues(getActiveEventDataValues())
        .build();
  }

  private Event getEventWithMandatoryValueNOTSet() {
    return Event.builder()
        .event(COMPLETED_EVENT_ID)
        .status(EventStatus.ACTIVE)
        .programStage(MetadataIdentifier.ofUid(programStage))
        .build();
  }

  private Set<DataValue> getActiveEventDataValues(TrackerIdSchemeParams idSchemes) {
    DataValue dataValue =
        DataValue.builder()
            .value(DATA_ELEMENT_VALUE)
            .dataElement(idSchemes.toMetadataIdentifier(dataElement))
            .build();
    return Set.of(dataValue);
  }

  private Set<DataValue> getActiveEventDataValues() {
    DataValue dataValue =
        DataValue.builder()
            .value(DATA_ELEMENT_VALUE)
            .dataElement(MetadataIdentifier.ofUid(DATA_ELEMENT_ID))
            .build();
    return Set.of(dataValue);
  }
}
