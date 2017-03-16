
$( document ).ready( function() {
	pingNotificationsTimeout();
} );

function startExport()
{
	$( '#notificationTable' ).show().prepend( '<tr><td>' + _loading_bar_html + '</td></tr>' );
	
	var url = '../api/resourceTables/analytics';
			
	$.ajax({ 
		url: url, 
		type: 'post',
		success: pingNotificationsTimeout 
	});
}

function pingNotificationsTimeout()
{
	pingNotifications( 'ANALYTICSTABLE_UPDATE', 'notificationTable' );
	setTimeout( "pingNotificationsTimeout()", 2500 );
}
