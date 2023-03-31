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
package org.hisp.dhis.analytics.common.params;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;

/**
 * This is a reusable and shared representation of queryable items to be used by
 * the service/dao layers.
 *
 * It encapsulates the most common objects that are very likely to be used by
 * the majority of analytics queries.
 */
@Getter
@Setter
@Builder( toBuilder = true )
public class CommonParams
{
    /**
     * The list of Program objects carried on by this object.
     */
    @Builder.Default
    private final List<Program> programs = new ArrayList<>();

    /**
     * Data structure containing dimensionParams, which can represent
     * dimensions, filters, queryItems or queryItemFilters.
     */
    @Builder.Default
    private final List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers = new ArrayList<>();

    /**
     * Data structure containing headers. If present, they will represent the
     * columns to be retrieved. Cannot be repeated and should keep ordering,
     * hence a {@link LinkedHashSet}.
     */
    @Builder.Default
    private final Set<String> headers = new LinkedHashSet<>();

    /**
     * The object that groups the paging and sorting parameters.
     */
    @Builder.Default
    private final AnalyticsPagingParams pagingParams = AnalyticsPagingParams.builder().build();

    /**
     * List of sorting params.
     */
    @Builder.Default
    private final List<AnalyticsSortingParams> orderParams = Collections.emptyList();

    /**
     * The coordinate fields to use as basis for spatial event analytics. The
     * list is built as collection of coordinate field and fallback fields. The
     * order defines priority of geometry fields.
     */
    @Builder.Default
    private final List<String> coordinateFields = Collections.emptyList();

    /**
     * The dimensional object for which to produce aggregated data.
     */
    private final DimensionalItemObject value;

    /**
     * Indicates which property to display.
     */
    private final DisplayProperty displayProperty;

    /**
     * The user's organization unit.
     */
    @Builder.Default
    private final List<OrganisationUnit> userOrgUnit = Collections.emptyList();

    /**
     * The mode of selecting organisation units. Default is DESCENDANTS, meaning
     * all subunits in the hierarchy. CHILDREN refers to immediate children in
     * the hierarchy; SELECTED refers to the selected organisation units only.
     */
    @Builder.Default
    private final OrganisationUnitSelectionMode ouMode = DESCENDANTS;

    /**
     * Id scheme to be used for data, more specifically data elements and
     * attributes which have an option set or legend set, e.g. return the name
     * of the option instead of the code, or the name of the legend instead of
     * the legend ID, in the data response.
     */
    @Builder.Default
    private final IdScheme dataIdScheme = UID;

    /**
     * The general id scheme, which drives the values in the response object.
     */
    private final IdScheme outputIdScheme;

    /**
     * The id scheme specific for data elements.
     */
    private final IdScheme outputDataElementIdScheme;

    /**
     * The id scheme specific for org units.
     */
    private final IdScheme outputOrgUnitIdScheme;

    /**
     * Overrides the start date of the relative period. e.g: "2016-01-01".
     */
    private final Date relativePeriodDate;

    /**
     * Indicates if the metadata element should be omitted from the response.
     */
    private final boolean skipMeta;

    /**
     * Indicates if the data should be omitted from the response.
     */
    private final boolean skipData;

    /**
     * Indicates if the headers should be omitted from the response.
     */
    private final boolean skipHeaders;

    /**
     * Indicates if full precision should be provided for numeric values.
     */
    private final boolean skipRounding;

    /**
     * Indicates if full metadata details should be provided.
     */
    private final boolean includeMetadataDetails;

    /**
     * Indicates if organization unit hierarchy should be provided.
     */
    private final boolean hierarchyMeta;

    /**
     * Indicates if additional ou hierarchy data should be provided.
     */
    private final boolean showHierarchy;

    /**
     * weather the query should consider only items with lat/long coordinates
     */
    private boolean coordinatesOnly;

    /**
     * weather the query should consider only items with geometry
     */
    private boolean geometryOnly;

    /**
     * Indicates whether this query defines a master identifier scheme different
     * from the default (UID).
     */
    public boolean isGeneralOutputIdSchemeSet()
    {
        return outputIdScheme != null && !UID.equals( outputIdScheme );
    }

    /**
     * Indicates whether this query defines a master identifier scheme different
     * from the default (UID).
     */
    public boolean isOutputDataElementIdSchemeSet()
    {
        return outputDataElementIdScheme != null && !UID.equals( outputDataElementIdScheme );
    }

    /**
     * Indicates whether this query defines a master identifier scheme different
     * from the default (UID).
     */
    public boolean isOutputOrgUnitIdSchemeSet()
    {
        return outputOrgUnitIdScheme != null && !UID.equals( outputOrgUnitIdScheme );
    }

    /**
     * Indicates whether a non-default identifier scheme is specified.
     */
    public boolean hasCustomIdSchemaSet()
    {
        return isGeneralOutputIdSchemeSet() || isOutputDataElementIdSchemeSet() || isOutputOrgUnitIdSchemeSet();
    }

    public List<DimensionIdentifier<DimensionParam>> getDimensionIdentifiers()
    {
        return emptyIfNull( dimensionIdentifiers ).stream().filter( Objects::nonNull ).collect( toList() );
    }

    /**
     * Gets a new instance of the internal delegator object.
     *
     * @return an instance of {@link CommonParamsDelegator}.
     */
    public CommonParamsDelegator delegate()
    {
        return new CommonParamsDelegator( getDimensionIdentifiers() );
    }
}
