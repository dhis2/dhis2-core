jQuery( document ).ready( function()
{
	var rules = getValidationRules( "profile" );

	validation2( 'updateUserProfileForm', updateUserProfile, {
		'rules' : rules
	} );

	datePickerValid( 'birthday', false );
	
	jQuery( '#email' ).focus();
} );

function updateUserProfile()
{
	jQuery.postUTF8( 'updateUserProfile.action',
	{
		id: getFieldValue( 'id' ),
		email: getFieldValue( 'email' ),
		phoneNumber: getFieldValue( 'phoneNumber' ),
		introduction: getFieldValue( 'introduction' ),
		jobTitle: getFieldValue( 'jobTitle' ),
		gender: getFieldValue( 'gender' ),
		birthday: getFieldValue( 'birthday' ),
		nationality: getFieldValue( 'nationality' ),
		employer: getFieldValue( 'employer' ),
		education: getFieldValue( 'education' ),
		interests: getFieldValue( 'interests' ),
		languages: getFieldValue( 'languages' )
	}
	, function( json ) 
	{
		setHeaderDelayMessage( json.message );
	} );
}
