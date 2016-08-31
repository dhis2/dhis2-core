
function setAppConfig() {
	
	var config = {
		appFolderPath: $( "#appFolderPath" ).val(),
		appBaseUrl: $( "#appBaseUrl" ).val()
	};
	
	$.ajax( {
		url: "../api/apps/config",
		type: "post",
		contentType: "application/json",
		data: JSON.stringify( config )
	} ).done( function() {
		setHeaderDelayMessage( "Settings updated" );
	} ).fail( function( xhr, text, error ) {
		setHeaderDelayMessage( xhr.responseText );
	} );	
}

function resetAppConfig() {
	
	$.ajax( {
		url: "../api/apps/config",
		type: "delete"
	} ).done( function() {
		window.location.href = "appSettings.action";
	} );
}