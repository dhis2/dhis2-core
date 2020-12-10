package org.hisp.dhis.outlierdetection;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorMessage;

public interface OutlierDetectionService
{
    /**
     * Validates the request.
     *
     * @param request the {@link OutlierDetectionRequest}.
     * @throws IllegalQueryException if request is invalid.
     */
    void validate( OutlierDetectionRequest params )
        throws IllegalQueryException;

    /**
     * Validates the request.
     *
     * @param request the {@link OutlierDetectionRequest}.
     * @return an {@link ErrorMessage} if request is invalid, or null if valid.
     */
    ErrorMessage validateForErrorMessage( OutlierDetectionRequest request );

    /**
     * Creates a {@link OutlierDetectionRequest} from the given query.
     *
     * @param query the {@link OutlierDetectionQuery}.
     * @return a {@link OutlierDetectionRequest}.
     */
    OutlierDetectionRequest fromQuery( OutlierDetectionQuery query );

    /**
     * Returns outlier values.
     *
     * @param request the {@link OutlierDetectionRequest}.
     * @return a {@link OutlierValueResponse}.
     */
    OutlierValueResponse getOutliers( OutlierDetectionRequest request );
}
