jQuery(document).ready(function() {
	validation2('editDataSetForm', function(form) {
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
		},
		sectionId : function() {
			return jQuery("#sectionId").val();
		}
	});
});
