jQuery(document).ready(function(){
	validation2( 'validationCriteriaForm', function( form )
	{
		form.submit();
	},{
		'rules' : getValidationRules( "validationCriteria" )
	});	

	checkValueIsExist( "name", "validateValidationCriteria.action", {id:getFieldValue('id')});
});