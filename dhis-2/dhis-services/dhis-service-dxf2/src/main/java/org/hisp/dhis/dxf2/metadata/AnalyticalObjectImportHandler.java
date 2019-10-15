package org.hisp.dhis.dxf2.metadata;

import org.hibernate.Session;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.schema.Schema;

public interface AnalyticalObjectImportHandler
{
    void handleAnalyticalObject( Session session, Schema schema, BaseAnalyticalObject analyticalObject, ObjectBundle bundle );
}