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
package org.hisp.dhis.programrule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Set;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionGroup;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.translation.Translatable;

/**
 * @author Markus Bekken
 */
@JacksonXmlRootElement(localName = "programRuleAction", namespace = DxfNamespaces.DXF_2_0)
public class ProgramRuleAction extends BaseIdentifiableObject implements MetadataObject {
  /** The programRule that the action belongs to */
  private ProgramRule programRule;

  /**
   * The type of action that is performed when the action is effectuated
   *
   * <p>The actual action the ruleaction row is performing. Allowed values are:
   *
   * <ul>
   *   <li>{@code displaytext} Shows a text in the rulebound widget with the matching location
   *       string. location: the location code of the widget to display data in content: A hardcoded
   *       string to display data: a variable to be evaluated and displayed at the end of the
   *       string, can be null.
   *   <li>{@code displaykeyvaluepair} Shows a key data box with a hardcoded name and a variable
   *       value. location: the location code of the widget to display data in content: A hardcoded
   *       string to display as title in the key data box data: The variable to be evaluated and
   *       display in the lower half of the key data box.
   *   <li>{@code hidefield} Hides a dataelement from the page, as long as the dataelement is not
   *       containing a value. dataelement: the dataelement to hide.
   *   <li>{@code assignvariable} Assigns/calculates a value that can be further used by other
   *       rules. Make sure the priorities is set so the rules that depend on the calculation is run
   *       after the assignvariable-rule. content: the variable name to be assigned. “$severeanemia”
   *       for example. data: the expression to be evaluated and assigned to the content field. Can
   *       contain a hardcoded value(f.ex. “true”) or an expression that is evaluated(for exampple
   *       “$hemoglobin < 7”).
   *   <li>{@code showwarning} Shows a validation warning connected to a designated dataelement
   *       dataelement: the dataelement to show validationerror for content: the validation error
   *       itself
   *   <li>{@code showerror} Shows a validation error connected to a designated dataelement
   *       dataelement: the dataelement to show validationerror for content: the validation error
   *       itself
   * </ul>
   */
  private ProgramRuleActionType programRuleActionType;

  /**
   * The data element that is affected by the rule action. Used for:
   *
   * <p>
   *
   * <ul>
   *   <li>hidefield
   *   <li>showwarning
   *   <li>showerror
   * </ul>
   */
  private DataElement dataElement;

  /**
   * The data element that is affected by the rule action. Used for:
   *
   * <p>
   *
   * <ul>
   *   <li>hidefield
   *   <li>showwarning
   *   <li>showerror
   * </ul>
   */
  private TrackedEntityAttribute attribute;

  /**
   * The program indicator that is affected by the rule action. Used for:
   *
   * <ul>
   *   <li>hidefield
   * </ul>
   */
  private ProgramIndicator programIndicator;

  /** The program stage section that is affected by the rule action. */
  private ProgramStageSection programStageSection;

  /** The program stage that is affected by the rule action. */
  private ProgramStage programStage;

  /**
   * The program notification that will be triggered by the rule action. Used for:
   *
   * <ul>
   *   <li>sendmessage
   * </ul>
   */
  private ProgramNotificationTemplate notificationTemplate;

  /**
   * Used to determine which widget to display data for the two action types:
   *
   * <p>
   *
   * <ul>
   *   <li>displaytext
   *   <li>displaykeydata
   * </ul>
   */
  private String location;

  /** Used by all the different actions. See “actions” */
  private String content;

  /**
   * Used by all the different actions. See “actions”. The data field will be evaluated, so it can
   * contain a rich expression.
   */
  private String data;

  /** Option affected by the rule action */
  private Option option;

  /** Option group affected by the rule action */
  private OptionGroup optionGroup;

  /**
   * The time when the rule is going to be evaluated. This field is used to run only the rules that
   * makes sense at each stage of the rule validation. Default to {@link
   * ProgramRuleActionEvaluationTime#ALWAYS}
   */
  private ProgramRuleActionEvaluationTime programRuleActionEvaluationTime =
      ProgramRuleActionEvaluationTime.ALWAYS;

  /**
   * The environments where the rule is going to be evaluated. This field is used to run only the
   * rules that makes sense in each environment. Default to {@link
   * ProgramRuleActionEvaluationEnvironment#getDefault()}
   */
  private Set<ProgramRuleActionEvaluationEnvironment> programRuleActionEvaluationEnvironments =
      ProgramRuleActionEvaluationEnvironment.getDefault();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public ProgramRuleAction() {}

  public ProgramRuleAction(
      String name,
      ProgramRule programRule,
      ProgramRuleActionType programRuleActionType,
      DataElement dataElement,
      TrackedEntityAttribute attribute,
      ProgramStageSection programStageSection,
      ProgramStage programStage,
      ProgramIndicator programIndicator,
      String location,
      String content,
      String data,
      Option option,
      OptionGroup optionGroup) {
    this.name = name;
    this.programRule = programRule;
    this.programRuleActionType = programRuleActionType;
    this.dataElement = dataElement;
    this.attribute = attribute;
    this.programStageSection = programStageSection;
    this.programStage = programStage;
    this.programIndicator = programIndicator;
    this.location = location;
    this.content = content;
    this.data = data;
    this.option = option;
    this.optionGroup = optionGroup;
  }

  public ProgramRuleAction(
      String name,
      ProgramRule programRule,
      ProgramRuleActionType programRuleActionType,
      DataElement dataElement,
      TrackedEntityAttribute attribute,
      ProgramStageSection programStageSection,
      ProgramStage programStage,
      ProgramIndicator programIndicator,
      String location,
      String content,
      String data,
      Option option,
      OptionGroup optionGroup,
      ProgramRuleActionEvaluationTime evaluationTime,
      Set<ProgramRuleActionEvaluationEnvironment> environments) {
    this(
        name,
        programRule,
        programRuleActionType,
        dataElement,
        attribute,
        programStageSection,
        programStage,
        programIndicator,
        location,
        content,
        data,
        option,
        optionGroup);
    this.programRuleActionEvaluationTime = evaluationTime;
    this.programRuleActionEvaluationEnvironments = environments;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public boolean hasDataElement() {
    return this.dataElement != null;
  }

  public boolean hasTrackedEntityAttribute() {
    return this.attribute != null;
  }

  public boolean hasContent() {
    return this.content != null && !this.content.isEmpty();
  }

  public boolean hasNotification() {
    return getTemplateUid() != null;
  }

  public boolean hasOption() {
    return this.option != null;
  }

  public boolean hasOptionGroup() {
    return this.optionGroup != null;
  }

  public boolean hasProgramStage() {
    return programStage != null;
  }

  public boolean hasProgramStageSection() {
    return programStageSection != null;
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ProgramRule getProgramRule() {
    return programRule;
  }

  public void setProgramRule(ProgramRule programRule) {
    this.programRule = programRule;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ProgramRuleActionType getProgramRuleActionType() {
    return programRuleActionType;
  }

  public void setProgramRuleActionType(ProgramRuleActionType programRuleActionType) {
    this.programRuleActionType = programRuleActionType;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DataElement getDataElement() {
    return dataElement;
  }

  public void setDataElement(DataElement dataElement) {
    this.dataElement = dataElement;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Property(required = Property.Value.FALSE, owner = Property.Value.TRUE)
  public String getTemplateUid() {
    return notificationTemplate != null ? notificationTemplate.getUid() : null;
  }

  public void setTemplateUid(String templateUid) {
    if (templateUid == null) {
      notificationTemplate = null;
      return;
    }

    ProgramNotificationTemplate template = new ProgramNotificationTemplate();
    template.setUid(templateUid);
    setNotificationTemplate(template);
  }

  public ProgramNotificationTemplate getNotificationTemplate() {
    return notificationTemplate;
  }

  public void setNotificationTemplate(ProgramNotificationTemplate notificationTemplate) {
    this.notificationTemplate = notificationTemplate;
  }

  @JsonProperty("trackedEntityAttribute")
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public TrackedEntityAttribute getAttribute() {
    return attribute;
  }

  public void setAttribute(TrackedEntityAttribute attribute) {
    this.attribute = attribute;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ProgramIndicator getProgramIndicator() {
    return programIndicator;
  }

  public void setProgramIndicator(ProgramIndicator programIndicator) {
    this.programIndicator = programIndicator;
  }

  public void setProgramStage(ProgramStage programStage) {
    this.programStage = programStage;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ProgramStage getProgramStage() {
    return programStage;
  }

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ProgramStageSection getProgramStageSection() {
    return programStageSection;
  }

  public void setProgramStageSection(ProgramStageSection programStageSection) {
    this.programStageSection = programStageSection;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getContent() {
    return content;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "content", key = "CONTENT")
  public String getDisplayContent() {
    return getTranslation("CONTENT", getContent());
  }

  public void setContent(String content) {
    this.content = content;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Option getOption() {
    return option;
  }

  public void setOption(Option option) {
    this.option = option;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public OptionGroup getOptionGroup() {
    return optionGroup;
  }

  public void setOptionGroup(OptionGroup optionGroup) {
    this.optionGroup = optionGroup;
  }

  @JsonProperty
  @JacksonXmlProperty(localName = "evaluationTime", namespace = DxfNamespaces.DXF_2_0)
  public ProgramRuleActionEvaluationTime getProgramRuleActionEvaluationTime() {
    return programRuleActionEvaluationTime;
  }

  public void setProgramRuleActionEvaluationTime(
      ProgramRuleActionEvaluationTime programRuleActionEvaluationTime) {
    this.programRuleActionEvaluationTime = programRuleActionEvaluationTime;
  }

  @JsonProperty
  @JacksonXmlElementWrapper(localName = "evaluationEnvironments", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "evaluationEnvironment", namespace = DxfNamespaces.DXF_2_0)
  public Set<ProgramRuleActionEvaluationEnvironment> getProgramRuleActionEvaluationEnvironments() {
    return programRuleActionEvaluationEnvironments;
  }

  public void setProgramRuleActionEvaluationEnvironments(
      Set<ProgramRuleActionEvaluationEnvironment> programRuleActionEvaluationEnvironments) {
    this.programRuleActionEvaluationEnvironments = programRuleActionEvaluationEnvironments;
  }
}
