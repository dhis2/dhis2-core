package org.hisp.dhis.analytics;

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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.*;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Service which provides methods for assembling DataQueryParams objects.
 *
 * @author Lars Helge Overland
 */
public interface DataQueryService
{
    /**
     * Creates a data query parameter object from the given URL.
     *
     * @param dimensionParams the dimension URL parameters.
     * @param filterParams the filter URL parameters.
     * @param aggregationType the aggregation type.
     * @param measureCriteria the measure criteria.
     * @param skipMeta whether to skip the meta data part of the response.
     * @param skipData whether to skip the data part of the response.
     * @param skipRounding whether to skip rounding and provide full precision
     *        for values.
     * @param completedOnly whether to only include completed events.
     * @param hierarchyMeta whether to include meta data about the organisation
     *        units in the hierarchy.
     * @param ignoreLimit whether to ignore the max number of cells limit.
     * @param hideEmptyRows whether to hide rows without data values, applies to
     *        table layout.
     * @param showHierarchy whether to show the organisation unit hierarchy
     *        together with the name.
     * @param includeNumDen whether to include the numerator and denominator of
     *        values in the response.
     * @param displayProperty the property to display for meta-data.
     * @param outputIdScheme the identifier scheme to use in the query response.
     * @param inputIdScheme the identifier scheme to interpret dimension and filters.
     * @param approvalLevel the approval level identifier.
     * @param relativePeriodDate the date to use as basis for relative periods.
     * @param userOrgUnit the user organisation unit to use, overrides current user.
     * @param format the i18n format.
     * @return a data query parameter object created based on the given URL info.
     */
    DataQueryParams getFromUrl( Set<String> dimensionParams, Set<String> filterParams, AggregationType aggregationType, String measureCriteria,
        boolean skipMeta, boolean skipData, boolean skipRounding, boolean completedOnly, boolean hierarchyMeta, boolean ignoreLimit,
        boolean hideEmptyRows, boolean showHierarchy, boolean includeNumDen, DisplayProperty displayProperty, IdentifiableProperty outputIdScheme, IdScheme inputIdScheme,
        String approvalLevel, Date relativePeriodDate, String userOrgUnit, I18nFormat format );

    /**
     * Creates a data query parameter object from the given BaseAnalyticalObject.
     *
     * @param object the BaseAnalyticalObject.
     * @return a data query parameter object created based on the given BaseAnalyticalObject.
     */
    DataQueryParams getFromAnalyticalObject( AnalyticalObject object );

    /**
     * Creates a list of DimensionalObject from the given set of dimension params.
     *
     * @param dimensionParams the dimension URL params.
     * @param relativePeriodDate the date to use as basis for relative periods.
     * @param userOrgUnit the user organisation unit param, overrides current
     *        user, can be null.
     * @param format the i18n format.
     * @param inputIdScheme the identifier scheme to interpret dimension and filters.
     * @return a list of DimensionalObject.
     */
    List<DimensionalObject> getDimensionalObjects( Set<String> dimensionParams, Date relativePeriodDate, String userOrgUnit, I18nFormat format, IdScheme inputIdScheme );

    /**
     * Returns a persisted DimensionalObject generated from the given  dimension
     * identifier and list of dimension options.
     *
     * For the pe dimension items, relative periods represented by enums will be
     * replaced by real ISO periods relative to the current date. For the ou
     * dimension items, the user  organisation unit enums
     * USER_ORG_UNIT|USER_ORG_UNIT_CHILDREN will be replaced by the persisted
     * organisation units for the current user.
     *
     * @param dimension the dimension identifier.
     * @param items the dimension items.
     * @param relativePeriodDate the date to use for generating relative periods, can be null.
     * @param userOrgUnits the list of user organisation units, overrides current
     *        user, can be null.
     * @param format the I18nFormat, can be null.
     * @param allowNull return null if no dimension was found.
     * @param inputIdScheme the identifier scheme to interpret dimension and filters.
     * @throws IllegalQueryException if no dimensions was found.
     * @return list of DimensionalObjects.
     */
    DimensionalObject getDimension( String dimension, List<String> items, Date relativePeriodDate,
        List<OrganisationUnit> userOrgUnits, I18nFormat format, boolean allowNull, IdScheme inputIdScheme );

    /**
     * Returns a list of user organisation units, looking first at the given user
     * org unit parameter, second at the organisation units associated with the
     * current user. Returns an empty list if no organisation units are found.
     *
     * @param params the data query parameters.
     * @param userOrgUnit the user org unit parameter string.
     * @return a list of organisation units.
     */
    List<OrganisationUnit> getUserOrgUnits( DataQueryParams params, String userOrgUnit );
}
