package org.hisp.dhis.scheduling;

import org.hisp.dhis.feedback.ErrorReport;

import java.io.Serializable;

/**
 * Interface for job specific parameters. Serializable so that we can store the object in the database.
 *
 * @author Henning Håkonsen
 */
public interface JobParameters
    extends Serializable
{
    ErrorReport validate();
}
