package org.hisp.dhis.outlierdetection.service;

import org.hisp.dhis.outlierdetection.OutlierDetectionService;
import org.springframework.stereotype.Service;

@Service
public class DefaultOutlierDetectionService
    implements OutlierDetectionService
{
    private final OutlierDetectionManager manager;

    public DefaultOutlierDetectionService( OutlierDetectionManager manager )
    {
        this.manager = manager;
    }

}
