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
package org.hisp.dhis.analytics.common;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;

/**
 * This object wraps the common params/request attributes used across analytics
 * requests.
 *
 * Some objects are specified as Set/LinkedHashSet as they might require
 * enforcing ordering and avoid duplications.
 */
@Getter
@Setter
@With
@NoArgsConstructor
@AllArgsConstructor
public class CommonQueryRequest
{

    private Set<String> program = new LinkedHashSet<>();

    private String userOrgUnit;

    /**
     * The dimensions to be returned/filtered at.
     */
    private Set<String> dimension = new LinkedHashSet<>();

    /**
     * The filters to be applied at querying time.
     */
    private Set<String> filter = new HashSet<>();

    /**
     * When set, the headers in the response object will match the specified
     * headers in the respective order. As the headers should not be duplicated,
     * this is represented as Set.
     */
    private Set<String> headers = new LinkedHashSet<>();

    private OrganisationUnitSelectionMode ouMode;

    private Set<String> asc = new HashSet<>();

    private Set<String> desc = new HashSet<>();

    private IdScheme dataIdScheme = IdScheme.UID;

    private Date relativePeriodDate;

    /**
     * Indicates if the metadata element should be omitted from the response.
     */
    private boolean skipMeta;

    /**
     * Indicates if the data should be omitted from the response.
     */
    private boolean skipData;

    /**
     * Indicates if the headers should be omitted from the response.
     */
    private boolean skipHeaders;

    /**
     * Indicates if full precision should be provided for numeric values.
     */
    private boolean skipRounding;

    /**
     * Indicates if full metadata details should be provided.
     */
    private boolean includeMetadataDetails;

    /**
     * Indicates if organization unit hierarchy should be provided
     */
    private boolean hierarchyMeta;

    /**
     * Indicates if additional ou hierarchy data should be provided
     */
    private boolean showHierarchy;

    /**
     * Custom date filters
     */
    private String eventDate;

    private String enrollmentDate;

    private String scheduledDate;

    private String incidentDate;

    private String lastUpdated;

    /**
     * The page number. Default page is 1.
     */
    private Integer page = 1;

    /**
     * The page size.
     */
    private Integer pageSize = 50;

    /**
     * The paging parameter. When set to false we should not paginate. The
     * default is true (always paginate).
     */
    private boolean paging = true;

    /**
     * The paging parameter. When set to false we should not count total pages.
     * The default is false.
     */
    private boolean totalPages = false;
}
