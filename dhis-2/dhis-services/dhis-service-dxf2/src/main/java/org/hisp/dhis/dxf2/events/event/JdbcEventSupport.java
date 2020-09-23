package org.hisp.dhis.dxf2.events.event;

import com.vividsolutions.jts.geom.Geometry;
import lombok.experimental.UtilityClass;
import org.postgis.PGgeometry;

import java.sql.SQLException;
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

    PGgeometry toGeometry( Geometry geometry ) throws SQLException
    {
        return geometry != null ? new PGgeometry( geometry.toText() ) : null;
    }
}
