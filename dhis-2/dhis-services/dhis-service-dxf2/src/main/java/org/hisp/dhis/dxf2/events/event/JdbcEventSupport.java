package org.hisp.dhis.dxf2.events.event;

import lombok.experimental.UtilityClass;

import java.sql.Timestamp;
import java.util.Date;

@UtilityClass
class JdbcEventSupport
{
    // Any chances we are duplicating this elsewhere ?
    Timestamp toTimestamp( Date date )
    {
        return date != null ? new Timestamp( date.getTime() ) : null;
    }
}
