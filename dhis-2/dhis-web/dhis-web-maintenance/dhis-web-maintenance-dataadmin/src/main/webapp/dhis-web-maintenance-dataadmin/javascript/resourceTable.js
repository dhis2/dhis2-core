
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
	pingNotifications( "RESOURCETABLE_UPDATE", "notificationTable" );
	setTimeout( "pingNotificationsTimeout()", 2500 );
}
