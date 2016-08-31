
$( document ).ready( function() {
	pingNotificationsTimeout();
} );

var pingTimeout = null;

function importEventData()
{
	if ( !$( "#upload" ).val() )
	{
		setHeaderDelayMessage( "Please select a file to upload" );
		return false;
	}

	$( "#notificationTable" ).empty();
	$( "#importForm" ).submit();
}

function pingNotificationsTimeout()
{
	pingNotifications( 'EVENT_IMPORT', 'notificationTable', displaySummaryLink );
	pingTimeout = setTimeout( "pingNotificationsTimeout()", 2500 );
}

function displaySummaryLink()
{
	var html = '<tr><td></td><td><a href="javascript:displaySummary()">Display import summary</a></td></tr>';
	$( '#notificationTable' ).prepend( html );
}

function displaySummary()
{
	$( '#notificationDiv' ).hide();
	$( '#importSummaryDiv' ).show( 'fast' ).load( 'getImportEventSummaries.action' );
}
