jQuery(document).ready(	function(){
		
	jQuery('aIsToB').focus();
		
	validation2( 'updateRelationshipTypeForm', function( form )
	{
		validateUpdateRelationshipType();
	},{
		'rules' : getValidationRules( "relationshipType" )
	});		
		
});	