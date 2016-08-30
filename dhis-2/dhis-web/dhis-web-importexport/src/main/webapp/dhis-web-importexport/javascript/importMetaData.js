
$( document ).ready( function() {
	pingNotificationsTimeout();
} );

var pingTimeout = null;

function importMetaData()
{
	if ( !$( "#upload" ).val() )
	{
		setHeaderDelayMessage( "Please select a file to upload" );
		return false;
	}
	
	$( "#notificationTable" ).empty();
	$( "#importForm" ).submit();
}

function metaDataImportFormatChanged()
{
	if ( $( "#importFormat" ).val() == "csv" )
	{
		$( "#csvImportTr" ).show();
	}
	else
	{
		$( "#csvImportTr" ).hide();
	}
}

function pingNotificationsTimeout()
{
	pingNotifications( 'METADATA_IMPORT', 'notificationTable', displaySummaryLink );
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
	$( '#importSummaryDiv' ).show( 'fast' ).load( 'getMetadataImportReport.action' );
}

function toggleOptions()
{
	$( ".moreOptions" ).toggle();
}
