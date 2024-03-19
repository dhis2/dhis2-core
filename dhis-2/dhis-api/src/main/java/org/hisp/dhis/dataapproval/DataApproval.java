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
package org.hisp.dhis.dataapproval;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.Property.Value;
import org.hisp.dhis.schema.annotation.PropertyTransformer;
import org.hisp.dhis.schema.transformer.UserPropertyTransformer;
import org.hisp.dhis.user.User;

/**
 * Records the approval of DataSet values for a given OrganisationUnit and Period.
 *
 * @author Jim Grace
 */
public class DataApproval implements Serializable {
  public static final String AUTH_APPROVE = "F_APPROVE_DATA";

  public static final String AUTH_APPROVE_LOWER_LEVELS = "F_APPROVE_DATA_LOWER_LEVELS";

  public static final String AUTH_ACCEPT_LOWER_LEVELS = "F_ACCEPT_DATA_LOWER_LEVELS";

  public static final String AUTH_VIEW_UNAPPROVED_DATA = "F_VIEW_UNAPPROVED_DATA";

  private static final long serialVersionUID = -4034531921928532366L;

  /** Identifies the data approval instance (required). */
  private long id;

  /** The approval level for which this approval is defined (required). */
  private DataApprovalLevel dataApprovalLevel;

  /** The workflow for the values being approved (required). */
  private DataApprovalWorkflow workflow;

  /** The Period of the approval (required). */
  private Period period;

  /** The OrganisationUnit of the approval (required). */
  private OrganisationUnit organisationUnit;

  /** The attribute category option combo being approved (required). */
  private CategoryOptionCombo attributeOptionCombo;

  /** Whether the approval has been accepted (optional, usually by another user.) */
  private boolean accepted;

  /** The Date (including time) when this approval was made (required). */
  private Date created;

  /** The User who made this approval (required). */
  private User creator;

  /** The Date (including time) when {@link #accepted} status of this approval was last changed */
  private Date lastUpdated;

  /** The User who made the last change to the {@link #accepted} status of this approval */
  private User lastUpdatedBy;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public DataApproval() {}

  public DataApproval(
      DataApprovalLevel dataApprovalLevel,
      DataApprovalWorkflow workflow,
      Period period,
      OrganisationUnit organisationUnit,
      CategoryOptionCombo attributeOptionCombo) {
    this.dataApprovalLevel = dataApprovalLevel;
    this.workflow = workflow;
    this.period = period;
    this.organisationUnit = organisationUnit;
    this.attributeOptionCombo = attributeOptionCombo;
  }

  public DataApproval(
      DataApprovalLevel dataApprovalLevel,
      DataApprovalWorkflow workflow,
      Period period,
      OrganisationUnit organisationUnit,
      CategoryOptionCombo attributeOptionCombo,
      boolean accepted,
      Date created,
      User creator) {
    this.dataApprovalLevel = dataApprovalLevel;
    this.workflow = workflow;
    this.period = period;
    this.organisationUnit = organisationUnit;
    this.attributeOptionCombo = attributeOptionCombo;
    this.accepted = accepted;
    this.created = created;
    this.creator = creator;
    this.lastUpdated = created;
    this.lastUpdatedBy = creator;
  }

  public DataApproval(DataApproval da) {
    this.dataApprovalLevel = da.dataApprovalLevel;
    this.workflow = da.workflow;
    this.period = da.period;
    this.organisationUnit = da.organisationUnit;
    this.attributeOptionCombo = da.attributeOptionCombo;
    this.accepted = da.accepted;
    this.created = da.created;
    this.creator = da.creator;
    this.lastUpdated = da.lastUpdated;
    this.lastUpdatedBy = da.lastUpdatedBy;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /** Finds the lowest level (if any) at which data would be approved. */
  public static DataApproval getLowestApproval(DataApproval dataApproval) {
    OrganisationUnit orgUnit = dataApproval.getOrganisationUnit();

    List<DataApprovalLevel> approvalLevels = dataApproval.getWorkflow().getSortedLevels();

    Collections.reverse(approvalLevels);

    DataApproval da = null;

    for (DataApprovalLevel approvalLevel : approvalLevels) {
      int orgUnitLevel = orgUnit.getLevel();

      if (approvalLevel.getOrgUnitLevel() <= orgUnitLevel) {
        if (approvalLevel.getOrgUnitLevel() < orgUnitLevel) {
          orgUnit = orgUnit.getAncestors().get(approvalLevel.getOrgUnitLevel() - 1);
        }

        da =
            new DataApproval(
                approvalLevel,
                dataApproval.getWorkflow(),
                dataApproval.getPeriod(),
                orgUnit,
                dataApproval.getAttributeOptionCombo());

        break;
      }
    }

    return da;
  }

  public String getCacheKey() {
    return dataApprovalLevel.getUid()
        + "-"
        + workflow.getUid()
        + "-"
        + period.getUid()
        + "-"
        + organisationUnit.getUid()
        + "-"
        + attributeOptionCombo.getUid();
  }

  // -------------------------------------------------------------------------
  // Getters and setters
  // -------------------------------------------------------------------------

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @JsonProperty
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public DataApprovalLevel getDataApprovalLevel() {
    return dataApprovalLevel;
  }

  public void setDataApprovalLevel(DataApprovalLevel dataApprovalLevel) {
    this.dataApprovalLevel = dataApprovalLevel;
  }

  public DataApprovalWorkflow getWorkflow() {
    return workflow;
  }

  public void setWorkflow(DataApprovalWorkflow workflow) {
    this.workflow = workflow;
  }

  public Period getPeriod() {
    return period;
  }

  public void setPeriod(Period period) {
    this.period = period;
  }

  public OrganisationUnit getOrganisationUnit() {
    return organisationUnit;
  }

  public void setOrganisationUnit(OrganisationUnit organisationUnit) {
    this.organisationUnit = organisationUnit;
  }

  public CategoryOptionCombo getAttributeOptionCombo() {
    return attributeOptionCombo;
  }

  public void setAttributeOptionCombo(CategoryOptionCombo attributeOptionCombo) {
    this.attributeOptionCombo = attributeOptionCombo;
  }

  public boolean isAccepted() {
    return accepted;
  }

  public void setAccepted(boolean accepted) {
    this.accepted = accepted;
  }

  public void setAccepted(boolean accepted, User by) {
    setAccepted(accepted);
    setLastUpdatedBy(by);
    setLastUpdated(new Date());
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public User getCreator() {
    return creator;
  }

  public void setCreator(User creator) {
    this.creator = creator;
  }

  @JsonProperty
  @JacksonXmlProperty(isAttribute = true)
  @Description("The date this object was last updated.")
  @Property(value = PropertyType.DATE, required = Value.FALSE)
  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  @JsonProperty
  @JsonSerialize(using = UserPropertyTransformer.JacksonSerialize.class)
  @JsonDeserialize(using = UserPropertyTransformer.JacksonDeserialize.class)
  @PropertyTransformer(UserPropertyTransformer.class)
  @JacksonXmlProperty(namespace = DxfNamespaces.DXF_2_0)
  public User getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public void setLastUpdatedBy(User lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  // ----------------------------------------------------------------------
  // hashCode, equals, toString
  // ----------------------------------------------------------------------

  @Override
  public int hashCode() {
    return Objects.hash(
        dataApprovalLevel, workflow, period, organisationUnit, attributeOptionCombo);
  }

  @Override
  public String toString() {
    return "DataApproval{"
        + "id="
        + id
        + ", dataApprovalLevel="
        + (dataApprovalLevel == null ? "(null)" : dataApprovalLevel.getLevel())
        + ", workflow='"
        + (workflow == null ? "(null)" : workflow.getName())
        + "'"
        + ", period="
        + (period == null ? "(null)" : period.getName())
        + ", organisationUnit='"
        + (organisationUnit == null ? "(null)" : organisationUnit.getName())
        + "'"
        + ", attributeOptionCombo='"
        + (attributeOptionCombo == null ? "(null)" : attributeOptionCombo.getName())
        + "'"
        + ", accepted="
        + accepted
        + ", created="
        + created
        + ", creator="
        + (creator == null ? "(null)" : creator.getName())
        + '}';
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DataApproval)) {
      return false;
    }

    DataApproval other = (DataApproval) object;

    return Objects.equals(dataApprovalLevel, other.dataApprovalLevel)
        && Objects.equals(workflow, other.workflow)
        && Objects.equals(period, other.period)
        && Objects.equals(organisationUnit, other.organisationUnit)
        && Objects.equals(attributeOptionCombo, other.attributeOptionCombo);
  }
}
