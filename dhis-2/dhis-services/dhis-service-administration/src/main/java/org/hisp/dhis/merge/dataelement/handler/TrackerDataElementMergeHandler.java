/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge.dataelement.handler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventStore;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorStore;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramStageSectionService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.springframework.stereotype.Service;

/**
 * Merge handler for tracker entities.
 *
 * @author david mackessy
 */
@Service
@RequiredArgsConstructor
public class TrackerDataElementMergeHandler {

  private final ProgramStageDataElementService programStageDataElementService;
  private final ProgramStageSectionService programStageSectionService;
  private final ProgramNotificationTemplateService programNotificationTemplateService;
  private final ProgramRuleVariableService programRuleVariableService;
  private final ProgramRuleActionService programRuleActionService;
  private final ProgramIndicatorStore programIndicatorStore;
  private final EventStore eventStore;

  /**
   * Method retrieving {@link ProgramIndicator}s which have a source {@link DataElement} reference
   * in its expression. All retrieved {@link ProgramIndicator}s will have their expression {@link
   * DataElement} {@link UID} replaced with the target {@link DataElement} {@link UID}
   *
   * @param sources source {@link DataElement}s used to retrieve {@link ProgramIndicator}s
   * @param target {@link DataElement} which will replace the {@link DataElement} {@link UID} in a
   *     {@link ProgramIndicator} expression
   */
  public void handleProgramIndicatorExpression(List<DataElement> sources, DataElement target) {
    List<ProgramIndicator> programIndicators =
        programIndicatorStore.getAllWithExpressionContainingStrings(
            IdentifiableObjectUtils.getUidsNonNull(sources));
    for (DataElement source : sources) {
      programIndicators.forEach(
          pi -> pi.setExpression(pi.getExpression().replace(source.getUid(), target.getUid())));
    }
  }

  /**
   * Method retrieving {@link ProgramIndicator}s which have a source {@link DataElement} reference
   * in its filter. All retrieved {@link ProgramIndicator}s will have their filter {@link
   * DataElement} {@link UID} replaced with the target {@link DataElement} {@link UID}
   *
   * @param sources source {@link DataElement}s used to retrieve {@link ProgramIndicator}s
   * @param target {@link DataElement} which will replace the {@link DataElement} {@link UID} in a
   *     {@link ProgramIndicator} filter
   */
  public void handleProgramIndicatorFilter(List<DataElement> sources, DataElement target) {
    List<ProgramIndicator> programIndicators =
        programIndicatorStore.getAllWithFilterContainingStrings(
            IdentifiableObjectUtils.getUidsNonNull(sources));
    for (DataElement source : sources) {
      programIndicators.forEach(
          pi -> pi.setFilter(pi.getFilter().replace(source.getUid(), target.getUid())));
    }
  }

  /**
   * Method retrieving {@link ProgramStageDataElement}s by source {@link DataElement} references.
   * All retrieved {@link ProgramStageDataElement}s will have their {@link DataElement} replaced
   * with the target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link ProgramStageDataElement}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     ProgramStageDataElement}
   */
  public void handleProgramStageDataElement(List<DataElement> sources, DataElement target) {
    List<ProgramStageDataElement> programStageDataElements =
        programStageDataElementService.getAllByDataElement(sources);
    programStageDataElements.forEach(p -> p.setDataElement(target));
  }

  /**
   * Method retrieving {@link ProgramStageSection}s by source {@link DataElement} references. All
   * retrieved {@link ProgramStageSection}s will have their {@link DataElement}s replaced with the
   * target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link ProgramStageSection}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     ProgramStageSection}
   */
  public void handleProgramStageSection(List<DataElement> sources, DataElement target) {
    List<ProgramStageSection> programStageSections =
        programStageSectionService.getAllByDataElement(sources);

    sources.forEach(
        source ->
            programStageSections.forEach(
                pss -> {
                  pss.getDataElements().remove(source);
                  pss.getDataElements().add(target);
                }));
  }

  /**
   * Method retrieving {@link ProgramNotificationTemplate}s by source {@link DataElement}
   * references. All retrieved {@link ProgramNotificationTemplate}s will have their {@link
   * DataElement} replaced with the target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link
   *     ProgramNotificationTemplate}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     ProgramNotificationTemplate}
   */
  public void handleProgramNotificationTemplate(List<DataElement> sources, DataElement target) {
    List<ProgramNotificationTemplate> programNotificationTemplates =
        programNotificationTemplateService.getByDataElement(sources);

    programNotificationTemplates.forEach(pnt -> pnt.setRecipientDataElement(target));
  }

  /**
   * Method retrieving {@link ProgramRuleVariable}s by source {@link DataElement} references. All
   * retrieved {@link ProgramRuleVariable}s will have their {@link DataElement} replaced with the
   * target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link ProgramRuleVariable}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     ProgramRuleVariable}
   */
  public void handleProgramRuleVariable(List<DataElement> sources, DataElement target) {
    List<ProgramRuleVariable> programRuleVariables =
        programRuleVariableService.getByDataElement(sources);

    programRuleVariables.forEach(prv -> prv.setDataElement(target));
  }

  /**
   * Method retrieving {@link ProgramRuleAction}s by source {@link DataElement} references. All
   * retrieved {@link ProgramRuleAction}s will have their {@link DataElement} replaced with the
   * target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link ProgramRuleAction}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     ProgramRuleAction}
   */
  public void handleProgramRuleAction(List<DataElement> sources, DataElement target) {
    List<ProgramRuleAction> programRuleActions = programRuleActionService.getByDataElement(sources);

    programRuleActions.forEach(pra -> pra.setDataElement(target));
  }

  /**
   * Method retrieving {@link Event}s by source {@link DataElement} references present in their
   * eventDataValues property. All retrieved {@link Event}s will have their {@link DataElement} ref
   * in eventDataValues replaced with the target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link Event}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} in {@link Event}
   *     eventDataValues
   */
  public void handleEventEventDataValues(List<DataElement> sources, DataElement target) {
    List<String> sourceDeUids = IdentifiableObjectUtils.getUids(sources);
    List<Event> events = eventStore.getAllWithEventDataValuesRootKeysContainingAnyOf(sourceDeUids);
    events.stream()
        .flatMap(e -> e.getEventDataValues().stream())
        .filter(edv -> sourceDeUids.contains(edv.getDataElement()))
        .forEach(edv -> edv.setDataElement(target.getUid()));
  }
}
