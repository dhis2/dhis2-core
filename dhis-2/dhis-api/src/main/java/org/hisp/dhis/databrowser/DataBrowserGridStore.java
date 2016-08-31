package org.hisp.dhis.databrowser;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.List;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.period.PeriodType;

/**
 * Contains methods for creating aggregated count queries for the Data Browser
 * module.
 * 
 * @author joakibj, martinwa, briane, eivinhb
 * 
 */
public interface DataBrowserGridStore
{
    String ID = DataBrowserGridStore.class.getName();

    // -------------------------------------------------------------------------
    // DataBrowser
    // -------------------------------------------------------------------------

    /**
     * Finds all DataSets connected to any period in betweenPeriodIds and does
     * an aggregated count.
     * 
     * @param betweenPeriodIds list of Period ids
     * @param periodType
     * @param isZeroAdded if true then Zero can be added and false is not
     * @return the Grid with structure for presentation
     */
    Grid getDataSetsBetweenPeriods( List<Integer> betweenPeriodIds, PeriodType periodType, boolean isZeroAdded );

    /**
     * Finds all DataElementGroups connected to any period in betweenPeriodIds
     * and does an aggregated count.
     * 
     * @param betweenPeriodIds list of Period ids
     * @param isZeroAdded if true then Zero can be added and false is not
     * @return the Grid with structure for presentation
     */
    Grid getDataElementGroupsBetweenPeriods( List<Integer> betweenPeriodIds, boolean isZeroAdded );

    /**
     * Finds all OrganisationUnitGroups connected to any period in
     * betweenPeriodIds and does an aggregated count.
     * 
     * @param betweenPeriodIds list of Period ids
     * @param isZeroAdded if true then Zero can be added and false is not
     * @return the Grid with structure for presentation
     */
    Grid getOrgUnitGroupsBetweenPeriods( List<Integer> betweenPeriodIds, boolean isZeroAdded );

    /**
     * Always called first.
     * 
     * Sets the structure in DataBrowserTable for DataElements. Finds all
     * DataSets with DataValue in betweenPeriod List and given DataSetId. Then
     * calls on helpers internally to set it up.
     * 
     * @param grid the Grid to set the structure in
     * @param dataSetId the DataSet id
     * @param metaIds list of MetaValue ids
     */
    void setDataElementStructureForDataSet( Grid grid, Integer dataSetId, List<Integer> metaIds );

    /**
     * Always called first.
     * 
     * Sets the structure in DataBrowserTable for DataElements. Finds all
     * DataElementGroups with DataValue in betweenPeriod List and given
     * DataElementGroupId. Then calls on helpers internally to set it up.
     * 
     * @param grid the Grid to set the structure in
     * @param dataElementGroupId the DataElementGroup id
     * @param metaIds list of MetaValue ids
     */
    void setDataElementStructureForDataElementGroup( Grid grid, Integer dataElementGroupId, List<Integer> metaIds );

    /**
     * Always called first.
     * 
     * Sets the structure in DataBrowserTable for DataElementGroups. Finds all
     * OrganisationUnitGroups with DataValue in betweenPeriod List and given
     * OrganisationUnitGroup id. Then calls on helpers in DataBrowserTable to
     * set it up.
     * 
     * @param grid the Grid to set the structure in
     * @param orgUnitGroupId the OrganisationUnitGroup id
     * @param metaIds list of MetaValue ids
     */
    void setDataElementGroupStructureForOrgUnitGroup( Grid grid, Integer orgUnitGroupId, List<Integer> metaIds );

    /**
     * Always called first.
     * 
     * Sets the structure in DataBrowserTable for OrgUnits. Finds all
     * OrganisationUnits with DataValues in betweenPeriod List and given
     * OrganisationUnit parent id. Then calls on helpers in DataBrowserTable to
     * set it up.
     * 
     * @param grid the Grid to set the structure in
     * @param orgUnitParent the OrganisationUnit parent id
     * @param metaIds list of MetaValue ids
     */
    void setStructureForOrgUnit( Grid grid, Integer orgUnitParent, List<Integer> metaIds );

    /**
     * Always called first.
     * 
     * Sets the structure in DataBrowserTable for DataElements. Finds all
     * OrganisationUnits with DataValue in betweenPeriod List and given
     * OrganisationUnit id. Then calls on helpers in DataBrowserTable to set it
     * up.
     * 
     * @param grid the Grid to set the structure in
     * @param orgUnitId the OrganisationUnit id
     * @param metaIds List of MetaValue ids
     */
    void setDataElementStructureForOrgUnit( Grid grid, Integer orgUnitId, List<Integer> metaIds );

    /**
     * Sets DataElement count-Columns in DataBrowserTable for betweenPeriod List
     * connected to one DataSet.
     * 
     * @param grid the Grid to insert column into
     * @param dataSetId id of DataSet the DataElements are for
     * @param periodType the type of period
     * @param betweenPeriodIds List of Period ids
     * @param metaIds List of MetaValue ids
     * @param isZeroAdded if true then Zero can be added and false is not
     * @return 0 if no results are found else number of rows inserted
     */
    Integer setCountDataElementsForDataSetBetweenPeriods( Grid grid, Integer dataSetId, PeriodType periodType,
        List<Integer> betweenPeriodIds, List<Integer> metaIds, boolean isZeroAdded );

    /**
     * Sets DataElement count-Columns in DataBrowserTable for betweenPeriod List
     * connected to one DataElementGroup.
     * 
     * @param grid the Grid to insert column into
     * @param dataElementGroupId id of DataElementGroup the DataElements are for
     * @param betweenPeriodIds list of Period ids
     * @param metaIds List of MetaValue ids
     * @param isZeroAdded if true then Zero can be added and false is not
     * @return 0 if no results are found else number of rows inserted
     */
    Integer setCountDataElementsForDataElementGroupBetweenPeriods( Grid grid, Integer dataElementGroupId,
        List<Integer> betweenPeriodIds, List<Integer> metaIds, boolean isZeroAdded );

    /**
     * Sets the DataElementGroup count-Columns in DataBrowserTable for
     * betweenPeriod List connected to one OrgUnitGroup.
     * 
     * @param grid the Grid to insert column into
     * @param orgUnitGroupId id of OrgUnitGroup the DataElementGroups are for
     * @param betweenPeriodIds list of Period ids
     * @param metaIds List of MetaValue ids
     * @param isZeroAdded if true then Zero can be added and false is not
     * @return 0 if no results are found else number of rows inserted
     */
    Integer setCountDataElementGroupsForOrgUnitGroupBetweenPeriods( Grid grid, Integer orgUnitGroupId,
        List<Integer> betweenPeriodIds, List<Integer> metaIds, boolean isZeroAdded );

    /**
     * Sets OrgUnit count-Columns in DataBrowserTable for betweenPeriod List
     * connected to one OrganisationUnit parent.
     * 
     * @param grid the Grid to insert column into
     * @param orgUnitParent the OrganisationUnit parent id
     * @param betweenPeriodIds list of Period ids
     * @param maxLevel is the max level of the hierarchy
     * @param metaIds List of MetaValue ids
     * @param isZeroAdded if true then Zero can be added and false is not
     * @return 0 if no results are found else number of rows inserted
     */
    Integer setCountOrgUnitsBetweenPeriods( Grid grid, Integer orgUnitParent, List<Integer> betweenPeriodIds,
        Integer maxLevel, List<Integer> metaIds, boolean isZeroAdded );

    /**
     * Sets DataElement count-Columns in DataBrowserTable for betweenPeriod List
     * connected to one OrgUnit.
     * 
     * @param grid the Grid to insert column into
     * @param orgUnitId id of OrganisationUnit the DataElements are for
     * @param betweenPeriodIds list of Period ids
     * @param metaIds List of MetaValue ids
     * @param isZeroAdded if true then Zero can be added and false is not
     * @return 0 if no results are found else number of rows inserted
     */
    Integer setRawDataElementsForOrgUnitBetweenPeriods( Grid grid, Integer orgUnitId, List<Integer> betweenPeriodIds,
        List<Integer> metaIds, boolean isZeroAdded );

}
