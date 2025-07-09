/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge.dataelement.handler;

import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.merge.DataMergeStrategy;
import org.hisp.dhis.merge.MergeRequest;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventStore;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorStore;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementStore;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramStageSectionStore;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateStore;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionStore;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableStore;
import org.hisp.dhis.tracker.export.singleevent.SingleEventChangeLogService;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventChangeLogService;
import org.springframework.stereotype.Component;

/**
 * Merge handler for tracker entities.
 *
 * @author david mackessy
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackerDataElementMergeHandler {

  private final ProgramStageDataElementStore programStageDataElementStore;
  private final ProgramStageSectionStore programStageSectionStore;
  private final ProgramNotificationTemplateStore programNotificationTemplateStore;
  private final ProgramRuleVariableStore programRuleVariableStore;
  private final ProgramRuleActionStore programRuleActionStore;
  private final ProgramIndicatorStore programIndicatorStore;
  private final EventStore eventStore;
  private final TrackerEventChangeLogService trackerEventChangeLogService;
  private final SingleEventChangeLogService singleEventChangeLogService;

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
        programStageDataElementStore.getAllByDataElement(sources);
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
        programStageSectionStore.getAllByDataElement(sources);

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
        programNotificationTemplateStore.getByDataElement(sources);

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
        programRuleVariableStore.getByDataElement(sources);

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
    List<ProgramRuleAction> programRuleActions = programRuleActionStore.getByDataElement(sources);

    programRuleActions.forEach(pra -> pra.setDataElement(target));
  }

  /**
   * Method retrieving {@link Event}s by source {@link DataElement} references present in their
   * {@link EventDataValue}s. All retrieved {@link Event}s will have their {@link DataElement} ref
   * in {@link EventDataValue}s replaced with the target {@link DataElement}.
   *
   * <p>A native query to retrieve events is required here as Hibernate does not support json
   * functions in the query. Because of this, all events are then updated at the end of this method,
   * which should prevent inconsistent state between Hibernate/JPA
   *
   * @param sources source {@link DataElement}s used to retrieve {@link Event}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} in {@link Event}
   *     eventDataValues
   * @param request merge request
   */
  public void handleEventDataValues(
      List<DataElement> sources, DataElement target, MergeRequest request) {
    Set<UID> sourceDeUids = UID.of(sources.toArray(new DataElement[0]));
    DataMergeStrategy mergeStrategy = request.getDataMergeStrategy();

    if (DataMergeStrategy.DISCARD == mergeStrategy) {
      log.info(mergeStrategy + " dataMergeStrategy being used, deleting source event data values");
      eventStore.deleteEventDataValuesWithDataElement(sourceDeUids);
    } else if (DataMergeStrategy.LAST_UPDATED == mergeStrategy) {
      log.info(mergeStrategy + " dataMergeStrategy being used, merging source event data values");
      eventStore.mergeEventDataValuesWithDataElement(sourceDeUids, UID.of(target));
    }
  }

  /**
   * Method handling {@link EventChangeLog}s. They will either be deleted or left as is, based on
   * whether the source {@link DataElement}s are being deleted or not.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link DataValueAudit}s
   * @param mergeRequest merge request
   */
  public void handleEventChangeLogs(
      @Nonnull List<DataElement> sources, @Nonnull MergeRequest mergeRequest) {
    if (mergeRequest.isDeleteSources()) {
      log.info("Deleting source event change log records as source DataElements are being deleted");
      sources.forEach(trackerEventChangeLogService::deleteEventChangeLog);
      sources.forEach(singleEventChangeLogService::deleteEventChangeLog);
    } else {
      log.info(
          "Leaving source event change log records as is, source DataElements are not being deleted");
    }
  }
}
