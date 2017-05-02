package org.hisp.dhis.webapi.controller.exception;

import org.hisp.dhis.webapi.view.ExcelGridView;

/**
 * Created by vanyas on 4/22/17.
 */
public class MetadataImportConflictException
    extends Exception
{
    public MetadataImportConflictException( String message)
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
}
