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
package org.hisp.dhis.dataanalysis;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.commons.collection.CollectionUtils.isEmpty;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@Service( "org.hisp.dhis.dataanalysis.FollowupAnalysisService" )
@RequiredArgsConstructor
public class DefaultFollowupAnalysisService
    implements FollowupAnalysisService
{
    private static final int MAX_LIMIT = 10_000;

    private final DataAnalysisStore dataAnalysisStore;

    private final FollowupValueManager followupValueManager;

    private final CurrentUserService currentUserService;

    private final I18nManager i18nManager;

    @Override
    @Transactional( readOnly = true )
    public List<DeflatedDataValue> getFollowupDataValues( Collection<OrganisationUnit> parents,
        Collection<DataElement> dataElements, Collection<Period> periods, int limit )
    {
        if ( parents == null || parents.isEmpty() || limit < 1 )
        {
            return emptyList();
        }

        Set<DataElement> elements = dataElements.stream()
            .filter( de -> de.getValueType().isNumeric() )
            .collect( Collectors.toSet() );

        Set<CategoryOptionCombo> categoryOptionCombos = new HashSet<>();

        for ( DataElement dataElement : elements )
        {
            categoryOptionCombos.addAll( dataElement.getCategoryOptionCombos() );
        }

        log.debug( "Starting min-max analysis, no of data elements: " + elements.size() + ", no of parent org units: "
            + parents.size() );

        return dataAnalysisStore.getFollowupDataValues( elements, categoryOptionCombos, periods, parents, limit );
    }

    @Override
    @Transactional( readOnly = true )
    public FollowupAnalysisResponse getFollowupDataValues( FollowupAnalysisRequest request )
    {
        validate( request );

        List<FollowupValue> followupValues = followupValueManager
            .getFollowupDataValues( currentUserService.getCurrentUser(), request );

        I18nFormat format = i18nManager.getI18nFormat();
        followupValues.forEach( value -> value.setPeName( format.formatPeriod( value.getPeAsPeriod() ) ) );
        return new FollowupAnalysisResponse( new FollowupAnalysisMetadata( request ), followupValues );
    }

    private void validate( FollowupAnalysisRequest request )
    {
        if ( isEmpty( request.getDe() ) && isEmpty( request.getDs() ) )
        {
            throw validationError( ErrorCode.E2300 );
        }
        if ( isEmpty( request.getOu() ) )
        {
            throw validationError( ErrorCode.E2203 );
        }
        if ( (request.getStartDate() == null || request.getEndDate() == null) && request.getPe() == null )
        {
            throw validationError( ErrorCode.E2301 );
        }
        if ( request.getStartDate() != null && request.getEndDate() != null
            && !request.getStartDate().before( request.getEndDate() ) )
        {
            throw validationError( ErrorCode.E2202 );
        }
        if ( request.getMaxResults() <= 0 )
        {
            throw validationError( ErrorCode.E2205 );
        }
        if ( request.getMaxResults() > MAX_LIMIT )
        {
            throw validationError( ErrorCode.E2206, MAX_LIMIT );
        }
    }

    private IllegalQueryException validationError( ErrorCode error, Object... args )
    {
        ErrorMessage message = new ErrorMessage( error, args );
        log.warn( String.format( "Followup analysis request validation failed, code: '%s', message: '%s'",
            error, message.getMessage() ) );
        return new IllegalQueryException( message );
    }
}
