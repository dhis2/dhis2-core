package org.hisp.dhis.outlierdetection.service;

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.springframework.beans.factory.annotation.Autowired;

public class OutlierDetectionSeviceTest
    extends IntegrationTestBase
{
    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }


}
