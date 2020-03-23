package org.hisp.dhis.db.migration.v34;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Statement;

public class V2_34_22__Add_teav_btree_index
    extends BaseJavaMigration
{

    private final String COP_POST_URL = "https://community.dhis2.org/t/draft-important-database-upgrade-for-tracker-performance/38766";

    @Override
    public void migrate( Context context )
        throws Exception
    {
        try (Statement statement = context.getConnection().createStatement())
        {
            statement
                .execute( "alter table trackedentityattributevalue alter column value set data type varchar(1200)" );
            statement.execute(
                "create index in_trackedentity_attribute_value on trackedentityattributevalue using btree (trackedentityattributeid, lower(value)) " );
        }
        catch ( SQLException sqlException )
        {
            StringBuilder stringBuilder = new StringBuilder(  );

            stringBuilder
                .append( "Could not perform upgrade of table 'trackedentityattributevalue'. " )
                .append( "Column 'value' should be altered to data type varchar(1200) and receive a new index. " )
                .append( "For more information, please see the following post: '" )
                .append( COP_POST_URL )
                .append( "'. " )
                .append( "Error message was: " )
                .append( sqlException.getMessage() );

            throw new Exception( stringBuilder.toString() );
        }
    }
}
