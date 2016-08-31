
$( document ).ready( function() { pageInit(); } );

function pageInit()
{
	// Zebra stripes in lists
	
	$( "table.listTable tbody tr:odd" ).addClass( "listAlternateRow" );
    $( "table.listTable tbody tr:even" ).addClass( "listRow" );
}

// -----------------------------------------------------------------------------
// Report table
// -----------------------------------------------------------------------------

function getReportParams( uid )
{
	window.location.href = "getReportParams.action?uid=" + uid;
}

function validationError()
{
	if ( $( "#selectionTree" ).length && !selectionTreeSelection.isSelected() )
	{
		setMessage( i18n_please_select_unit );
		return true;
	}
	
	return false;
}	

function generateReport()
{
	if ( validationError() )
	{
		return false;
	}
	
	var url = "uid=" + $( "#uid" ).val();
	    
    if ( $( "#reportingPeriod" ).length )
    {
        url += "&pe=" + $( "#reportingPeriod" ).val();
    }
        
    if ( $( "#selectionTree" ).length )
    {
        url += "&ou=" + selectionTreeSelection.getSelectedUid();
    }
    
	window.location.href = "getReport.action?" + url;
}
