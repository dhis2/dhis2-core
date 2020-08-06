package org.hisp.dhis.db.migration.v35;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Statement;

public class V2_35_13__Migrate_sharings_to_jsonb
    extends BaseJavaMigration
{
    private static final Logger log = LoggerFactory.getLogger( V2_35_13__Migrate_sharings_to_jsonb.class );

    @Override
    public void migrate( Context context )
    {
        // Loop through all metadata tables that has sharing enable
        // 1) Get all sharing data from useraccess and usergroupaccess
        // 2) select ua.access from UserAccess ua inner join DataElementUserAccess deua on ua.useraccessid = deua.useraccessid  inner join dataelement de on deua.dataelementid = de.dataelementid ;

        String tableName = "dataelement";
        String[] tableNames = {"dataelement", "dataset"};
        try ( Statement statement = context.getConnection().createStatement() )
        {

            String userQuery = " select ua.access as access, usr.uuid as id " +
                " from dataelement de inner join dataelementuseraccesses deua on de.dataelementid = deua.dataelementid" +
                " inner join useraccess ua on deua.useraccessid = ua.useraccessid" +
                " inner join users usr on usr.userid = ua.userid;";

            String userGroupQuery = "select ug.uuid as id, uga.access as access " +
                "from dataelement de inner join dataelementusergroupaccesses deuga on de.dataelementid = deuga.dataelementid" +
                "inner join usergroupaccess uga on uga.usergroupaccessid = deuga.usergroupaccessid" +
                "inner join usergroup ug on ug.usergroupid = uga.usergroupid;";

//            statement.execute( query );

        }
        catch ( SQLException e )
        {
            log.warn( e.getMessage() );
        }
    }

    @Override
    public boolean canExecuteInTransaction()
    {
        return false;
    }
}
