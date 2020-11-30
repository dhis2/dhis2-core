package org.hisp.dhis.db.migration.v35;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class V2_35_22__add_sms_authority_to_userauthoritygroups
    extends BaseJavaMigration
{
    @Override
    public void migrate( Context context ) throws Exception
    {
        final String sql = "SELECT DISTINCT(userroleid), authority FROM userroleauthorities ura WHERE authority='M_dhis-web-maintenance-mobile' AND " +
            "NOT EXISTS (SELECT * FROM userroleauthorities WHERE userroleid=ura.userroleid AND authority='M_dhis-web-sms-configuration')";

        try ( final Statement stmt = context.getConnection().createStatement();
              final ResultSet rs = stmt.executeQuery( sql ) )
        {
            while ( rs.next() )
            {
                long id = rs.getLong( "userroleid" );

                final String insertSql = "INSERT INTO userroleauthorities (userroleid, authority) VALUES (?, ?)";

                try ( final PreparedStatement ps = context.getConnection().prepareStatement( insertSql ) )
                {
                    ps.setLong( 1, id );
                    ps.setString( 2, "M_dhis-web-sms-configuration" );

                    ps.execute();
                }
            }
        }
    }
}
