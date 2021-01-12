package org.hisp.dhis.dataanalysis;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.*;
import java.util.stream.Collectors;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@Service( "org.hisp.dhis.dataanalysis.FollowupAnalysisService" )
public class DefaultFollowupAnalysisService
    implements FollowupAnalysisService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final DataAnalysisStore dataAnalysisStore;

    public DefaultFollowupAnalysisService( DataAnalysisStore dataAnalysisStore )
    {
        checkNotNull( dataAnalysisStore );
        this.dataAnalysisStore = dataAnalysisStore;
    }

    // -------------------------------------------------------------------------
    // FollowupAnalysisService implementation
    // -------------------------------------------------------------------------

    @Override
    public List<DeflatedDataValue> getFollowupDataValues( Collection<OrganisationUnit> parents,
        Collection<DataElement> dataElements, Collection<Period> periods, int limit )
    {
        if ( parents == null || parents.size() == 0 || limit < 1 )
        {
            return new ArrayList<>();
        }

        Set<DataElement> elements = dataElements.stream()
            .filter( de -> ValueType.NUMERIC_TYPES.contains( de.getValueType() ) )
            .collect( Collectors.toSet() );

        Set<CategoryOptionCombo> categoryOptionCombos = new HashSet<>();

        for ( DataElement dataElement : elements )
        {
            categoryOptionCombos.addAll( dataElement.getCategoryOptionCombos() );
        }

        log.debug( "Starting min-max analysis, no of data elements: " + elements.size() + ", no of parent org units: " + parents.size() );

        return dataAnalysisStore.getFollowupDataValues( elements, categoryOptionCombos, periods, parents, limit );
    }
}
