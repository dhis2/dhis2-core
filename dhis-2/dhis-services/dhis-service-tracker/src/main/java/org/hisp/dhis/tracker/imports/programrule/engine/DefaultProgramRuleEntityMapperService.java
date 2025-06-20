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
package org.hisp.dhis.tracker.imports.programrule.engine;

import static org.hisp.dhis.programrule.ProgramRuleActionType.ASSIGN;
import static org.hisp.dhis.programrule.ProgramRuleActionType.ERRORONCOMPLETE;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SCHEDULEEVENT;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SCHEDULEMESSAGE;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SENDMESSAGE;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SETMANDATORYFIELD;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SHOWERROR;
import static org.hisp.dhis.programrule.ProgramRuleActionType.SHOWWARNING;
import static org.hisp.dhis.programrule.ProgramRuleActionType.WARNINGONCOMPLETE;
import static org.hisp.dhis.rules.models.AttributeType.DATA_ELEMENT;
import static org.hisp.dhis.rules.models.AttributeType.TRACKED_ENTITY_ATTRIBUTE;
import static org.hisp.dhis.tracker.imports.programrule.engine.RuleActionKey.ATTRIBUTE_TYPE;
import static org.hisp.dhis.tracker.imports.programrule.engine.RuleActionKey.CONTENT;
import static org.hisp.dhis.tracker.imports.programrule.engine.RuleActionKey.FIELD;
import static org.hisp.dhis.tracker.imports.programrule.engine.RuleActionKey.NOTIFICATION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.rules.api.DataItem;
import org.hisp.dhis.rules.api.EnvironmentVariables;
import org.hisp.dhis.rules.api.ItemValueType;
import org.hisp.dhis.rules.models.AttributeType;
import org.hisp.dhis.rules.models.Option;
import org.hisp.dhis.rules.models.Rule;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleValueType;
import org.hisp.dhis.rules.models.RuleVariable;
import org.hisp.dhis.rules.models.RuleVariableAttribute;
import org.hisp.dhis.rules.models.RuleVariableCalculatedValue;
import org.hisp.dhis.rules.models.RuleVariableCurrentEvent;
import org.hisp.dhis.rules.models.RuleVariableNewestEvent;
import org.hisp.dhis.rules.models.RuleVariableNewestStageEvent;
import org.hisp.dhis.rules.models.RuleVariablePreviousEvent;
import org.springframework.stereotype.Service;

/**
 * @author Zubair Asghar
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.tracker.imports.programrule.engine.ProgramRuleEntityMapperService")
public class DefaultProgramRuleEntityMapperService implements ProgramRuleEntityMapperService {
  private final I18nManager i18nManager;

  @Override
  public List<Rule> toRules(@Nonnull List<ProgramRule> programRules) {
    return programRules.stream()
        .map(this::toRule)
        .filter(rule -> !rule.getActions().isEmpty())
        .toList();
  }

  @Override
  public List<RuleVariable> toRuleVariables(
      @Nonnull List<ProgramRuleVariable> programRuleVariables) {
    return programRuleVariables.stream()
        .filter(Objects::nonNull)
        .map(this::toRuleVariable)
        .toList();
  }

  @Override
  public Map<String, DataItem> getItemStore(
      @Nonnull List<ProgramRuleVariable> programRuleVariables, @Nonnull List<Constant> constants) {
    Map<String, DataItem> itemStore = new HashMap<>();

    // program rule variables
    programRuleVariables.forEach(
        prv ->
            itemStore.put(
                ObjectUtils.firstNonNull(prv.getName(), prv.getDisplayName()),
                getDescription(prv)));

    constants.forEach(
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
    EnvironmentVariables.INSTANCE
        .getENV_VARIABLES()
        .forEach(
            (key, value) ->
                itemStore.put(
                    key,
                    new DataItem(
                        ObjectUtils.firstNonNull(i18nManager.getI18n().getString(key), key),
                        value)));

    return itemStore;
  }

  private Rule toRule(@Nonnull ProgramRule programRule) {
    try {
      List<RuleAction> ruleActions =
          programRule.getProgramRuleActions().stream()
              .map(this::toRuleAction)
              .filter(Objects::nonNull)
              .toList();

      return new Rule(
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
  }

  private RuleAction toRuleAction(@Nonnull ProgramRuleAction pra) {
    return switch (pra.getProgramRuleActionType()) {
      case ASSIGN ->
          new RuleAction(
              pra.getData(),
              ASSIGN.name(),
              createValues(
                  CONTENT,
                  pra.getContent(),
                  FIELD,
                  getAssignedParameter(pra),
                  ATTRIBUTE_TYPE,
                  getAttributeType(pra).name()));
      case SCHEDULEEVENT ->
          new RuleAction(
              pra.getData(),
              SCHEDULEEVENT.name(),
              createValues(CONTENT, pra.getContent(), FIELD, pra.getProgramStage().getUid()));
      case SHOWWARNING ->
          new RuleAction(
              pra.getData(),
              SHOWWARNING.name(),
              createValues(
                  CONTENT,
                  pra.getContent(),
                  FIELD,
                  getAssignedParameter(pra),
                  ATTRIBUTE_TYPE,
                  getAttributeType(pra).name()));
      case WARNINGONCOMPLETE ->
          new RuleAction(
              pra.getData(),
              WARNINGONCOMPLETE.name(),
              createValues(
                  CONTENT,
                  pra.getContent(),
                  FIELD,
                  getAssignedParameter(pra),
                  ATTRIBUTE_TYPE,
                  getAttributeType(pra).name()));
      case SHOWERROR ->
          new RuleAction(
              pra.getData(),
              SHOWERROR.name(),
              createValues(
                  CONTENT,
                  pra.getContent(),
                  FIELD,
                  getAssignedParameter(pra),
                  ATTRIBUTE_TYPE,
                  getAttributeType(pra).name()));
      case ERRORONCOMPLETE ->
          new RuleAction(
              pra.getData(),
              ERRORONCOMPLETE.name(),
              createValues(
                  CONTENT,
                  pra.getContent(),
                  FIELD,
                  getAssignedParameter(pra),
                  ATTRIBUTE_TYPE,
                  getAttributeType(pra).name()));
      case SETMANDATORYFIELD ->
          new RuleAction(
              null,
              SETMANDATORYFIELD.name(),
              createValues(
                  FIELD, getAssignedParameter(pra), ATTRIBUTE_TYPE, getAttributeType(pra).name()));
      case SENDMESSAGE ->
          new RuleAction(
              null, SENDMESSAGE.name(), createValues(NOTIFICATION, pra.getTemplateUid()));
      case SCHEDULEMESSAGE ->
          new RuleAction(
              pra.getData(),
              SCHEDULEMESSAGE.name(),
              createValues(NOTIFICATION, pra.getTemplateUid()));
      default -> null;
    };
  }

  private Map<String, String> createValues(@Nonnull String key1, @CheckForNull String value1) {
    Map<String, String> values = new HashMap<>();

    if (value1 != null) {
      values.put(key1, value1);
    }

    return values;
  }

  private Map<String, String> createValues(
      @Nonnull String key1,
      @CheckForNull String value1,
      @Nonnull String key2,
      @CheckForNull String value2) {
    Map<String, String> values = createValues(key1, value1);

    if (value2 != null) {
      values.put(key2, value2);
    }

    return values;
  }

  private Map<String, String> createValues(
      @Nonnull String key1,
      @CheckForNull String value1,
      @Nonnull String key2,
      @CheckForNull String value2,
      @Nonnull String key3,
      @CheckForNull String value3) {
    Map<String, String> values = createValues(key1, value1, key2, value2);

    if (value3 != null) {
      values.put(key3, value3);
    }

    return values;
  }

  private RuleVariable toRuleVariable(ProgramRuleVariable prv) {
    return switch (prv.getSourceType()) {
      case DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE ->
          new RuleVariableNewestStageEvent(
              prv.getName(),
              prv.getUseCodeForOptionSet(),
              getOptions(prv),
              prv.getDataElement().getUid(),
              toMappedValueType(prv),
              prv.getProgramStage().getUid());
      case DATAELEMENT_NEWEST_EVENT_PROGRAM ->
          new RuleVariableNewestEvent(
              prv.getName(),
              prv.getUseCodeForOptionSet(),
              getOptions(prv),
              prv.getDataElement().getUid(),
              toMappedValueType(prv));
      case DATAELEMENT_CURRENT_EVENT ->
          new RuleVariableCurrentEvent(
              prv.getName(),
              prv.getUseCodeForOptionSet(),
              getOptions(prv),
              prv.getDataElement().getUid(),
              toMappedValueType(prv));
      case DATAELEMENT_PREVIOUS_EVENT ->
          new RuleVariablePreviousEvent(
              prv.getName(),
              prv.getUseCodeForOptionSet(),
              getOptions(prv),
              prv.getDataElement().getUid(),
              toMappedValueType(prv));
      case CALCULATED_VALUE ->
          new RuleVariableCalculatedValue(
              prv.getName(),
              prv.getUseCodeForOptionSet(),
              List.of(),
              prv.getUid(),
              toMappedValueType(prv));
      case TEI_ATTRIBUTE ->
          new RuleVariableAttribute(
              prv.getName(),
              prv.getUseCodeForOptionSet(),
              getOptions(prv),
              prv.getAttribute().getUid(),
              toMappedValueType(prv));
    };
  }

  private RuleValueType toMappedValueType(ProgramRuleVariable programRuleVariable) {
    ValueType valueType =
        switch (programRuleVariable.getSourceType()) {
          case DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE,
              DATAELEMENT_PREVIOUS_EVENT,
              DATAELEMENT_NEWEST_EVENT_PROGRAM,
              DATAELEMENT_CURRENT_EVENT ->
              programRuleVariable.getDataElement().getValueType();
          case CALCULATED_VALUE -> programRuleVariable.getValueType();
          case TEI_ATTRIBUTE -> programRuleVariable.getAttribute().getValueType();
        };

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

  private DataItem getDescription(ProgramRuleVariable prv) {
    return switch (prv.getSourceType()) {
      case DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE,
          DATAELEMENT_PREVIOUS_EVENT,
          DATAELEMENT_NEWEST_EVENT_PROGRAM,
          DATAELEMENT_CURRENT_EVENT ->
          new DataItem(
              ObjectUtils.firstNonNull(
                  prv.getDataElement().getDisplayFormName(),
                  prv.getDataElement().getFormName(),
                  prv.getDataElement().getName()),
              getItemValueType(prv.getDataElement().getValueType()));
      case CALCULATED_VALUE ->
          new DataItem(
              ObjectUtils.firstNonNull(prv.getDisplayName(), prv.getName()),
              getItemValueType(prv.getValueType()));
      case TEI_ATTRIBUTE ->
          new DataItem(
              ObjectUtils.firstNonNull(
                  prv.getAttribute().getDisplayName(),
                  prv.getAttribute().getDisplayFormName(),
                  prv.getAttribute().getName()),
              getItemValueType(prv.getAttribute().getValueType()));
    };
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
