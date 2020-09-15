package org.hisp.dhis.dxf2.events.importer.context;

/**
 * Signal an irrecoverable exception during the WorkContext creation
 *
 * @author Luciano Fiandesio
 */
public class WorkContextLoaderException extends RuntimeException
{
    public WorkContextLoaderException() {
        super();
    }

    public WorkContextLoaderException(String message) {
        super( message );
    }

    public WorkContextLoaderException(String message, Throwable cause) {
        super( message, cause );
    }

    public WorkContextLoaderException(Throwable cause) {
        super( cause );
    }
}
