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
package org.hisp.dhis.merge.dataelement;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationService;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.predictor.PredictorService;
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
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.springframework.stereotype.Service;

/**
 * Merge handler for metadata entities.
 *
 * @author david mackessy
 */
@Service
@RequiredArgsConstructor
public class DefaultDataElementMergeHandler {

  private final MinMaxDataElementService minMaxDataElementService;
  private final EventVisualizationService eventVisualizationService;
  private final SMSCommandService smsCommandService;
  private final PredictorService predictorService;
  private final ProgramStageDataElementService programStageDataElementService;
  private final ProgramStageSectionService programStageSectionService;
  private final ProgramNotificationTemplateService programNotificationTemplateService;
  private final ProgramRuleVariableService programRuleVariableService;
  private final ProgramRuleActionService programRuleActionService;

  /**
   * Method retrieving {@link MinMaxDataElement}s by source {@link DataElement} references. All
   * retrieved {@link MinMaxDataElement}s will have their {@link DataElement} replaced with the
   * target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link MinMaxDataElement}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     MinMaxDataElement}
   */
  public void handleMinMaxDataElement(List<DataElement> sources, DataElement target) {
    List<MinMaxDataElement> minMaxDataElements =
        minMaxDataElementService.getAllByDataElement(sources);
    minMaxDataElements.forEach(mmde -> mmde.setDataElement(target));
  }

  /**
   * Method retrieving {@link EventVisualization}s by source {@link DataElement} references. All
   * retrieved {@link EventVisualization}s will have their {@link DataElement} replaced with the
   * target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link EventVisualization}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     EventVisualization}
   */
  public void handleEventVisualization(List<DataElement> sources, DataElement target) {
    List<EventVisualization> eventVisualizations =
        eventVisualizationService.getAllByDataElement(sources);
    eventVisualizations.forEach(ev -> ev.setDataElementValueDimension(target));
  }

  /**
   * Method retrieving {@link SMSCode}s by source {@link DataElement} references. All retrieved
   * {@link SMSCode}s will have their {@link DataElement} replaced with the target {@link
   * DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link SMSCode}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     SMSCode}
   */
  public void handleSmsCode(List<DataElement> sources, DataElement target) {
    List<SMSCode> smsCodes = smsCommandService.getSmsCodesByDataElement(sources);
    smsCodes.forEach(c -> c.setDataElement(target));
  }

  /**
   * Method retrieving {@link org.hisp.dhis.predictor.Predictor}s by source {@link DataElement}
   * references. All retrieved {@link org.hisp.dhis.predictor.Predictor}s will have their {@link
   * DataElement} replaced with the target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link
   *     org.hisp.dhis.predictor.Predictor}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     org.hisp.dhis.predictor.Predictor}
   */
  public void handlePredictor(List<DataElement> sources, DataElement target) {
    List<Predictor> predictors = predictorService.getAllByDataElement(sources);
    predictors.forEach(p -> p.setOutput(target));
  }

  /**
   * Method retrieving {@link org.hisp.dhis.program.ProgramStageDataElement}s by source {@link
   * DataElement} references. All retrieved {@link org.hisp.dhis.program.ProgramStageDataElement}s
   * will have their {@link DataElement} replaced with the target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link
   *     org.hisp.dhis.program.ProgramStageDataElement}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     org.hisp.dhis.program.ProgramStageDataElement}
   */
  public void handleProgramStageDataElement(List<DataElement> sources, DataElement target) {
    List<ProgramStageDataElement> programStageDataElements =
        programStageDataElementService.getAllByDataElement(sources);
    programStageDataElements.forEach(p -> p.setDataElement(target));
  }

  /**
   * Method retrieving {@link org.hisp.dhis.program.ProgramStageSection}s by source {@link
   * DataElement} references. All retrieved {@link org.hisp.dhis.program.ProgramStageSection}s will
   * have their {@link DataElement}s replaced with the target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link
   *     org.hisp.dhis.program.ProgramStageSection}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     org.hisp.dhis.program.ProgramStageSection}
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
}
