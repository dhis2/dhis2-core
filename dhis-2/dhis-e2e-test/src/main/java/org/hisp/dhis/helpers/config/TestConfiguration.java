package org.hisp.dhis.helpers.config;



import org.aeonbits.owner.ConfigFactory;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TestConfiguration
{
    private static Config config;

    public static Config get()
    {
        if ( config == null )
        {
            config = ConfigFactory.create( Config.class );
        }

        return config;
    }
}
