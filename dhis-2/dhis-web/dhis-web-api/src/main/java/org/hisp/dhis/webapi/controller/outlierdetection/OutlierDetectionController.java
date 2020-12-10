package org.hisp.dhis.webapi.controller.outlierdetection;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.outlierdetection.OutlierDetectionQuery;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierDetectionService;
import org.hisp.dhis.outlierdetection.OutlierValueResponse;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@PreAuthorize( "hasRole('ALL') or hasRole('F_RUN_VALIDATION')" )
public class OutlierDetectionController
{
    private final OutlierDetectionService outlierService;

    public OutlierDetectionController( OutlierDetectionService outlierService )
    {
        this.outlierService = outlierService;
    }

    @GetMapping( value = "/outlierDetection" )
    public OutlierValueResponse getOutliers( OutlierDetectionQuery query )
    {
        OutlierDetectionRequest request = outlierService.fromQuery( query );

        return outlierService.getOutliers( request );
    }
}
