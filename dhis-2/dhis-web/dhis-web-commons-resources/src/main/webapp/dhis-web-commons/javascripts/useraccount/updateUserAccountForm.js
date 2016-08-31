jQuery( document ).ready( function()
{
	var rules = getValidationRules("user");

	/* some customization is needed for the updateUserAccount validation rules */
    rules["rawPassword"].required = false;
    rules["retypePassword"].required = false;
    rules["inviteEmail"].required = false;

    rules["oldPassword"] = {
			required: true
	};

	validation2( 'updateUserinforForm', updateUser, {
		'rules' : rules
	} );

	var oldPassword = byId( 'oldPassword' );
	oldPassword.select();
	oldPassword.focus();
} );

function updateUser()
{
	$.postUTF8( 'updateUserAccount.action',
	{
		id: getFieldValue( 'id' ),
		oldPassword: getFieldValue( 'oldPassword' ),
		rawPassword: getFieldValue( 'rawPassword' ),
		retypePassword: getFieldValue( 'retypePassword' ),
		surname: getFieldValue( 'surname' ),
		firstName: getFieldValue( 'firstName' ),
		email: getFieldValue( 'email' ),
		phoneNumber: getFieldValue( 'phoneNumber' )
	},
	function( json ) 
	{
		setHeaderDelayMessage( json.message );
	} );
}
