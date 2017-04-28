package org.hisp.dhis.webapi.controller.exception;

import org.hisp.dhis.webapi.view.ExcelGridView;

/**
 * Created by vanyas on 4/22/17.
 */
public class MetadataImportValidationException extends Exception
{
    public MetadataImportValidationException( String message)
    {
        super( message );
    }

    public MetadataImportValidationException( Throwable cause )
    {
        super( cause );
    }

    public MetadataImportValidationException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
