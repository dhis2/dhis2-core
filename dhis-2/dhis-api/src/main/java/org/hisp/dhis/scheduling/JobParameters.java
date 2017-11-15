package org.hisp.dhis.scheduling;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.Serializable;

/**
 * Interface for job specific parameters. Serializable so that we can store the object in the database.
 *
 * @author Henning Håkonsen
 */
public interface JobParameters
    extends Serializable
{
    JobParameters mapParameters( JsonNode parameters )
        throws IOException;
}
