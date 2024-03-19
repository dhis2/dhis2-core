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
package org.hisp.dhis.webapi.webdomain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement
public class ValidationResultView {
  private String validationRuleId;

  private String validationRuleDescription;

  private String organisationUnitId;

  private String organisationUnitDisplayName;

  private String organisationUnitPath;

  private String organisationUnitAncestorNames;

  private String periodId;

  private String periodDisplayName;

  private String attributeOptionComboId;

  private String attributeOptionComboDisplayName;

  private String importance;

  private Double leftSideValue;

  private String operator;

  private Double rightSideValue;

  @JsonProperty
  public String getValidationRuleId() {
    return validationRuleId;
  }

  public void setValidationRuleId(String validationRuleId) {
    this.validationRuleId = validationRuleId;
  }

  @JsonProperty
  public String getValidationRuleDescription() {
    return validationRuleDescription;
  }

  public void setValidationRuleDescription(String validationRuleDescription) {
    this.validationRuleDescription = validationRuleDescription;
  }

  @JsonProperty
  public String getOrganisationUnitId() {
    return organisationUnitId;
  }

  public void setOrganisationUnitId(String organisationUnitId) {
    this.organisationUnitId = organisationUnitId;
  }

  @JsonProperty
  public String getOrganisationUnitDisplayName() {
    return organisationUnitDisplayName;
  }

  public void setOrganisationUnitDisplayName(String organisationUnitDisplayName) {
    this.organisationUnitDisplayName = organisationUnitDisplayName;
  }

  @JsonProperty
  public String getOrganisationUnitPath() {
    return organisationUnitPath;
  }

  public void setOrganisationUnitPath(String organisationUnitPath) {
    this.organisationUnitPath = organisationUnitPath;
  }

  @JsonProperty
  public String getOrganisationUnitAncestorNames() {
    return organisationUnitAncestorNames;
  }

  public void setOrganisationUnitAncestorNames(String organisationUnitAncestorNames) {
    this.organisationUnitAncestorNames = organisationUnitAncestorNames;
  }

  @JsonProperty
  public String getPeriodId() {
    return periodId;
  }

  public void setPeriodId(String periodId) {
    this.periodId = periodId;
  }

  @JsonProperty
  public String getPeriodDisplayName() {
    return periodDisplayName;
  }

  public void setPeriodDisplayName(String periodDisplayName) {
    this.periodDisplayName = periodDisplayName;
  }

  @JsonProperty
  public String getAttributeOptionComboId() {
    return attributeOptionComboId;
  }

  public void setAttributeOptionComboId(String attributeOptionComboId) {
    this.attributeOptionComboId = attributeOptionComboId;
  }

  @JsonProperty
  public String getAttributeOptionComboDisplayName() {
    return attributeOptionComboDisplayName;
  }

  public void setAttributeOptionComboDisplayName(String attributeOptionComboDisplayName) {
    this.attributeOptionComboDisplayName = attributeOptionComboDisplayName;
  }

  @JsonProperty
  public String getImportance() {
    return importance;
  }

  public void setImportance(String importance) {
    this.importance = importance;
  }

  @JsonProperty
  public Double getLeftSideValue() {
    return leftSideValue;
  }

  public void setLeftSideValue(Double leftSideValue) {
    this.leftSideValue = leftSideValue;
  }

  @JsonProperty
  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

  @JsonProperty
  public Double getRightSideValue() {
    return rightSideValue;
  }

  public void setRightSideValue(Double rightSideValue) {
    this.rightSideValue = rightSideValue;
  }
}
