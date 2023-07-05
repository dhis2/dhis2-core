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
package org.hisp.dhis.visualization;

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;

/**
 * The ReportingParams object represents the reporting parameters for a Visualization. The reporting
 * parameters are meant to make the Visualization more generic, as it can avoid having dynamic,
 * selectable parameters rather than static. The ReportingParams is mainly used to assist during the
 * Report generation.
 */
@JacksonXmlRootElement(localName = "reportingParams", namespace = DXF_2_0)
public class ReportingParams implements Serializable {

  /** Determines if a de-serialized file is compatible with this class. */
  private static final long serialVersionUID = 3380878533309773050L;

  /** Flag to indicate if there is a reporting period set or not. */
  private boolean reportingPeriod;

  /** Indicates if there is a grandparent organisation unit set or not. */
  private boolean grandParentOrganisationUnit;

  /** Indicates if there is a parent organisation unit set or not. */
  private boolean parentOrganisationUnit;

  /** Indicates if there is an organisation unit set or not. */
  private boolean organisationUnit;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public ReportingParams() {}

  public ReportingParams(
      boolean reportingPeriod,
      boolean grandParentOrganisationUnit,
      boolean parentOrganisationUnit,
      boolean organisationUnit) {
    this.reportingPeriod = reportingPeriod;
    this.grandParentOrganisationUnit = grandParentOrganisationUnit;
    this.parentOrganisationUnit = parentOrganisationUnit;
    this.organisationUnit = organisationUnit;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public boolean isSet() {
    return isReportingPeriod() || isOrganisationUnitSet();
  }

  public boolean isOrganisationUnitSet() {
    return isGrandParentOrganisationUnit() || isParentOrganisationUnit() || isOrganisationUnit();
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty(value = "reportingPeriod")
  @JacksonXmlProperty(namespace = DXF_2_0)
  public boolean isReportingPeriod() {
    return reportingPeriod;
  }

  public void setReportingPeriod(Boolean reportingPeriod) {
    this.reportingPeriod = reportingPeriod;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public boolean isGrandParentOrganisationUnit() {
    return grandParentOrganisationUnit;
  }

  public void setGrandParentOrganisationUnit(Boolean grandParentOrganisationUnit) {
    this.grandParentOrganisationUnit = grandParentOrganisationUnit;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public boolean isParentOrganisationUnit() {
    return parentOrganisationUnit;
  }

  public void setParentOrganisationUnit(Boolean parentOrganisationUnit) {
    this.parentOrganisationUnit = parentOrganisationUnit;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DXF_2_0)
  public boolean isOrganisationUnit() {
    return organisationUnit;
  }

  public void setOrganisationUnit(Boolean organisationUnit) {
    this.organisationUnit = organisationUnit;
  }
}
