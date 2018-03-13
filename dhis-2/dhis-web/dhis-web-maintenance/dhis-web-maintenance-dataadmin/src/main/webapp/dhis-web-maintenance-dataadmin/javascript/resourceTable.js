
function generateResourceTable()
{
	$( "#notificationTable" ).show().prepend( "<tr><td>" + _loading_bar_html + "</td></tr>" );
	$.ajax( {
		url: "../api/resourceTables",
		type: "put",
		success: pingNotificationsTimeout 
	} );
}

function pingNotificationsTimeout()
{
	pingNotifications( "RESOURCE_TABLE", "notificationTable" );
	setTimeout( "pingNotificationsTimeout()", 2500 );
}
