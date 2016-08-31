currentType = '';
function changeValueType( value )
{
	var saveSettingsInput = jQuery("#savesettings");
	var addMoreButton = jQuery("#addmore");
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

	if (value === "http") {
		saveSettingsInput.prop("type","button");
		saveSettingsInput.attr("onclick","saveSettings()");
		addMoreButton.show();
	} else {
		saveSettingsInput.prop("type","submit");
		saveSettingsInput.removeAttr("onclick");
		addMoreButton.hide();
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
		var usernameParameter = getFieldValue( 'genericHTTPFields input[id=usernameParameter]' );
		var passwordParameter = getFieldValue( 'genericHTTPFields input[id=passwordParameter]' );
		var username = getFieldValue( 'genericHTTPFields input[id=username]' );
		var password = getFieldValue( 'genericHTTPFields input[id=password]' );

		var messageParameter = getFieldValue( 'genericHTTPFields input[id=messageParameter]' );
		var recipientParameter = getFieldValue( 'genericHTTPFields input[id=recipientParameter]' );
		
		
		var URL = getFieldValue( 'genericHTTPFields input[id=urlTemplate]' );

		if( usernameParameter == "" || passwordParameter == "" || URL == "" ||  username == "" || password == "" || messageParameter == "" || recipientParameter == "")
		{	
			showErrorMessage( i18n_required_data_error );
		}
		else
		{
			lockScreen();
			jQuery.postJSON( "saveHTTPConfig.action", {
				gatewayType: getFieldValue( 'gatewayType' ),
				name: getFieldValue( 'genericHTTPFields input[id=name]' ),
				
				usernameParameter: getFieldValue( 'genericHTTPFields input[id=usernameParameter]' ),
				username: getFieldValue( 'genericHTTPFields input[id=username]' ),
				
				passwordParameter: getFieldValue( 'genericHTTPFields input[id=passwordParameter]' ),
				password: getFieldValue( 'genericHTTPFields input[id=password]' ),
				
				messageParameter: getFieldValue( 'genericHTTPFields input[id=messageParameter]' ),
				recipientParameter: getFieldValue( 'genericHTTPFields input[id=recipientParameter]' ),
				
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

function generateaddKeyValueParamForm()
{
	var rowId = jQuery('.trNewParam').length + 1;
	var rowName = '\'#trNewParam'+rowId;
	var contend = '<tr id="trNewParam' + rowId + '" name="trNewParam' + rowId + '" class="trNewParam">'
		+	'<td><input id="name' + rowId + '" name="name' + rowId + '" style="width:9em" type="text" onblur="checkDuplicatedKeyName(this.value,' + rowId + ') " placeholder="' + i18_new_key + '" />:</td>'
		+	'<td><input id="value' + rowId + '" name="value' + rowId + '" type="text" style="width: 10em" placeholder="' + i18_value + '"/>'
		+   	'<input type="radio" name="inputType'+rowId+'" value="text" onclick="updateInputType(' + rowId + ',\'text\')" checked>'+i18_text+'</input>'
		+   	'<input type="radio" name="inputType'+rowId+'" value="classified" onclick="updateInputType(' + rowId + ',\'classified\')" style="margin-left: 1em" >'+i18_password+'</input>'
		+   	'<input style="margin-left: 3em" type="button" value="remove" onclick="removeNewParamForm(' + rowId + ')"/></td>'
		+ '</tr>';
	jQuery('#genericHTTPFields').append(contend);
}

function removeNewParamForm( rowId )
{
	jQuery('#trNewParam'+rowId).remove();
}
function removeOldParamForm( rowId )
{
	jQuery('#trOldParam'+rowId).remove();
}

function updateInputType( rowId, type )
{
	jQuery('#value'+rowId).prop('type', type);
}

function saveSettings ()
{
	var url = "../api/gateways/generichttp";
	var httpFields = jQuery('#genericHTTPFields');
	var data = {
		name: httpFields.find("#name")[0].value,
		messageParameter: httpFields.find("#messageParameter")[0].value,
		recipientParameter: httpFields.find("#recipientParameter")[0].value,
		urltemplate: httpFields.find("#urlTemplate")[0].value
	};

	var newParams = getHttpKeyValueParamsAddedByTheUser(httpFields);

	if (data.name == "" || data.messageParameter == "" || data.recipientParameter == "" || data.urltemplate == "") {
		showErrorMessage(i18n_required_data_error);
	} else {

		if (newParams) {
			data['parameters'] = newParams;
		}

		jQuery.ajax({
			url: url,
			type: 'POST',
			dataType: 'json',
			data: JSON.stringify(data),
			contentType: "application/json"

		}).done(function (response) {
			console.log(response);
			showSuccessMessage(i18n_add_update_success);
		}).fail(function (error) {
			console.log(error);
			showErrorMessage("Error - status:"+error.status+" message:"+error.statusText);
		});
	}
}

function validateParameters(data)
{
	if (data.key =="" || data.value =="")
	{
		return false;
	}
	else
	{
		return true;
	}
}

function getHttpKeyValueParamsAddedByTheUser(allFields)
{
	var newParamName, newParamValue;
	var newParamsCount = jQuery('.trNewParam').length;
	var oldParamsCount = jQuery('.trOldParam').length;
	
	var parameters = [];
	
	for (var rowIndex = 1; rowIndex <= newParamsCount; rowIndex++) {
		var object = {};
		newParamName = allFields.find('#trNewParam'+rowIndex).find('#name'+rowIndex)[0].value;
		newParamValue = allFields.find('#trNewParam'+rowIndex).find('#value'+rowIndex)[0].value;	
		newParamType = $('input[name=inputType'+rowIndex+']:checked').val();
		
		object.key=newParamName;
		object.value=newParamValue;
		
		if(newParamType == 'classified')
		{
			object.classified=true;
		}
		else
		{
			object.classified=false;
		}
		
		if(validateParameters(object))
		{
			parameters.push(object);
		}	
	}
	
	for (var rowIndex = 1; rowIndex <= oldParamsCount; rowIndex++) {
		var object = {};
		oldParamName = allFields.find('#trOldParam'+rowIndex).find('#oldVName'+rowIndex)[0].value;
		oldParamValue = allFields.find('#trOldParam'+rowIndex).find('#oldValue'+rowIndex)[0].value;
		oldParamType = allFields.find('#trOldParam'+rowIndex).find('#oldHidden'+rowIndex)[0].value;

		object.key=oldParamName;
		object.value=oldParamValue;
		object.classified=oldParamType;
		parameters.push(object);
	}
	if (Object.keys(parameters).length === 0) {
		return null;
	}
	return parameters;
}
