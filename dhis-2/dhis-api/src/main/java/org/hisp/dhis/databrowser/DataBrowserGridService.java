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

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.period.PeriodType;

/**
 * @author Dang Duy hieu
 * @version $Id$
 */
public interface DataBrowserGridService
{
    String ID = DataBrowserGridService.class.getName();

    // -------------------------------------------------------------------------
    // DataBrowser
    // -------------------------------------------------------------------------

    /**
     * Method that retrieves - all DataSets with DataElement quantity - in a
     * given period and type (DataSet | Count)
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @param periodType the period type
     * @return Grid the Grid with structure for
     *         presentation
     */
    Grid getDataSetsInPeriod( String startDate, String endDate, PeriodType periodType, I18nFormat format,
        boolean isZeroAdded );

    /**
     * Method that retrieves - all DataElementGroups with DataElement quantity -
     * in a given period and type (DataElementGroup | Count)
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @param periodType the period type
     * @return Grid the Grid with structure for
     *         presentation
     */
    Grid getDataElementGroupsInPeriod( String startDate, String endDate, PeriodType periodType,
        I18nFormat format, boolean isZeroAdded );

    /**
     * Method that retrieves - all OrganisationUnitGroups with DataElement
     * quantity - in a given period and type (OrgUnitGroup | Count)
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @param periodType the period type
     * @return Grid the Grid with structure for
     *         presentation
     */
    Grid getOrgUnitGroupsInPeriod( String startDate, String endDate, PeriodType periodType,
        I18nFormat format, boolean isZeroAdded );

    /**
     * Method that retrieves - all OrganisationUnits with DataElement quantity -
     * in a given period - that is child of a given OrganisationUnit parent.
     * 
     * @param orgUnitParent the OrganisationUnit parent
     * @param startDate the start date
     * @param endDate the end date
     * @param periodType the period type
     * @param maxLevel is the max level of the hierarchy
     * @return Grid the Grid with structure for
     *         presentation
     */
    Grid getOrgUnitsInPeriod( Integer orgUnitParent, String startDate, String endDate,
        PeriodType periodType, Integer maxLevel, I18nFormat format, boolean isZeroAdded );

    /**
     * Method that retrieves - all the DataElements count - in a given period -
     * for a given DataSet and returns a Grid with the data.
     * 
     * @param dataSetId the DataSet id
     * @param startDate the start date
     * @param endDate the end date
     * @param periodType the period type
     * @return Grid the Grid with structure for
     *         presentation
     */
    Grid getCountDataElementsForDataSetInPeriod( Integer dataSetId, String startDate, String endDate,
        PeriodType periodType, I18nFormat format, boolean isZeroAdded );

    /**
     * Method that retrieves - all the DataElements count - in a given period -
     * for a given DataElementGroup and returns a Grid with the
     * data.
     * 
     * @param dataElementGroupId the DataElementGroup id
     * @param startDate the start date
     * @param endDate the end date
     * @param periodType the period type
     * @return Grid the Grid with structure for
     *         presentation
     */
    Grid getCountDataElementsForDataElementGroupInPeriod( Integer dataElementGroupId, String startDate,
        String endDate, PeriodType periodType, I18nFormat format, boolean isZeroAdded );

    /**
     * Method retrieves - all the DataElementGroups count - in a given period -
     * for a given OrganisationUnitGroup and returns a Grid with the
     * data.
     * 
     * @param orgUnitGroupId the OrganisationUnitGroup id
     * @param startDate the start date
     * @param endDate the end date
     * @param periodType the period type
     * @return Grid the Grid with structure for
     *         presentation
     */
    Grid getCountDataElementGroupsForOrgUnitGroupInPeriod( Integer orgUnitGroupId, String startDate,
        String endDate, PeriodType periodType, I18nFormat format, boolean isZeroAdded );

    /**
     * Method that retrieves - all the DataElements count - in a given period -
     * for a given OrganisationUnit and returns a Grid with the
     * data.
     * 
     * @param orgUnitId the OrganisationUnit id
     * @param startDate the start date
     * @param endDate the end date
     * @param periodType the period type
     * @return Grid the Grid with structure for
     *         presentation
     */
    Grid getRawDataElementsForOrgUnitInPeriod( Integer orgUnitId, String startDate, String endDate,
        PeriodType periodType, I18nFormat format, boolean isZeroAdded );

    /**
     * This method converts a string from the date format "yyyy-MM-dd" to "MMMM
     * yyyy", for instance.
     * 
     * @param date is the string to be converted.
     * @param periodService service of period
     * @param format is i18n format object
     * @return converted string if the date is valid, else the original string
     *         is returned
     */
    String convertDate( PeriodType periodType, String dateString, I18n i18n, I18nFormat format );

    /**
     * This method returns the string of name of periods in the list which
     * between fromDate and toDate input params
     * 
     * @param periodType is the type of period
     * @param fromDate the beginning date
     * @param toDate the end date
     * @param format is i18n format object
     * @return The name of periods in the list which between fromDate and toDate
     *         input params will be returned.
     */
    String getFromToDateFormat( PeriodType periodType, String fromDate, String toDate, I18nFormat format );

}
