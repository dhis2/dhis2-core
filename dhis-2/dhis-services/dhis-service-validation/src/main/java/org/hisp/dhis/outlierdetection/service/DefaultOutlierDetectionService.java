/*
 * Copyright (c) 2004-2020, University of Oslo
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

package org.hisp.dhis.outlierdetection.service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionMetadata;
import org.hisp.dhis.outlierdetection.OutlierDetectionQuery;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierDetectionService;
import org.hisp.dhis.outlierdetection.OutlierDetectionResponse;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service
public class DefaultOutlierDetectionService
    implements OutlierDetectionService
{
    private static final int MAX_LIMIT = 10_000;

    private final IdentifiableObjectManager idObjectManager;

    private final OutlierDetectionManager outlierDetectionManager;

    public DefaultOutlierDetectionService( IdentifiableObjectManager idObjectManager,
        OutlierDetectionManager outlierDetectionManager )
    {
        this.idObjectManager = idObjectManager;
        this.outlierDetectionManager = outlierDetectionManager;
    }

    @Override
    public void validate( OutlierDetectionRequest request )
        throws IllegalQueryException
    {
        ErrorMessage error = validateForErrorMessage( request );

        if ( error != null )
        {
            log.warn( String.format(
                "Outlier detection request validation failed, code: '%s', message: '%s'",
                error.getErrorCode(), error.getMessage() ) );

            throw new IllegalQueryException( error );
        }
    }

    @Override
    public ErrorMessage validateForErrorMessage( OutlierDetectionRequest request )
    {
        ErrorMessage error = null;

        if ( request.getDataElements().isEmpty() )
        {
            error = new ErrorMessage( ErrorCode.E2200 );
        }

        if ( request.getStartDate() == null || request.getEndDate() == null )
        {
            error = new ErrorMessage( ErrorCode.E2201 );
        }

        if ( request.getOrgUnits().isEmpty() )
        {
            error = new ErrorMessage( ErrorCode.E2202 );
        }

        if ( request.getThreshold() <= 0 )
        {
            error = new ErrorMessage( ErrorCode.E2203 );
        }

        if ( request.getMaxResults() <= 0 )
        {
            error = new ErrorMessage( ErrorCode.E2204 );
        }

        if ( request.getMaxResults() > MAX_LIMIT )
        {
            error = new ErrorMessage( ErrorCode.E2205, MAX_LIMIT );
        }

        return error;
    }

    @Override
    public OutlierDetectionRequest fromQuery( OutlierDetectionQuery query )
    {
        OutlierDetectionRequest.Builder request = new OutlierDetectionRequest.Builder();

        List<DataSet> dataSets = idObjectManager.getByUid( DataSet.class, query.getDs() );
        List<DataElement> dataElements = idObjectManager.getByUid( DataElement.class, query.getDe() );
        List<OrganisationUnit> orgUnits = idObjectManager.getByUid( OrganisationUnit.class, query.getOu() );

        dataElements.addAll( dataSets.stream()
            .map( ds -> ds.getDataElements() )
            .flatMap( Collection::stream )
            .filter( de -> de.getValueType().isNumeric() )
            .collect( Collectors.toList() ) );

        request
            .withDataElements( dataElements )
            .withStartEndDate( query.getStartDate(), query.getEndDate() )
            .withOrgUnits( orgUnits );

        if ( query.getThreshold() != null )
        {
            request.withThreshold( query.getThreshold() );
        }

        if ( query.getOrderBy() != null )
        {
            request.withOrderBy( query.getOrderBy() );
        }

        if ( query.getMaxResults() != null )
        {
            request.withMaxResults( query.getMaxResults() );
        }

        return request.build();
    }

    @Override
    public OutlierDetectionResponse getOutliers( OutlierDetectionRequest request )
    {
        validate( request );

        final OutlierDetectionResponse response = new OutlierDetectionResponse();
        response.setOutlierValues( outlierDetectionManager.getOutliers( request ) );

        final OutlierDetectionMetadata metadata = new OutlierDetectionMetadata();
        metadata.setCount( response.getOutlierValues().size() );
        metadata.setThreshold( request.getThreshold() );
        metadata.setOrderBy( request.getOrderBy() );
        metadata.setMaxResults( request.getMaxResults() );
        response.setMetadata( metadata );

        return response;
    }
}
