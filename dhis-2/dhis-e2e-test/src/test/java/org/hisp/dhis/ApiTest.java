package org.hisp.dhis;



import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.helpers.TestCleanUp;
import org.hisp.dhis.helpers.extensions.ConfigurationExtension;
import org.hisp.dhis.helpers.extensions.MetadataSetupExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@ExtendWith( ConfigurationExtension.class )
@ExtendWith( MetadataSetupExtension.class )
public abstract class ApiTest
{
    @AfterAll
    public void afterAll()
    {
        new LoginActions().loginAsDefaultUser();
        new TestCleanUp().deleteCreatedEntities();
    }
}
