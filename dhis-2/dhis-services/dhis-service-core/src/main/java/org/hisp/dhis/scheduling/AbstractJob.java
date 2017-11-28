package org.hisp.dhis.scheduling;

import org.hisp.dhis.feedback.ErrorReport;

/**
 * All jobs related to the system extends AbstractJob and can override the validate method.
 *
 * @author Henning Håkonsen
 */
public abstract class AbstractJob
    implements Job
{
    @Override
    public ErrorReport validate()
    {
        return null;
    }
}
