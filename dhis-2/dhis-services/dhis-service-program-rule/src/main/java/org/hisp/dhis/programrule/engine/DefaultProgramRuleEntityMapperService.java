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
package org.hisp.dhis.programrule.engine;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SHOWERROR;
import static org.hisp.dhis.rules.models.AttributeType.DATA_ELEMENT;
import static org.hisp.dhis.rules.models.AttributeType.TRACKED_ENTITY_ATTRIBUTE;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.rules.DataItem;
import org.hisp.dhis.rules.ItemValueType;
import org.hisp.dhis.rules.Option;
import org.hisp.dhis.rules.models.AttributeType;
import org.hisp.dhis.rules.models.Rule;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionAssign;
import org.hisp.dhis.rules.models.RuleActionCreateEvent;
import org.hisp.dhis.rules.models.RuleActionHideField;
import org.hisp.dhis.rules.models.RuleActionHideProgramStage;
import org.hisp.dhis.rules.models.RuleActionHideSection;
import org.hisp.dhis.rules.models.RuleActionMessage;
import org.hisp.dhis.rules.models.RuleActionScheduleMessage;
import org.hisp.dhis.rules.models.RuleActionSendMessage;
import org.hisp.dhis.rules.models.RuleActionSetMandatoryField;
import org.hisp.dhis.rules.models.RuleActionText;
import org.hisp.dhis.rules.models.RuleAttributeValue;
import org.hisp.dhis.rules.models.RuleDataValue;
import org.hisp.dhis.rules.models.RuleEnrollment;
import org.hisp.dhis.rules.models.RuleEvent;
import org.hisp.dhis.rules.models.RuleValueType;
import org.hisp.dhis.rules.models.RuleVariable;
import org.hisp.dhis.rules.models.RuleVariableAttribute;
import org.hisp.dhis.rules.models.RuleVariableCalculatedValue;
import org.hisp.dhis.rules.models.RuleVariableCurrentEvent;
import org.hisp.dhis.rules.models.RuleVariableNewestEvent;
import org.hisp.dhis.rules.models.RuleVariableNewestStageEvent;
import org.hisp.dhis.rules.models.RuleVariablePreviousEvent;
import org.hisp.dhis.rules.utils.RuleEngineUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import kotlinx.datetime.LocalDate;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Transactional(readOnly = true)
@Service("org.hisp.dhis.programrule.engine.ProgramRuleEntityMapperService")
public class DefaultProgramRuleEntityMapperService implements ProgramRuleEntityMapperService {
  private static final String LOCATION_FEEDBACK = "feedback";

  private static final String LOCATION_INDICATOR = "indicators";

  private final ImmutableMap<ProgramRuleActionType, Function<ProgramRuleAction, RuleAction>>
      ACTION_MAPPER =
          new ImmutableMap.Builder<ProgramRuleActionType, Function<ProgramRuleAction, RuleAction>>()
              .put(
                  ProgramRuleActionType.ASSIGN,
                  pra ->
                      RuleActionAssign.Companion.create(
                          pra.getContent(),
                          pra.getData(),
                          getAssignedParameter(pra),
                          getAttributeType(pra)))
              .put(
                  ProgramRuleActionType.CREATEEVENT,
                  pra ->
                      new RuleActionCreateEvent(pra.getLocation(), pra.getContent(), pra.getData()))
              .put(
                  ProgramRuleActionType.DISPLAYKEYVALUEPAIR,
                  this::getLocationBasedDisplayRuleAction)
              .put(ProgramRuleActionType.DISPLAYTEXT, this::getLocationBasedDisplayRuleAction)
              .put(
                  ProgramRuleActionType.HIDEFIELD,
                  pra ->
                      new RuleActionHideField(
                          getAssignedParameter(pra), pra.getContent(), getAttributeType(pra), ""))
              .put(
                  ProgramRuleActionType.HIDEPROGRAMSTAGE,
                  pra -> new RuleActionHideProgramStage(pra.getProgramStage().getUid(), ""))
              .put(
                  ProgramRuleActionType.HIDESECTION,
                  pra -> new RuleActionHideSection(pra.getProgramStageSection().getUid(), ""))
              .put(
                  SHOWERROR,
                  pra ->
                      RuleActionMessage.Companion.create(
                          pra.getContent(),
                          pra.getData(),
                          getAssignedParameter(pra),
                          getAttributeType(pra),
                          RuleActionMessage.Type.SHOW_ERROR))
              .put(
                  ProgramRuleActionType.SHOWWARNING,
                  pra ->
                      RuleActionMessage.Companion.create(
                          pra.getContent(),
                          pra.getData(),
                          getAssignedParameter(pra),
                          getAttributeType(pra),
                          RuleActionMessage.Type.SHOW_WARNING))
              .put(
                  ProgramRuleActionType.SETMANDATORYFIELD,
                  pra ->
                      new RuleActionSetMandatoryField(
                          getAssignedParameter(pra), getAttributeType(pra), ""))
              .put(
                  ProgramRuleActionType.WARNINGONCOMPLETE,
                  pra ->
                      RuleActionMessage.Companion.create(
                          pra.getContent(),
                          pra.getData(),
                          getAssignedParameter(pra),
                          getAttributeType(pra),
                          RuleActionMessage.Type.WARNING_ON_COMPILATION))
              .put(
                  ProgramRuleActionType.ERRORONCOMPLETE,
                  pra ->
                      RuleActionMessage.Companion.create(
                          pra.getContent(),
                          pra.getData(),
                          getAssignedParameter(pra),
                          getAttributeType(pra),
                          RuleActionMessage.Type.ERROR_ON_COMPILATION))
              .put(
                  ProgramRuleActionType.SENDMESSAGE,
                  pra -> new RuleActionSendMessage(pra.getTemplateUid(), pra.getData()))
              .put(
                  ProgramRuleActionType.SCHEDULEMESSAGE,
                  pra -> new RuleActionScheduleMessage(pra.getTemplateUid(), pra.getData()))
              .build();

  private final ImmutableMap<
          ProgramRuleVariableSourceType, Function<ProgramRuleVariable, RuleVariable>>
      VARIABLE_MAPPER =
          new ImmutableMap.Builder<
                  ProgramRuleVariableSourceType, Function<ProgramRuleVariable, RuleVariable>>()
              .put(
                  ProgramRuleVariableSourceType.CALCULATED_VALUE,
                  prv ->
                      new RuleVariableCalculatedValue(
                          prv.getName(),
                          prv.getUseCodeForOptionSet(),
                          List.of(),
                          prv.getUid(),
                          toMappedValueType(prv)))
              .put(
                  ProgramRuleVariableSourceType.TEI_ATTRIBUTE,
                  prv ->
                      new RuleVariableAttribute(
                          prv.getName(),
                          prv.getUseCodeForOptionSet(),
                          getOptions(prv),
                          prv.getAttribute().getUid(),
                          toMappedValueType(prv)))
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT,
                  prv ->
                      new RuleVariableCurrentEvent(
                          prv.getName(),
                          prv.getUseCodeForOptionSet(),
                          getOptions(prv),
                          prv.getDataElement().getUid(),
                          toMappedValueType(prv)))
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT,
                  prv ->
                      new RuleVariablePreviousEvent(
                          prv.getName(),
                          prv.getUseCodeForOptionSet(),
                          getOptions(prv),
                          prv.getDataElement().getUid(),
                          toMappedValueType(prv)))
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM,
                  prv ->
                      new RuleVariableNewestEvent(
                          prv.getName(),
                          prv.getUseCodeForOptionSet(),
                          getOptions(prv),
                          prv.getDataElement().getUid(),
                          toMappedValueType(prv)))
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE,
                  prv ->
                      new RuleVariableNewestStageEvent(
                          prv.getName(),
                          prv.getUseCodeForOptionSet(),
                          getOptions(prv),
                          prv.getDataElement().getUid(),
                          toMappedValueType(prv),
                          prv.getProgramStage().getUid()))
              .build();

  private final ImmutableMap<
          ProgramRuleVariableSourceType, Function<ProgramRuleVariable, ValueType>>
      VALUE_TYPE_MAPPER =
          new ImmutableMap.Builder<
                  ProgramRuleVariableSourceType, Function<ProgramRuleVariable, ValueType>>()
              .put(
                  ProgramRuleVariableSourceType.CALCULATED_VALUE, ProgramRuleVariable::getValueType)
              .put(
                  ProgramRuleVariableSourceType.TEI_ATTRIBUTE,
                  prv -> prv.getAttribute().getValueType())
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT,
                  prv -> prv.getDataElement().getValueType())
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT,
                  prv -> prv.getDataElement().getValueType())
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM,
                  prv -> prv.getDataElement().getValueType())
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE,
                  prv -> prv.getDataElement().getValueType())
              .build();

  private final ImmutableMap<ProgramRuleVariableSourceType, Function<ProgramRuleVariable, DataItem>>
      DESCRIPTION_MAPPER =
          new ImmutableMap.Builder<
                  ProgramRuleVariableSourceType, Function<ProgramRuleVariable, DataItem>>()
              .put(
                  ProgramRuleVariableSourceType.TEI_ATTRIBUTE,
                  prv -> {
                    TrackedEntityAttribute attribute = prv.getAttribute();

                    return new DataItem(
                        ObjectUtils.firstNonNull(
                            attribute.getDisplayName(),
                            attribute.getDisplayFormName(),
                            attribute.getName()),
                        getItemValueType(attribute.getValueType()));
                  })
              .put(
                  ProgramRuleVariableSourceType.CALCULATED_VALUE,
                  prv ->
                      new DataItem(
                          ObjectUtils.firstNonNull(prv.getDisplayName(), prv.getName()),
                          getItemValueType(prv.getValueType())))
              .put(ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT, this::getDisplayName)
              .put(ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT, this::getDisplayName)
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM,
                  this::getDisplayName)
              .put(
                  ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE,
                  this::getDisplayName)
              .build();

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final ProgramRuleService programRuleService;

  private final ProgramRuleVariableService programRuleVariableService;

  private final ConstantService constantService;

  private final I18nManager i18nManager;

  public DefaultProgramRuleEntityMapperService(
      ProgramRuleService programRuleService,
      ProgramRuleVariableService programRuleVariableService,
      ConstantService constantService,
      I18nManager i18nManager) {
    checkNotNull(programRuleService);
    checkNotNull(programRuleVariableService);
    checkNotNull(constantService);
    checkNotNull(i18nManager);

    this.programRuleService = programRuleService;
    this.programRuleVariableService = programRuleVariableService;
    this.constantService = constantService;
    this.i18nManager = i18nManager;
  }

  @Override
  public List<Rule> toMappedProgramRules() {
    List<ProgramRule> programRules = programRuleService.getAllProgramRule();

    return toMappedProgramRules(programRules);
  }

  @Override
  public List<Rule> toMappedProgramRules(List<ProgramRule> programRules) {
    return programRules.stream()
        .map(this::toRule)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public List<RuleVariable> toMappedProgramRuleVariables() {
    List<ProgramRuleVariable> programRuleVariables =
        programRuleVariableService.getAllProgramRuleVariable();

    return toMappedProgramRuleVariables(programRuleVariables);
  }

  @Override
  public List<RuleVariable> toMappedProgramRuleVariables(
      List<ProgramRuleVariable> programRuleVariables) {
    return programRuleVariables.stream()
        .filter(Objects::nonNull)
        .map(this::toRuleVariable)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public Map<String, DataItem> getItemStore(List<ProgramRuleVariable> programRuleVariables) {
    Map<String, DataItem> itemStore = new HashMap<>();

    // program rule variables
    programRuleVariables.forEach(
        prv ->
            itemStore.put(
                ObjectUtils.firstNonNull(prv.getName(), prv.getDisplayName()),
                DESCRIPTION_MAPPER.get(prv.getSourceType()).apply(prv)));

    // constants
    constantService
        .getAllConstants()
        .forEach(
            constant ->
                itemStore.put(
                    constant.getUid(),
                    new DataItem(
                        ObjectUtils.firstNonNull(
                            constant.getDisplayName(),
                            constant.getDisplayFormName(),
                            constant.getName()),
                        ItemValueType.NUMBER)));

    // program variables
    RuleEngineUtils.INSTANCE
        .getENV_VARIABLES()
        .entrySet()
        .forEach(
            var ->
                itemStore.put(
                    var.getKey(),
                    new DataItem(
                        ObjectUtils.firstNonNull(
                            i18nManager.getI18n().getString(var.getKey()), var.getKey()),
                        var.getValue())));

    return itemStore;
  }

  @Override
  public RuleEnrollment toMappedRuleEnrollment(
      Enrollment enrollment, List<TrackedEntityAttributeValue> trackedEntityAttributeValues) {
    if (enrollment == null) {
      return null;
    }

    String orgUnit = "";
    String orgUnitCode = "";

    if (enrollment.getOrganisationUnit() != null) {
      orgUnit = enrollment.getOrganisationUnit().getUid();
      orgUnitCode = enrollment.getOrganisationUnit().getCode();
    }

    List<RuleAttributeValue> ruleAttributeValues;

    if (enrollment.getTrackedEntity() != null) {
      ruleAttributeValues =
          enrollment.getTrackedEntity().getTrackedEntityAttributeValues().stream()
              .filter(Objects::nonNull)
              .map(
                  attr ->
                      new RuleAttributeValue(
                          attr.getAttribute().getUid(), getTrackedEntityAttributeValue(attr)))
              .collect(Collectors.toList());
    } else {
      ruleAttributeValues =
          trackedEntityAttributeValues.stream()
              .filter(Objects::nonNull)
              .map(
                  attr ->
                      new RuleAttributeValue(
                          attr.getAttribute().getUid(), getTrackedEntityAttributeValue(attr)))
              .collect(Collectors.toList());
    }
    return new RuleEnrollment(
        enrollment.getUid(),
        enrollment.getProgram().getName(),
        LocalDate.Companion.parse(enrollment.getOccurredDate().toString()),
        LocalDate.Companion.parse(enrollment.getEnrollmentDate().toString()),
        RuleEnrollment.Status.valueOf(enrollment.getStatus().toString()),
        orgUnit,
        orgUnitCode,
        ruleAttributeValues);
  }

  @Override
  public List<RuleEvent> toMappedRuleEvents(Set<Event> events, Event eventToEvaluate) {
    return events.stream()
        .filter(Objects::nonNull)
        .filter(
            event -> !(eventToEvaluate != null && event.getUid().equals(eventToEvaluate.getUid())))
        .map(this::toMappedRuleEvent)
        .collect(Collectors.toList());
  }

  @Override
  public RuleEvent toMappedRuleEvent(Event eventToEvaluate) {
    if (eventToEvaluate == null) {
      return null;
    }

    String orgUnit = getOrgUnit(eventToEvaluate);
    String orgUnitCode = getOrgUnitCode(eventToEvaluate);

    return new RuleEvent(
        eventToEvaluate.getUid(),
        eventToEvaluate.getProgramStage().getUid(),
        eventToEvaluate.getProgramStage().getName(),
        RuleEvent.Status.valueOf(eventToEvaluate.getStatus().toString()),
            eventToEvaluate.getOccurredDate() == null ?
            LocalDate.Companion.parse(eventToEvaluate.getOccurredDate().toString()) :
                    LocalDate.Companion.parse(eventToEvaluate.getScheduledDate().toString()),
            LocalDate.Companion.parse(eventToEvaluate.getScheduledDate().toString()),
                    LocalDate.Companion.parse(eventToEvaluate.getCompletedDate().toString()),
        orgUnit,
        orgUnitCode,
        eventToEvaluate.getEventDataValues().stream()
            .filter(Objects::nonNull)
            .filter(dv -> dv.getValue() != null)
            .map(
                dv ->
                    new RuleDataValue(
                        eventToEvaluate.getOccurredDate() == null ?
                        LocalDate.Companion.parse(eventToEvaluate.getOccurredDate().toString()) :
                        LocalDate.Companion.parse(eventToEvaluate.getScheduledDate().toString()),
                        eventToEvaluate.getProgramStage().getUid(),
                        dv.getDataElement(),
                        dv.getValue()))
            .toList());
  }

  // ---------------------------------------------------------------------
  // Supportive Methods
  // ---------------------------------------------------------------------

  private String getOrgUnit(Event event) {
    if (event.getOrganisationUnit() != null) {
      return event.getOrganisationUnit().getUid();
    }

    return "";
  }

  private String getOrgUnitCode(Event event) {
    if (event.getOrganisationUnit() != null) {
      return event.getOrganisationUnit().getCode();
    }

    return "";
  }

  private Rule toRule(ProgramRule programRule) {
    if (programRule == null) {
      return null;
    }

    Set<ProgramRuleAction> programRuleActions = programRule.getProgramRuleActions();

    List<RuleAction> ruleActions;

    Rule rule;
    try {
      ruleActions = programRuleActions.stream().map(this::toRuleAction).toList();
      rule =
          new Rule(
              programRule.getCondition(),
              ruleActions,
              programRule.getUid(),
              programRule.getName(),
              programRule.getProgramStage() != null
                  ? programRule.getProgramStage().getUid()
                  : StringUtils.EMPTY,
              programRule.getPriority());

    } catch (Exception e) {
      log.debug("Invalid rule action in ProgramRule: " + programRule.getUid());

      return null;
    }

    return rule;
  }

  private RuleAction toRuleAction(ProgramRuleAction programRuleAction) {
    return ACTION_MAPPER
        .getOrDefault(
            programRuleAction.getProgramRuleActionType(),
            pra ->
                RuleActionAssign.Companion.create(
                    pra.getContent(), pra.getData(), getAssignedParameter(pra)))
        .apply(programRuleAction);
  }

  private RuleVariable toRuleVariable(ProgramRuleVariable programRuleVariable) {
    RuleVariable ruleVariable = null;

    try {
      if (VARIABLE_MAPPER.containsKey(programRuleVariable.getSourceType())) {
        ruleVariable =
            VARIABLE_MAPPER.get(programRuleVariable.getSourceType()).apply(programRuleVariable);
      }
    } catch (Exception e) {
      log.debug("Invalid ProgramRuleVariable: " + programRuleVariable.getUid());
    }

    return ruleVariable;
  }

  private RuleValueType toMappedValueType(ProgramRuleVariable programRuleVariable) {
    ValueType valueType =
        VALUE_TYPE_MAPPER
            .getOrDefault(programRuleVariable.getSourceType(), prv -> ValueType.TEXT)
            .apply(programRuleVariable);

    if (valueType.isBoolean()) {
      return RuleValueType.BOOLEAN;
    }

    if (valueType.isNumeric()) {
      return RuleValueType.NUMERIC;
    }

    if (valueType.isDate()) {
      return RuleValueType.DATE;
    }

    return RuleValueType.TEXT;
  }

  private AttributeType getAttributeType(ProgramRuleAction programRuleAction) {
    if (programRuleAction.hasDataElement()) {
      return DATA_ELEMENT;
    }

    if (programRuleAction.hasTrackedEntityAttribute()) {
      return TRACKED_ENTITY_ATTRIBUTE;
    }

    return AttributeType.UNKNOWN;
  }

  private String getAssignedParameter(ProgramRuleAction programRuleAction) {
    if (programRuleAction.hasDataElement()) {
      return programRuleAction.getDataElement().getUid();
    }

    if (programRuleAction.hasTrackedEntityAttribute()) {
      return programRuleAction.getAttribute().getUid();
    }

    if (programRuleAction.hasContent()) {
      return StringUtils.EMPTY;
    }

    log.warn(
        String.format(
            "No location found for ProgramRuleAction: %s in ProgramRule: %s",
            programRuleAction.getProgramRuleActionType(),
            programRuleAction.getProgramRule().getUid()));

    return StringUtils.EMPTY;
  }

  private RuleAction getLocationBasedDisplayRuleAction(ProgramRuleAction programRuleAction) {
    if (ProgramRuleActionType.DISPLAYTEXT.equals(programRuleAction.getProgramRuleActionType())) {
      if (LOCATION_FEEDBACK.equals(programRuleAction.getLocation())) {
        return RuleActionText.Companion.createForFeedback(
            RuleActionText.Type.DISPLAYTEXT,
            programRuleAction.getContent(),
            programRuleAction.getData());
      }

      if (LOCATION_INDICATOR.equals(programRuleAction.getLocation())) {
        return RuleActionText.Companion.createForIndicators(
            RuleActionText.Type.DISPLAYTEXT,
            programRuleAction.getContent(),
            programRuleAction.getData());
      }

      return RuleActionText.Companion.createForFeedback(
          RuleActionText.Type.DISPLAYTEXT,
          programRuleAction.getContent(),
          programRuleAction.getData());
    } else {
      if (LOCATION_FEEDBACK.equals(programRuleAction.getLocation())) {
        return RuleActionText.Companion.createForFeedback(
            RuleActionText.Type.DISPLAYKEYVALUEPAIR,
            programRuleAction.getContent(),
            programRuleAction.getData());
      }

      if (LOCATION_INDICATOR.equals(programRuleAction.getLocation())) {
        return RuleActionText.Companion.createForIndicators(
            RuleActionText.Type.DISPLAYKEYVALUEPAIR,
            programRuleAction.getContent(),
            programRuleAction.getData());
      }

      return RuleActionText.Companion.createForFeedback(
          RuleActionText.Type.DISPLAYKEYVALUEPAIR,
          programRuleAction.getContent(),
          programRuleAction.getData());
    }
  }

  private String getTrackedEntityAttributeValue(TrackedEntityAttributeValue attributeValue) {
    ValueType valueType = attributeValue.getAttribute().getValueType();

    if (valueType.isBoolean()) {
      return attributeValue.getValue() != null ? attributeValue.getValue() : "false";
    }

    if (valueType.isNumeric()) {
      return attributeValue.getValue() != null ? attributeValue.getValue() : "0";
    }

    return attributeValue.getValue() != null ? attributeValue.getValue() : "";
  }

  private ItemValueType getItemValueType(ValueType valueType) {
    if (valueType.isDate()) {
      return ItemValueType.DATE;
    }

    if (valueType.isNumeric()) {
      return ItemValueType.NUMBER;
    }

    if (valueType.isBoolean()) {
      return ItemValueType.BOOLEAN;
    }

    // default
    return ItemValueType.TEXT;
  }

  private DataItem getDisplayName(ProgramRuleVariable prv) {
    DataElement dataElement = prv.getDataElement();

    return new DataItem(
        ObjectUtils.firstNonNull(
            dataElement.getDisplayFormName(), dataElement.getFormName(), dataElement.getName()),
        getItemValueType(dataElement.getValueType()));
  }

  private List<Option> getOptions(ProgramRuleVariable prv) {
    if (prv.getUseCodeForOptionSet()) {
      return List.of();
    }

    if (prv.hasDataElement() && prv.getDataElement().hasOptionSet()) {
      return prv.getDataElement().getOptionSet().getOptions().stream()
          .map(op -> new Option(op.getName(), op.getCode()))
          .toList();
    } else if (prv.hasTrackedEntityAttribute() && prv.getAttribute().hasOptionSet()) {
      return prv.getAttribute().getOptionSet().getOptions().stream()
          .map(op -> new Option(op.getName(), op.getCode()))
          .toList();
    }

    return List.of();
  }
}
