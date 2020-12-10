package org.hisp.dhis.webapi.controller.outlierdetection;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@PreAuthorize( "hasRole('ALL') or hasRole('F_RUN_VALIDATION')" )
public class OutlierDetectionController
{


}
