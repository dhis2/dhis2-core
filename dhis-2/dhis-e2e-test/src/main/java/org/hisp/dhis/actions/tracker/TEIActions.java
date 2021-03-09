package org.hisp.dhis.actions.tracker;



import org.hisp.dhis.actions.RestApiActions;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TEIActions
    extends RestApiActions
{
    public TEIActions()
    {
        super( "/trackedEntityInstances" );
    }
}
