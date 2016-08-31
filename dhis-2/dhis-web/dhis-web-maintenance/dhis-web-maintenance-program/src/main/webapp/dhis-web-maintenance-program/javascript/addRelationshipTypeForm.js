jQuery(document).ready(	function(){

	jQuery('aIsToB').focus();
	
	validation2( 'addRelationshipTypeForm', function( form )
	{
		validateAddRelationshipType();
	},{
		'rules' : getValidationRules( "relationshipType" )
	});
	
});	