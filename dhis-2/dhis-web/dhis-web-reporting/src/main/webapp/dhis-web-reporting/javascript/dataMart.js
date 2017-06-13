
$( document ).ready( function() {
	pingNotificationsTimeout();
} );

function startExport()
{
  var lastYears = jQuery("#lastYears").val();
  if ( lastYears && !dhis2.validation.isPositiveNumber( lastYears ) ) {
    alert(jQuery("#invalidLastYears").val());
    return;
  }

	$( '#notificationTable' ).show().prepend( '<tr><td>' + _loading_bar_html + '</td></tr>' );

	var postData = {};
	postData.skipResourceTables = jQuery("#cbResource").is(':checked') ? "true" : "false";
	postData.skipAggregate = jQuery("#cbAggregate").is(':checked') ? "true" : "false";
	postData.skipEvents = jQuery("#cbEvent").is(':checked') ? "true" : "false";
	postData.skipEnrollment = jQuery("#cbEnrollment").is(':checked') ? "true" : "false";
  if ( lastYears ) {
    postData.lastYears = lastYears;
  }

var url = '../api/resourceTables/analytics';
			
	$.ajax({ 
		url: url,
		data: postData,
		type: 'post',
		success: pingNotificationsTimeout 
	});
}

function pingNotificationsTimeout()
{
	pingNotifications( 'ANALYTICSTABLE_UPDATE', 'notificationTable' );
	setTimeout( "pingNotificationsTimeout()", 2500 );
}
