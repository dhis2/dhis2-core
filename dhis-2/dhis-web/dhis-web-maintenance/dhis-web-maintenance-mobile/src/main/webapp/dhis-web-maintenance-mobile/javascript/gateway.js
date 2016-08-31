currentType = '';
function changeValueType( value )
{
	hideAll();
    if ( value == 'bulksms' ) {
    	showById( "bulksmsFields" );
    } else if ( value == 'clickatell' ) {
    	showById( "clickatellFields" );
    } else if ( value == "smpp_gw"){
    	showById( "smppFields" );
    } else {
	    showById( "genericHTTPFields" );
	}
	currentType = value;
}

function hideAll() 
{
	 hideById( "bulksmsFields" );
	 hideById( "smppFields" );
	 hideById( "clickatellFields" );
	 hideById( "genericHTTPFields" ); 
}

function getValidationRulesGateway()
{
	var rules = {};
	if ( currentType == 'bulksms' ) {
		rules = {
			'bulksmsFields input[id=name]' : { 'required' : true },
			'bulksmsFields input[id=username]' : { 'required' : true },
			'bulksmsFields input[id=urlTemplate]' : { 'required' : true },
			'bulksmsFields input[id=password]' : { 'required' : true }
		};
	} else if ( currentType == 'smpp_gw' ) {
		rules = {
			'smppFields input[id=name]' : { 'required' : true },
			'smppFields input[id=username]' : { 'required' : true },
			'smppFields input[id=password]' : { 'required' : true }
		};
	} else if ( currentType == 'clickatell' ) {
		rules = {
			'clickatellFields input[id=name]' : { 'required' : true },
			'clickatellFields input[id=username]' : { 'required' : true },
			'clickatellFields input[id=password]' : { 'required' : true },
			'clickatellFields input[id=urlTemplate]' : { 'required' : true },
			'clickatellFields input[id=authToken]' : { 'required' : true },
			'clickatellFields input[id=apiId]' : { 'required' : true }
		};
	} else {
		rules = {
			'genericHTTPFields input[id=name]' : { 'required' : true },
			'genericHTTPFields input[id=username]' : { 'required' : true },
			'genericHTTPFields input[id=password]' : { 'required' : true },
			'genericHTTPFields input[id=urlTemplate]' : { 'required' : true }
		};
	}

	return rules;
}

function getValidationRulesUpdateClient()
{
	var rules = { 
		"version" : { 
			"required" : true,
			"digits" : true
		}
	};
	return rules;
}

function saveGatewayConfig()
{
	
	if ( currentType == 'bulksms' )
	{
		var username = getFieldValue( 'bulksmsFields input[id=username]' );
		var password = getFieldValue( 'bulksmsFields input[id=password]' );
		var URL = getFieldValue( 'bulksmsFields input[id=urlTemplate]' );
		if ( username == "" || password == "" || URL == "")
		{	
			showErrorMessage( i18n_required_data_error );
		}
		else
		{
			lockScreen();
			jQuery.postJSON( "saveBulkSMSConfig.action", {
				gatewayType: getFieldValue( 'gatewayType' ),
				name: getFieldValue( 'bulksmsFields input[id=name]' ),
				username: getFieldValue( 'bulksmsFields input[id=username]' ),
				password: getFieldValue( 'bulksmsFields input[id=password]' ),
				urlTemplate: getFieldValue( 'bulksmsFields input[id=urlTemplate]' ),
				urlTemplateForBatchSms: getFieldValue( 'bulksmsFields input[id=urlTemplateForBatchSms]' ),
				region: getFieldValue( 'bulksmsFields select[id=region]' )
			}, function ( json ) {
				unLockScreen();
				showMessage( json );
			} );
		}
	}
	else if ( currentType == 'smpp_gw' )
	{
		var username = getFieldValue( 'smppFields input[id=username]' );
		var password = getFieldValue( 'smppFields input[id=password]' );
		if ( username == "" || password == "")
		{	
			showErrorMessage( i18n_required_data_error );
		}
		else
		{
			lockScreen();
			jQuery.postJSON( "updateSMPPConfig.action", {
				gatewayType: getFieldValue( 'gatewayType' ),
				name: getFieldValue( 'smppFields input[id=name]' ),
				username: getFieldValue( 'smppFields input[id=username]' ),
				password: getFieldValue( 'smppFields input[id=password]' ),
				port: getFieldValue( 'smppFields input[id=port]' ),
				address: getFieldValue( 'smppFields input[id=address]' ),
			}, function ( json ) {
				unLockScreen();
				showMessage( json );
			} );
		}
	}
	else if ( currentType == 'clickatell' )
	{
		var username = getFieldValue( 'clickatellFields input[id=username]' );
		var password = getFieldValue( 'clickatellFields input[id=password]' );
		var URL = getFieldValue( 'clickatellFields input[id=urlTemplate]' );
		if ( username == "" || password == "" || URL == "")
		{	
			showErrorMessage( i18n_required_data_error );
		}
		else
		{
			lockScreen();
			jQuery.postJSON( "saveClickatellConfig.action", {
				gatewayType: getFieldValue( 'gatewayType' ),
				name: getFieldValue( 'clickatellFields input[id=name]' ),
				username: getFieldValue( 'clickatellFields input[id=username]' ),
				password: getFieldValue( 'clickatellFields input[id=password]' ),
				apiId: getFieldValue( 'clickatellFields input[id=apiId]' ),
				urlTemplate: getFieldValue( 'clickatellFields input[id=urlTemplate]' ),
				authToken: getFieldValue( 'clickatellFields input[id=authToken]' )
			}, function ( json ) {
				unLockScreen();
				showMessage( json );
			} );
		}
	}
	else
	{
		var username = getFieldValue( 'genericHTTPFields input[id=username]' );
		var password = getFieldValue( 'genericHTTPFields input[id=password]' );
		var URL = getFieldValue( 'genericHTTPFields input[id=urlTemplate]' );
		if( username == "" || password == "" || URL == "" )
		{	
			showErrorMessage( i18n_required_data_error );
		}
		else
		{
			lockScreen();
			jQuery.postJSON( "saveHTTPConfig.action", {
				gatewayType: getFieldValue( 'gatewayType' ),
				name: getFieldValue( 'genericHTTPFields input[id=name]' ),
				username: getFieldValue( 'genericHTTPFields input[id=username]' ),
				password: getFieldValue( 'genericHTTPFields input[id=password]' ),
				urlTemplate: getFieldValue( 'genericHTTPFields input[id=urlTemplate]' )
			}, function ( json ) {
				unLockScreen();
				showMessage( json );
			} );
		}
	}
}

function showMessage( json )
{
	if ( json.response == "success" ) {
		showSuccessMessage( i18n_add_update_success );
	} else {
		showErrorMessage( json.message, 7000 );
	}
}

function removeItem( itemId, itemName, confirmation, action, success )
{                
    var result = window.confirm( confirmation + "\n\n" + itemName );
    
    if ( result )
    {
		lockScreen();
    	$.postJSON(
    	    action,
    	    {
    	        "id": itemId
    	    },
    	    function( json )
    	    { 
    	    	if ( json.response == "success" )
    	    	{
					jQuery( "tr#tr" + itemId ).remove();
	                
					jQuery( "table.listTable tbody tr" ).removeClass( "listRow listAlternateRow" );
	                jQuery( "table.listTable tbody tr:odd" ).addClass( "listAlternateRow" );
	                jQuery( "table.listTable tbody tr:even" ).addClass( "listRow" );
					jQuery( "table.listTable tbody" ).trigger("update");
  
					if ( success && typeof( success) == "function" )
					{
						success.call();
					}
					unLockScreen();
					showSuccessMessage( i18n_delete_success );
					refreshIndex( itemId );
    	    	}
    	    	else if ( json.response == "error" )
    	    	{ 
					unLockScreen();
					showWarningMessage( json.message );
    	    	}
    	    }
    	);
    }
}