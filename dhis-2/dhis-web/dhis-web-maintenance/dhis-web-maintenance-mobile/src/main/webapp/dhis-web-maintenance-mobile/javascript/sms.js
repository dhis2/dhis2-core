// -----------------------------------------------------------------------------
// Change what type of sms to show
// -----------------------------------------------------------------------------

function criteriaChanged()
{
    var status = getListValue( "smsStatus" );

    var url = "showReceivingPage.action?smsStatus=" + status;

    window.location.href = url;
}

function reimport(confirmation, itemId, success )
{                
    var result = window.confirm( confirmation );
    var action = "reimport.action";
    if ( result )
    {
    	$.postJSON(
    	    action,
    	    {
    	        "incomingSMSId": itemId   
    	    },
    	    function( json )
    	    { 
    	    	if ( json.response == "success" )
    	    	{  
					if ( success && typeof( success) == "function" )
					{
						success.call();
					}
  
					showSuccessMessage( json.message );
					setTimeout("window.location.reload();", 3000);
    	    	}
    	    	else if ( json.response == "error" )
    	    	{ 
					showWarningMessage( json.message );
					setTimeout("window.location.reload();", 3000);
    	    	}
    	    }
    	);
    }
}