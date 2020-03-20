package org.hisp.dhis.db.migration.v34;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Statement;

public class V2_34_22__Add_teav_btree_index extends BaseJavaMigration
{
    private static final Logger log = LoggerFactory.getLogger( V2_34_22__Add_teav_btree_index.class );

    @Override
    public void migrate( Context context )
        throws Exception
    {
        try ( Statement statement = context.getConnection().createStatement() )
        {
            statement.execute( "create index in_attribute_value on trackedentityattributevalue using btree (trackedentityattributeid, lower(value)) " );
        }
        catch ( SQLException sqlException )
        {
            log.warn( "Could not add btree-index to table 'trackedentityattributevalue': " + sqlException.getMessage() );
        }
    }
}
