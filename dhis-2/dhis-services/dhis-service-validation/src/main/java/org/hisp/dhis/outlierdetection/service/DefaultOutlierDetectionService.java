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
import org.hisp.dhis.outlierdetection.OutlierDetectionQuery;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierDetectionService;
import org.hisp.dhis.outlierdetection.OutlierValue;
import org.hisp.dhis.outlierdetection.OutlierValueResponse;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DefaultOutlierDetectionService
    implements OutlierDetectionService
{
    private final IdentifiableObjectManager idObjectManager;
    private final OutlierDetectionManager outlierDetectionManager;

    public DefaultOutlierDetectionService(
        IdentifiableObjectManager idObjectManager, OutlierDetectionManager outlierDetectionManager )
    {
        this.idObjectManager = idObjectManager;
        this.outlierDetectionManager = outlierDetectionManager;
    }

    /**
     * Validates the request.
     *
     * @param request the {@link OutlierDetectionRequest}.
     * @throws IllegalQueryException if request is invalid.
     */
    public void validate( OutlierDetectionRequest params )
        throws IllegalQueryException
    {
        ErrorMessage error = validateForErrorMessage( params );

        if ( error != null )
        {
            log.warn( String.format(
                "Outlier detection validation failed, code: '%s', message: '%s'",
                error.getErrorCode(), error.getMessage() ) );

            throw new IllegalQueryException( error );
        }
    }

    /**
     * Validates the request.
     *
     * @param request the {@link OutlierDetectionRequest}.
     * @return an {@link ErrorMessage} if request is invalid, or null if valid.
     */
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

        return error;
    }

    /**
     * Creates a {@link OutlierDetectionRequest} from the given query.
     *
     * @param query the {@link OutlierDetectionQuery}.
     * @return a {@link OutlierDetectionRequest}.
     */
    public OutlierDetectionRequest fromQuery( OutlierDetectionQuery query )
    {
        OutlierDetectionRequest.Builder request = new OutlierDetectionRequest.Builder();

        List<DataSet> dataSets = idObjectManager.getByUid( DataSet.class, query.getDs() );

        List<DataElement> dataElements = dataSets.stream()
            .map( ds -> ds.getDataElements() )
            .flatMap( Collection::stream )
            .collect( Collectors.toList() );

        request
            .withDataElements( dataElements )
            .withStartEndDate( query.getStartDate(), query.getEndDate() )
            .withOrgUnits( idObjectManager.getByUid( OrganisationUnit.class, query.getOu() ) );

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

    /**
     * Returns outlier data values.
     *
     * @param request the {@link OutlierDetectionRequest}.
     * @return a {@link OutlierValueResponse}.
     */
    public OutlierValueResponse getOutliers( OutlierDetectionRequest request )
    {
        OutlierValueResponse response = new OutlierValueResponse();
        List<OutlierValue> outlierValues = outlierDetectionManager.getOutliers( request );
        response.setOutlierValues( outlierValues );
        return response;
    }
}
