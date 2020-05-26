package org.hisp.dhis.trackedentity;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Component( "org.hisp.dhis.trackedentity.TrackedEntityTypeDeletionHandler" )
public class TrackedEntityTypeDeletionHandler extends DeletionHandler
{

    private final IdentifiableObjectManager idObjectManager;

    public TrackedEntityTypeDeletionHandler(
        IdentifiableObjectManager idObjectManager )
    {
        checkNotNull( idObjectManager );

        this.idObjectManager = idObjectManager;
    }

    @Override
    protected String getClassName()
    {
        return TrackedEntityType.class.getSimpleName();
    }

    @Override
    public void deleteTrackedEntityAttribute( TrackedEntityAttribute trackedEntityAttribute )
    {
        Collection<TrackedEntityType> trackedEntityTypes = idObjectManager.getAllNoAcl( TrackedEntityType.class );

        for ( TrackedEntityType trackedEntityType : trackedEntityTypes )
        {

            List<TrackedEntityTypeAttribute> trackedEntityTypeAttributes = new ArrayList<>( trackedEntityType
                .getTrackedEntityTypeAttributes() );

            for ( TrackedEntityTypeAttribute trackedEntityTypeAttribute : trackedEntityTypeAttributes )
            {

                if ( trackedEntityTypeAttribute.getTrackedEntityAttribute().equals( trackedEntityAttribute ) )
                {
                    trackedEntityType.getTrackedEntityTypeAttributes().remove( trackedEntityTypeAttribute );
                    idObjectManager.updateNoAcl( trackedEntityType );
                }

            }
        }
    }
}
