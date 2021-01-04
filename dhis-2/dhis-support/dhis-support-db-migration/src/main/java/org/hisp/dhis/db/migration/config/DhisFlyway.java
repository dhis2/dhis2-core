package org.hisp.dhis.db.migration.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.output.MigrateResult;

public class DhisFlyway
    extends
    Flyway
{
    private boolean repairBeforeMigrate = false;

    public DhisFlyway( Configuration configuration, boolean repairBeforeMigrate )
    {
        super( configuration );
        this.repairBeforeMigrate = repairBeforeMigrate;
    }

    @Override
    public MigrateResult migrate()
        throws FlywayException
    {
        if ( repairBeforeMigrate )
        {
            super.repair();
        }
        return super.migrate();
    }
}
