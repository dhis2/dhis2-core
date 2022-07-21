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

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
@Builder( toBuilder = true )
public class CommonQueryRequest
{

    @Builder.Default
    private Set<String> program = new LinkedHashSet<>();

    private String userOrgUnit;

    /**
     * The dimensions to be returned/filtered at.
     */
    @Builder.Default
    private Set<String> dimension = new LinkedHashSet<>();

    /**
     * The filters to be applied at querying time.
     */
    @Builder.Default
    private Set<String> filter = new HashSet<>();

    /**
     * When set, the headers in the response object will match the specified
     * headers in the respective order. As the headers should not be duplicated,
     * this is represented as Set.
     */
    @Builder.Default
    private Set<String> headers = new LinkedHashSet<>();

    private OrganisationUnitSelectionMode ouMode;

    @Builder.Default
    private Set<String> asc = new HashSet<>();

    @Builder.Default
    private Set<String> desc = new HashSet<>();

    @Builder.Default
    private IdScheme dataIdScheme = IdScheme.UID;

    /**
     * When set to true, this will count the total of pages related to the
     * current query. If enabled this might cause performance issues depending
     * on the result size.
     */
    private boolean totalPages;

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
     * Custom date filters
     */
    private String eventDate;

    private String enrollmentDate;

    private String scheduledDate;

    private String incidentDate;

    private String lastUpdated;

    public boolean hasHeaders()
    {
        return isNotEmpty( getHeaders() );
    }
}
