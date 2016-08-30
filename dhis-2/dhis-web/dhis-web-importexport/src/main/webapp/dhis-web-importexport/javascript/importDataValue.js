
$( document ).ready( function() {
	pingNotificationsTimeout();
	displayIdentifierSchemes();
} );

var pingTimeout = null;

function importDataValue()
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
	pingNotifications( 'DATAVALUE_IMPORT', 'notificationTable', displaySummaryLink );	
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
	$( '#importSummaryDiv' ).show( 'fast' ).load( 'getDataValueImportSummary.action' );
}

function toggleOptions()
{
	$( ".moreOptions" ).toggle();
}

function displayIdentifierSchemes() {
	displayIdentifierScheme( "dataElementIdScheme", "filter=dataElementAttribute:eq:true" );
	displayIdentifierScheme( "orgUnitIdScheme", "filter=organisationUnitAttribute:eq:true" );
	displayIdentifierScheme( "idScheme", "filter=dataElementAttribute:eq:true&filter=organisationUnitAttribute:eq:true" );
}

function displayIdentifierScheme( id, filter ) {
	
	var url = "../api/attributes?fields=id,displayName&filter=unique:eq:true&" + filter;
	
	$.getJSON( url, function( json ) {
		var html = "";
		$.each( json.attributes, function( inx, attr ) {
			html += "<option value=\"ATTRIBUTE:" + attr.id + "\">" + attr.displayName + "</option>";
		} );
		
		$( "#" + id ).append( html );
	} );
}
