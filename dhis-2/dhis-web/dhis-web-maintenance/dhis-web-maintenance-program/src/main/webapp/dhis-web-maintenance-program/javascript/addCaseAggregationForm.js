jQuery(document).ready(	function(){
	
	validation2( 'addCaseAggregationForm', function( form )
	{
		form.submit();
	},{
		'rules' : getValidationRules( "caseAggregation" )
	});
	
	jQuery("#tabs").tabs();
	checkValueIsExist( "aggregationDataElementId", "validateCaseAggregation.action");
	byId('name').focus();
});	