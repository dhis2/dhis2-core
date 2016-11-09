var _target = "";
var isOrgunitSelected = false;

function selectedOrganisationUnitSendSMS( unitIds )
{
	isOrgunitSelected = (unitIds && unitIds.length > 0);
}

function toggleSMSGUI( _value )
{
	if ( _value == "phone" ) 
	{
		showById( 'phoneType' );
		showById( 'trRawPhone' );
		hideById( 'orgunitType' );
		hideById( 'trUserGroup' );
	}
	else if ( _value == "userGroup" )
	{
		showById( 'trUserGroup' );
		hideById( 'trRawPhone' );
		hideById( 'orgunitType' );
	}
	else if ( _value == "user" || _value == "unit" )
	{
		showById( 'orgunitType' );
		hideById( 'phoneType' );
		hideById( 'trRawPhone' );
		hideById( 'trUserGroup' );
	}
	else {
		window.location.href = "showBeneficiarySMSForm.action";
	}
	
	_target = _value;
}

function toggleAll( checked )
{
	var list = jQuery( "input[type=checkbox][name=patientSMSCheckBox]" );
	
	for ( var i in list )
	{
		list[i].checked = checked;
	}
}

function sendSMSMessage( _form )
{
	var p = {};
	p.recipients = [];

	if ( _target == "phone" )
	{
		var list = getFieldValue( "recipient" );

		if ( list == '' )
		{
			setHeaderDelayMessage( i18n_no_recipient );
			return;
		}
		
		list = list.split( ";" );

		for ( var i in list )
		{
			if ( list[i] && list[i] != '' )
			{
				p.recipients.push( list[i] );
			}
		}
	}
	else if ( _target == "userGroup" )
	{
		var userGroup = getFieldValue( _target );

		if ( userGroup == null )
		{
			setHeaderDelayMessage( i18n_please_select_user_group );
			return;
		}
	}
	
	else
	{
		if ( hasElements( 'recipients' ) )
		{
			var list = jQuery( '#recipients' ).children();
	
			list.each( function( i, item ){
				p.recipients.push( item.value );
			});
		}
		else { markInvalid( "recipients", i18n_person_list_empty ); }
	}

	jQuery.postUTF8( _form.action,
	{
		recipients: JSON.stringify( p.recipients ),
		gatewayId: getFieldValue( 'gatewayId' ),
		text: getFieldValue( 'text' ),
		sendTarget: getFieldValue( 'sendTarget' ),
		userGroup: getFieldValue( 'userGroup' )
	}, function ( json )
	{
		if ( json.response == "success" ) {
			setHeaderDelayMessage( json.message );
		}
		else {
			setHeaderDelayMessage( json.message, 7000 );
		}
	} );
}
