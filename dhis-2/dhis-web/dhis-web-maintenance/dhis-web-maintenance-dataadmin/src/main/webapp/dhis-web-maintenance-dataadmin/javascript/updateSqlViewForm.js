jQuery(document).ready(function() {
	validation2('updateSqlViewForm', function() {
		validateAddUpdateSqlView('update');
	}, {
		'rules' : getValidationRules("sqlView")
	});
});
