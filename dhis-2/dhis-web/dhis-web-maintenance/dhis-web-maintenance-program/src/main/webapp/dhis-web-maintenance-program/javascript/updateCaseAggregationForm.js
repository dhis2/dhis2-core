jQuery(document).ready(	function(){
	
	validation2( 'updateCaseAggregationForm', function( form )
	{
		form.submit();
	},{
		'rules' : getValidationRules( "caseAggregation" )
	});
	
	checkValueIsExist( "aggregationDataElementId", "validateCaseAggregation.action", {id:getFieldValue('id')});
	
	byId('name').focus();
	jQuery("#tabs").tabs();
		
});	