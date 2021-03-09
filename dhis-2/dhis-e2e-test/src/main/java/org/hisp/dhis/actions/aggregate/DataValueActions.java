package org.hisp.dhis.actions.aggregate;



import org.hisp.dhis.actions.RestApiActions;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class DataValueActions
    extends RestApiActions
{
    public DataValueActions()
    {
        super( "/dataValues" );
    }
}
