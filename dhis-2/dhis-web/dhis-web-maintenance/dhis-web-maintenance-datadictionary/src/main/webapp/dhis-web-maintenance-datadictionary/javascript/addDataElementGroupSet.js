jQuery(document).ready(function() {
    validation2('addDataElementGroupSet', function( form ) {
        form.submit();
    }, {
        'beforeValidateHandler': function() {
            beforeSubmit();
            $("#degSelected").find("option").attr("selected", "selected");
        },
        'rules': getValidationRules("dataElementGroupSet")
    });

    checkValueIsExist("name", "validateDataElementGroupSet.action");
});
