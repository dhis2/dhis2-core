jQuery( document ).ready( function()
{
	jQuery( "#name" ).focus();

	validation2( 'updateRoleForm', function( form )
	{
		selectAllById( 'selectedList' );
		selectAllById( 'selectedProgramList' );
		selectAllById( 'selectedListAuthority' );
		form.submit();
	}, {
		'rules' : getValidationRules("role")
	} );

	/* remote validation */
	checkValueIsExist( "name", "validateRole.action", {
		id : getFieldValue( 'id' )
	} );

	sortList( 'availableListAuthority', 'ASC' );
	sortList( 'selectedListAuthority', 'ASC' );
} );
