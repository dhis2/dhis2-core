
$( document ).ready( function() {
	pingNotificationsTimeout();
} );

function startExport()
{
	$( '#notificationTable' ).show().prepend( '<tr><td>' + _loading_bar_html + '</td></tr>' );
	
	var url = 'startExport.action';
	
	$( 'input[name="periodTypes"]').each( function() 
	{
		if ( $( this ).is( ':checked' ) )
		{
			url += "&periodTypes=" + $( this ).val();
		}
	} );
		
	$.get( url, pingNotificationsTimeout );
}

function pingNotificationsTimeout()
{
	pingNotifications( 'ANALYTICSTABLE_UPDATE', 'notificationTable' );
	setTimeout( "pingNotificationsTimeout()", 2500 );
}
