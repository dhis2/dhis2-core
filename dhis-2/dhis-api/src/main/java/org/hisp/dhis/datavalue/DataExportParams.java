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
package org.hisp.dhis.datavalue;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

/**
 * @author Lars Helge Overland
 */
public class DataExportParams {
  private Set<DataElement> dataElements = new HashSet<>();

  private Set<DataElementOperand> dataElementOperands = new HashSet<>();

  private Set<DataSet> dataSets = new HashSet<>();

  private Set<DataElementGroup> dataElementGroups = new HashSet<>();

  private Set<Period> periods = new HashSet<>();

  private Set<PeriodType> periodTypes = new HashSet<>();

  private Date startDate;

  private Date endDate;

  private Date includedDate;

  private Set<OrganisationUnit> organisationUnits = new HashSet<>();

  private OrganisationUnitSelectionMode ouMode = SELECTED;

  private Integer orgUnitLevel;

  private boolean includeDescendants;

  private boolean orderByOrgUnitPath;

  private boolean orderByPeriod;

  private Set<OrganisationUnitGroup> organisationUnitGroups = new HashSet<>();

  private Set<CategoryOptionCombo> attributeOptionCombos = new HashSet<>();

  private Set<CategoryOption> coDimensionConstraints;

  private Set<CategoryOptionGroup> cogDimensionConstraints;

  private boolean includeDeleted;

  private Date lastUpdated;

  private String lastUpdatedDuration;

  private Integer limit;

  private IdSchemes outputIdSchemes;

  private BlockingQueue<DeflatedDataValue> blockingQueue;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  public DataExportParams() {}

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public Set<DataElement> getAllDataElements() {
    final Set<DataElement> elements = Sets.newHashSet();

    elements.addAll(dataElements);
    dataSets.forEach(ds -> elements.addAll(ds.getDataElements()));
    dataElementGroups.forEach(dg -> elements.addAll(dg.getMembers()));

    return ImmutableSet.copyOf(elements);
  }

  public Set<OrganisationUnit> getAllOrganisationUnits() {
    final Set<OrganisationUnit> orgUnits = Sets.newHashSet();
    orgUnits.addAll(organisationUnits);

    for (OrganisationUnitGroup group : organisationUnitGroups) {
      orgUnits.addAll(group.getMembers());
    }

    return ImmutableSet.copyOf(orgUnits);
  }

  public boolean hasDataElements() {
    return dataElements != null && !dataElements.isEmpty();
  }

  public boolean hasDataSets() {
    return dataSets != null && !dataSets.isEmpty();
  }

  public boolean hasDataElementGroups() {
    return dataElementGroups != null && !dataElementGroups.isEmpty();
  }

  public DataSet getFirstDataSet() {
    return hasDataSets() ? dataSets.iterator().next() : null;
  }

  public Period getFirstPeriod() {
    return hasPeriods() ? periods.iterator().next() : null;
  }

  public boolean hasPeriods() {
    return periods != null && !periods.isEmpty();
  }

  public boolean hasPeriodTypes() {
    return periodTypes != null && !periodTypes.isEmpty();
  }

  public boolean hasStartEndDate() {
    return startDate != null && endDate != null;
  }

  public boolean hasIncludedDate() {
    return includedDate != null;
  }

  public boolean hasOrganisationUnits() {
    return organisationUnits != null && !organisationUnits.isEmpty();
  }

  public boolean isIncludeDescendantsForOrganisationUnits() {
    return includeDescendants && hasOrganisationUnits();
  }

  public boolean hasOrgUnitLevel() {
    return orgUnitLevel != null;
  }

  public boolean hasBlockingQueue() {
    return blockingQueue != null;
  }

  public OrganisationUnit getFirstOrganisationUnit() {
    return organisationUnits != null && !organisationUnits.isEmpty()
        ? organisationUnits.iterator().next()
        : null;
  }

  public boolean hasOrganisationUnitGroups() {
    return organisationUnitGroups != null && !organisationUnitGroups.isEmpty();
  }

  public boolean hasAttributeOptionCombos() {
    return attributeOptionCombos != null && !attributeOptionCombos.isEmpty();
  }

  public boolean hasCoDimensionConstraints() {
    return coDimensionConstraints != null && !coDimensionConstraints.isEmpty();
  }

  public boolean hasCogDimensionConstraints() {
    return cogDimensionConstraints != null && !cogDimensionConstraints.isEmpty();
  }

  public boolean hasLastUpdated() {
    return lastUpdated != null;
  }

  public boolean hasLastUpdatedDuration() {
    return lastUpdatedDuration != null;
  }

  public boolean hasLimit() {
    return limit != null;
  }

  /**
   * Indicates whether this parameters represents a single data value set, implying that it contains
   * exactly one of data sets, periods and organisation units.
   */
  public boolean isSingleDataValueSet() {
    return dataSets.size() == 1
        && periods.size() == 1
        && organisationUnits.size() == 1
        && dataElementGroups.isEmpty();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("data elements", dataElements)
        .add("data element operands", dataElementOperands)
        .add("data sets", dataSets)
        .add("data element groups", dataElementGroups)
        .add("periods", periods)
        .add("period types", periodTypes)
        .add("start date", startDate)
        .add("end date", endDate)
        .add("included date", includedDate)
        .add("org units", organisationUnits)
        .add("org unit selection mode", ouMode)
        .add("org unit level", orgUnitLevel)
        .add("descendants", includeDescendants)
        .add("order by org unit path", orderByOrgUnitPath)
        .add("order by period", orderByPeriod)
        .add("org unit groups", organisationUnitGroups)
        .add("attribute option combos", attributeOptionCombos)
        .add("category option dimension constraints", coDimensionConstraints)
        .add("category option group dimension constraints", cogDimensionConstraints)
        .add("deleted", includeDeleted)
        .add("last updated", lastUpdated)
        .add("last updated duration", lastUpdatedDuration)
        .add("limit", limit)
        .add("output id schemes", outputIdSchemes)
        .add("blockingQueue", blockingQueue)
        .toString();
  }

  // -------------------------------------------------------------------------
  // Get and set methods
  // -------------------------------------------------------------------------

  public Set<DataElement> getDataElements() {
    return dataElements;
  }

  public DataExportParams setDataElements(Set<DataElement> dataElements) {
    this.dataElements = dataElements;
    return this;
  }

  public Set<DataElementOperand> getDataElementOperands() {
    return dataElementOperands;
  }

  public DataExportParams setDataElementOperands(Set<DataElementOperand> dataElementOperands) {
    this.dataElementOperands = dataElementOperands;
    return this;
  }

  public Set<DataSet> getDataSets() {
    return dataSets;
  }

  public DataExportParams setDataSets(Set<DataSet> dataSets) {
    this.dataSets = dataSets;
    return this;
  }

  public Set<DataElementGroup> getDataElementGroups() {
    return dataElementGroups;
  }

  public DataExportParams setDataElementGroups(Set<DataElementGroup> dataElementGroups) {
    this.dataElementGroups = dataElementGroups;
    return this;
  }

  public Set<PeriodType> getPeriodTypes() {
    return periodTypes;
  }

  public DataExportParams setPeriodTypes(Set<PeriodType> periodTypes) {
    this.periodTypes = periodTypes;
    return this;
  }

  public Set<Period> getPeriods() {
    return periods;
  }

  public DataExportParams setPeriods(Set<Period> periods) {
    this.periods = periods;
    return this;
  }

  public Date getStartDate() {
    return startDate;
  }

  public DataExportParams setStartDate(Date startDate) {
    this.startDate = startDate;
    return this;
  }

  public Date getIncludedDate() {
    return includedDate;
  }

  public DataExportParams setIncludedDate(Date includedDate) {
    this.includedDate = includedDate;
    return this;
  }

  public Date getEndDate() {
    return endDate;
  }

  public DataExportParams setEndDate(Date endDate) {
    this.endDate = endDate;
    return this;
  }

  public Set<OrganisationUnit> getOrganisationUnits() {
    return organisationUnits;
  }

  public DataExportParams setOrganisationUnits(Set<OrganisationUnit> organisationUnits) {
    this.organisationUnits = organisationUnits;
    return this;
  }

  public OrganisationUnitSelectionMode getOuMode() {
    return ouMode;
  }

  public DataExportParams setOuMode(OrganisationUnitSelectionMode ouMode) {
    this.ouMode = ouMode;
    return this;
  }

  public Integer getOrgUnitLevel() {
    return orgUnitLevel;
  }

  public DataExportParams setOrgUnitLevel(Integer orgUnitLevel) {
    this.orgUnitLevel = orgUnitLevel;
    return this;
  }

  public boolean isIncludeDescendants() {
    return includeDescendants;
  }

  public DataExportParams setIncludeDescendants(boolean includeDescendants) {
    this.includeDescendants = includeDescendants;
    return this;
  }

  public boolean isOrderByOrgUnitPath() {
    return orderByOrgUnitPath;
  }

  public DataExportParams setOrderByOrgUnitPath(boolean orderByOrgUnitPath) {
    this.orderByOrgUnitPath = orderByOrgUnitPath;
    return this;
  }

  public boolean isOrderByPeriod() {
    return orderByPeriod;
  }

  public DataExportParams setOrderByPeriod(boolean orderByPeriod) {
    this.orderByPeriod = orderByPeriod;
    return this;
  }

  public Set<OrganisationUnitGroup> getOrganisationUnitGroups() {
    return organisationUnitGroups;
  }

  public DataExportParams setOrganisationUnitGroups(
      Set<OrganisationUnitGroup> organisationUnitGroups) {
    this.organisationUnitGroups = organisationUnitGroups;
    return this;
  }

  public Set<CategoryOptionCombo> getAttributeOptionCombos() {
    return attributeOptionCombos;
  }

  public DataExportParams setAttributeOptionCombos(Set<CategoryOptionCombo> attributeOptionCombos) {
    this.attributeOptionCombos = attributeOptionCombos;
    return this;
  }

  public Set<CategoryOption> getCoDimensionConstraints() {
    return coDimensionConstraints;
  }

  public DataExportParams setCoDimensionConstraints(Set<CategoryOption> coDimensionConstraints) {
    this.coDimensionConstraints = coDimensionConstraints;
    return this;
  }

  public Set<CategoryOptionGroup> getCogDimensionConstraints() {
    return cogDimensionConstraints;
  }

  public DataExportParams setCogDimensionConstraints(
      Set<CategoryOptionGroup> cogDimensionConstraints) {
    this.cogDimensionConstraints = cogDimensionConstraints;
    return this;
  }

  public boolean isIncludeDeleted() {
    return includeDeleted;
  }

  public DataExportParams setIncludeDeleted(boolean includeDeleted) {
    this.includeDeleted = includeDeleted;
    return this;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public DataExportParams setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
    return this;
  }

  public String getLastUpdatedDuration() {
    return lastUpdatedDuration;
  }

  public DataExportParams setLastUpdatedDuration(String lastUpdatedDuration) {
    this.lastUpdatedDuration = lastUpdatedDuration;
    return this;
  }

  public Integer getLimit() {
    return limit;
  }

  public DataExportParams setLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  public IdSchemes getOutputIdSchemes() {
    return outputIdSchemes;
  }

  public DataExportParams setOutputIdSchemes(IdSchemes outputIdSchemes) {
    this.outputIdSchemes = outputIdSchemes;
    return this;
  }

  public BlockingQueue<DeflatedDataValue> getBlockingQueue() {
    return blockingQueue;
  }

  public DataExportParams setBlockingQueue(BlockingQueue<DeflatedDataValue> blockingQueue) {
    this.blockingQueue = blockingQueue;
    return this;
  }
}
