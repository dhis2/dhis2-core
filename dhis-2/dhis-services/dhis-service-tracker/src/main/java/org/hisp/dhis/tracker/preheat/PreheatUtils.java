package org.hisp.dhis.tracker.preheat;

import java.util.Optional;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;

import lombok.experimental.UtilityClass;

/**
 * @author Luciano Fiandesio
 */
@UtilityClass
public class PreheatUtils
{
    public <T extends IdentifiableObject> Optional<String> resolveKey( TrackerIdentifier identifier, T object )
    {
        if ( identifier.getIdScheme().equals( TrackerIdScheme.UID ) )
        {
            return Optional.ofNullable( object.getUid() );
        }
        else if ( identifier.getIdScheme().equals( TrackerIdScheme.CODE ) )
        {
            return Optional.ofNullable( object.getCode() );
        }
        else if ( identifier.getIdScheme().equals( TrackerIdScheme.NAME ) )
        {
            return Optional.ofNullable( object.getName() );
        }
        else if ( identifier.getIdScheme().equals( TrackerIdScheme.ATTRIBUTE ) )
        {
            return Optional.ofNullable( identifier.getIdentifier( object ) );
        }
        // TODO TrackerIdScheme.AUTO ??

        return Optional.empty();
    }
}
