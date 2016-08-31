jQuery(document).ready(function() {
	validation2('addSectionForm', function(form) {
		form.submit();
	}, {
		'beforeValidateHandler' : function() {
			selectAllById('selectedDataElementList');
			selectAllById('selectedIndicatorList');
		},
		'rules' : getValidationRules("section")
	});

	checkValueIsExist("sectionName", "validateSection.action", {
		dataSetId : function() {
			return jQuery("#dataSetId").val();
		},
		name : function() {
			return jQuery("#sectionName").val();
		}
	});
});
