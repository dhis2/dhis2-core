package org.hisp.dhis.dxf2.metadata.sync.exception;

/**
 * Created by vanyas on 5/1/17.
 */
public class MetadataSyncImportException extends
    RuntimeException
{
    public MetadataSyncImportException( String message )
    {
        super( message );
    }

    public MetadataSyncImportException( Throwable cause )
    {
        super( cause );
    }

    public MetadataSyncImportException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
