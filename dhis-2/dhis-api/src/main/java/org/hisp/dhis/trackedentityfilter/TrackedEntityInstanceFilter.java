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
package org.hisp.dhis.trackedentityfilter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.programstagefilter.DateFilterPeriod;
import org.hisp.dhis.programstagefilter.DatePeriodType;
import org.hisp.dhis.translation.Translatable;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 */
@JacksonXmlRootElement(localName = "trackedEntityInstanceFilter", namespace = DxfNamespaces.DXF_2_0)
public class TrackedEntityInstanceFilter extends BaseIdentifiableObject implements MetadataObject {

  /** Property indicating program's of trackedEntityInstanceFilter */
  private Program program;

  /** Property indicating description of trackedEntityInstanceFilter */
  private String description;

  /** Property indicating the filter's order in tracked entity instance search UI */
  private int sortOrder;

  /** Property indicating the filter's rendering style */
  private ObjectStyle style;

  /** Property to filter tracked entity instances based on event dates and statues */
  private List<EventFilter> eventFilters = new ArrayList<>();

  private EntityQueryCriteria entityQueryCriteria;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public TrackedEntityInstanceFilter() {}

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  @JsonProperty
  @JsonSerialize(as = BaseIdentifiableObject.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Program getProgram() {
    return program;
  }

  public void setProgram(Program program) {
    this.program = program;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  @Translatable(propertyName = "description", key = "DESCRIPTION")
  public String getDisplayDescription() {
    return getTranslation("DESCRIPTION", getDescription());
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ObjectStyle getStyle() {
    return style;
  }

  public void setStyle(ObjectStyle style) {
    this.style = style;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public ProgramStatus getEnrollmentStatus() {
    if (this.entityQueryCriteria != null) {
      return entityQueryCriteria.getEnrollmentStatus();
    }
    return null;
  }

  public void setEnrollmentStatus(ProgramStatus enrollmentStatus) {
    if (this.entityQueryCriteria == null) {
      this.entityQueryCriteria = new EntityQueryCriteria();
    }
    this.entityQueryCriteria.setEnrollmentStatus(enrollmentStatus);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public Boolean isFollowup() {
    if (this.entityQueryCriteria != null) {
      return entityQueryCriteria.getFollowUp();
    }
    return false;
  }

  public void setFollowup(Boolean followup) {
    if (this.entityQueryCriteria == null) {
      this.entityQueryCriteria = new EntityQueryCriteria();
    }
    this.entityQueryCriteria.setFollowUp(followup);
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public FilterPeriod getEnrollmentCreatedPeriod() {
    if (this.entityQueryCriteria != null
        && this.entityQueryCriteria.getEnrollmentCreatedDate() != null
        && this.entityQueryCriteria.getEnrollmentCreatedDate().getType()
            == DatePeriodType.RELATIVE) {
      DateFilterPeriod enrollmentCreatedDate = this.entityQueryCriteria.getEnrollmentCreatedDate();
      FilterPeriod enrollmentCreatedPeriod = new FilterPeriod();
      enrollmentCreatedPeriod.setPeriodFrom(enrollmentCreatedDate.getStartBuffer());
      enrollmentCreatedPeriod.setPeriodTo(enrollmentCreatedDate.getEndBuffer());
      return enrollmentCreatedPeriod;
    }
    return null;
  }

  public void setEnrollmentCreatedPeriod(FilterPeriod enrollmentCreatedPeriod) {
    if (enrollmentCreatedPeriod == null) {
      return;
    }

    if (this.entityQueryCriteria == null) {
      this.entityQueryCriteria = new EntityQueryCriteria();
    }

    DateFilterPeriod enrollmentCreatedDate = new DateFilterPeriod();
    enrollmentCreatedDate.setStartBuffer(enrollmentCreatedPeriod.getPeriodFrom());
    enrollmentCreatedDate.setEndBuffer(enrollmentCreatedPeriod.getPeriodTo());
    enrollmentCreatedDate.setType(DatePeriodType.RELATIVE);
    enrollmentCreatedDate.setPeriod(RelativePeriodEnum.TODAY);
    this.entityQueryCriteria.setEnrollmentCreatedDate(enrollmentCreatedDate);
  }

  @JsonProperty("eventFilters")
  @JacksonXmlElementWrapper(localName = "eventFilters", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "eventFilters", namespace = DxfNamespaces.DXF_2_0)
  public List<EventFilter> getEventFilters() {
    return eventFilters;
  }

  public void setEventFilters(List<EventFilter> eventFilters) {
    this.eventFilters = eventFilters;
  }

  @JsonProperty("entityQueryCriteria")
  @JacksonXmlElementWrapper(localName = "entityQueryCriteria", namespace = DxfNamespaces.DXF_2_0)
  @JacksonXmlProperty(localName = "entityQueryCriteria", namespace = DxfNamespaces.DXF_2_0)
  public EntityQueryCriteria getEntityQueryCriteria() {
    return entityQueryCriteria;
  }

  public void setEntityQueryCriteria(EntityQueryCriteria entityQueryCriteria) {
    this.entityQueryCriteria = entityQueryCriteria;
  }
}
