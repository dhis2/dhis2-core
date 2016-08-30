
// -----------------------------------------------------------------------------
// Import
// -----------------------------------------------------------------------------

function getImportStatus()
{
    var request = new Request();
    request.setResponseTypeXML( "response" );
    request.setCallbackSuccess( importStatusReceived );    
    request.send( "getImportStatus.action" );
}

function importStatusReceived( messageElement )
{
	var actionType = getElementValue( messageElement, "actionType" );
	
	if ( actionType == "info" )
	{
		var statusMessage = getElementValue( messageElement, "statusMessage" );
		var fileMessage = getElementValue( messageElement, "fileMessage" );
		var running = getElementValue( messageElement, "running" );
		
		setInfo( i18n_current_import_file + ": " + fileMessage );
		
		if ( running == "true" )
        {
            setWaitMessage( statusMessage );
		    waitAndGetImportStatus( 2000 );
        }
        else
        {        	
            setMessage( statusMessage );
        }
	}
	else if ( actionType == "preview" )
	{
		window.location.href = "displayPreviewForm.action";
	}
	else if ( actionType == "analysis" )
	{
	    window.location.href = "getImportAnalysis.action";
	}
}

function waitAndGetImportStatus( millis )
{
    setTimeout( "getImportStatus();", millis );
}
