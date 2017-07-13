package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataset.notifications.DataSetNotificationTemplate;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;

/**
 * Created by zubair on 29.06.17.
 */
public class DataSetNotificationTemplateObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Override
    public <T extends IdentifiableObject> void preCreate( T object, ObjectBundle bundle )
    {
        if ( !DataSetNotificationTemplate.class.isInstance( object ) ) return;

        DataSetNotificationTemplate template = (DataSetNotificationTemplate) object;

        preProcess( template );
    }

    @Override
    public <T extends IdentifiableObject> void preUpdate( T object, T persistedObject, ObjectBundle bundle )
    {
        if ( !DataSetNotificationTemplate.class.isInstance( object ) ) return;

        DataSetNotificationTemplate template = (DataSetNotificationTemplate) object;

        preProcess( template );
    }

    @Override
    public <T extends IdentifiableObject> void postCreate( T persistedObject, ObjectBundle bundle )
    {
        if ( !DataSetNotificationTemplate.class.isInstance( persistedObject ) ) return;

        DataSetNotificationTemplate template = (DataSetNotificationTemplate) persistedObject;

        postProcess( template );
    }

    @Override
    public <T extends IdentifiableObject> void postUpdate( T persistedObject, ObjectBundle bundle )
    {
        if ( !DataSetNotificationTemplate.class.isInstance( persistedObject ) ) return;

        DataSetNotificationTemplate template = (DataSetNotificationTemplate) persistedObject;

        postProcess( template );
    }

    private void preProcess( DataSetNotificationTemplate template )
    {

    }

    private void postProcess( DataSetNotificationTemplate template )
    {

    }
}
