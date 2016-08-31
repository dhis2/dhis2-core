
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

function getReportParams( id )
{
	window.location.href = "getReportParams.action?id=" + id;
}

function validationError()
{
	if ( $( "#selectionTree" ).length && selectionTreeSelection.getSelected().length == 0 )
	{
		setHeaderDelayMessage( i18n_please_select_unit );
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
	
	var url = "id=" + $( "#id" ).val() + "&mode=" + $( "#mode" ).val();
	    
    if ( $( "#reportingPeriod" ).length )
    {
        url += "&reportingPeriod=" + $( "#reportingPeriod" ).val();
    }
        
    if ( $( "#selectionTree" ).length )
    {
        url += "&organisationUnitId=" + selectionTreeSelection.getSelected()[0];
    }
    
	window.location.href = "getReport.action?" + url;
}
