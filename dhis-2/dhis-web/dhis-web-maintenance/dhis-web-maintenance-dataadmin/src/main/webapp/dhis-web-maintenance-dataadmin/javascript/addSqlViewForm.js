jQuery(document).ready(function() {
	validation2('addSqlViewForm', function() {
		validateAddUpdateSqlView('add');
	}, {
		'rules' : getValidationRules("sqlView")
	});
});
