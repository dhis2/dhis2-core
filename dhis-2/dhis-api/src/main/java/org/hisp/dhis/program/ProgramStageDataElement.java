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
package org.hisp.dhis.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.Objects;
import java.util.function.BiFunction;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.adapter.DeviceRenderTypeMapSerializer;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.render.type.ValueTypeRenderingObject;

/**
 * @author Viet Nguyen
 */
@JacksonXmlRootElement(localName = "programStageDataElement", namespace = DxfNamespaces.DXF_2_0)
public class ProgramStageDataElement extends BaseIdentifiableObject implements EmbeddedObject {
  private ProgramStage programStage;

  private DataElement dataElement;

  /** Whether data element is mandatory for program stage. */
  private boolean compulsory = false;

  private Boolean allowProvidedElsewhere = false;

  private Integer sortOrder;

  private Boolean displayInReports = false;

  /** Whether to allow capture of events in the future. */
  private Boolean allowFutureDate = false;

  // Remove this in the future, will be replaced by renderType
  private Boolean renderOptionsAsRadio = false;

  private DeviceRenderTypeMap<ValueTypeRenderingObject> renderType;

  /** Whether to skip data element in data synchronization. */
  private Boolean skipSynchronization = false;

  /** Whether to skip data element in analytics tables and queries, not null. */
  private Boolean skipAnalytics = false;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public ProgramStageDataElement() {}

  public ProgramStageDataElement(ProgramStage programStage, DataElement dataElement) {
    this.programStage = programStage;
    this.dataElement = dataElement;
  }

  public ProgramStageDataElement(
      ProgramStage programStage, DataElement dataElement, boolean compulsory) {
    this.programStage = programStage;
    this.dataElement = dataElement;
    this.compulsory = compulsory;
  }

  public ProgramStageDataElement(
      ProgramStage programStage, DataElement dataElement, boolean compulsory, Integer sortOrder) {
    this.programStage = programStage;
    this.dataElement = dataElement;
    this.compulsory = compulsory;
    this.sortOrder = sortOrder;
  }

  // -------------------------------------------------------------------------
  // Get and set methods
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ProgramStage getProgramStage() {
    return programStage;
  }

  public void setProgramStage(ProgramStage programStage) {
    this.programStage = programStage;
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
  public Boolean getAllowProvidedElsewhere() {
    return allowProvidedElsewhere;
  }

  public void setAllowProvidedElsewhere(Boolean allowProvidedElsewhere) {
    this.allowProvidedElsewhere = allowProvidedElsewhere;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public boolean isCompulsory() {
    return compulsory;
  }

  public void setCompulsory(boolean compulsory) {
    this.compulsory = compulsory;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getDisplayInReports() {
    return displayInReports;
  }

  public void setDisplayInReports(Boolean displayInReports) {
    this.displayInReports = displayInReports;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getAllowFutureDate() {
    return allowFutureDate;
  }

  public void setAllowFutureDate(Boolean allowFutureDate) {
    this.allowFutureDate = allowFutureDate;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getRenderOptionsAsRadio() {
    return renderOptionsAsRadio;
  }

  public void setRenderOptionsAsRadio(Boolean renderOptionsAsRadio) {
    this.renderOptionsAsRadio = renderOptionsAsRadio;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @JsonSerialize(using = DeviceRenderTypeMapSerializer.class)
  public DeviceRenderTypeMap<ValueTypeRenderingObject> getRenderType() {
    return renderType;
  }

  public void setRenderType(DeviceRenderTypeMap<ValueTypeRenderingObject> renderType) {
    this.renderType = renderType;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getSkipSynchronization() {
    return skipSynchronization;
  }

  public void setSkipSynchronization(Boolean skipSynchronization) {
    this.skipSynchronization = skipSynchronization;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean getSkipAnalytics() {
    return skipAnalytics;
  }

  public void setSkipAnalytics(Boolean skipAnalytics) {
    this.skipAnalytics = skipAnalytics;
  }

  // -------------------------------------------------------------------------
  // hashCode, equals and toString
  // -------------------------------------------------------------------------

  @Override
  public boolean equals(Object obj) {
    return this == obj
        || obj instanceof ProgramStageDataElement && objectEquals((ProgramStageDataElement) obj);
  }

  private boolean objectEquals(ProgramStageDataElement other) {
    return Objects.equals(dataElement, other.dataElement)
        && Objects.equals(programStage, other.programStage);
  }

  @Override
  public int hashCode() {
    int result = programStage != null ? programStage.hashCode() : 0;
    result = 31 * result + (dataElement != null ? dataElement.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ProgramStageDataElement{"
        + "programStage="
        + programStage
        + ", dataElement="
        + dataElement
        + ", compulsory="
        + compulsory
        + ", allowProvidedElsewhere="
        + allowProvidedElsewhere
        + ", sortOrder="
        + sortOrder
        + ", displayInReports="
        + displayInReports
        + ", allowFutureDate="
        + allowFutureDate
        + ", renderOptionsAsRadio="
        + renderOptionsAsRadio
        + '}';
  }

  public static final BiFunction<ProgramStageDataElement, ProgramStage, ProgramStageDataElement>
      copyOf =
          (original, stage) -> {
            ProgramStageDataElement copy = new ProgramStageDataElement();
            copy.setProgramStage(stage);
            copy.setAutoFields();
            setShallowCopyValues(copy, original);
            return copy;
          };

  private static void setShallowCopyValues(
      ProgramStageDataElement copy, ProgramStageDataElement original) {
    copy.setAllowFutureDate(original.getAllowFutureDate());
    copy.setAllowProvidedElsewhere(original.getAllowProvidedElsewhere());
    copy.setCompulsory(original.isCompulsory());
    copy.setDataElement(original.getDataElement());
    copy.setDisplayInReports(original.getDisplayInReports());
    copy.setLastUpdatedBy(original.getLastUpdatedBy());
    copy.setName(original.getName());
    copy.setPublicAccess(original.getPublicAccess());
    copy.setRenderOptionsAsRadio(original.getRenderOptionsAsRadio());
    copy.setRenderType(original.getRenderType());
    copy.setSharing(original.getSharing());
    copy.setSkipAnalytics(original.getSkipAnalytics());
    copy.setSkipSynchronization(original.getSkipSynchronization());
    copy.setSortOrder(original.getSortOrder());
  }
}
