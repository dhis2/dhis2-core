package org.hisp.dhis.webapi.controller.exception;

import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;

/**
 * Created by vanyas on 4/22/17.
 * This exception can be used for handling Metadata Import related conflict exceptions
 * to return 409.
 */
public class MetadataImportConflictException
    extends Exception
{
    private MetadataSyncSummary metadataSyncSummary = null;

    public MetadataImportConflictException( MetadataSyncSummary metadataSyncSummary )
    {
        this.metadataSyncSummary = metadataSyncSummary;
    }

    public MetadataImportConflictException( String message )
    {
        super( message );
    }

    public MetadataImportConflictException( Throwable cause )
    {
        super( cause );
    }

    public MetadataImportConflictException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public MetadataSyncSummary getMetadataSyncSummary()
    {
        return metadataSyncSummary;
    }
}
