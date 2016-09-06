jQuery(document).ready(function() {
    jQuery('name').focus();

    validation2('updateProgramIndicatorGroupForm', function( form ) {
        form.submit();
    }, {
        'beforeValidateHandler': function() {
            $("#piSelected").find("option").attr("selected", "selected");
            if( jQuery("#piSelected option").length > 0 ) {
                setFieldValue('programIndicatorList', 'true');
            }
        },
        'rules': getValidationRules("programIndicatorGroup")
    });

    checkValueIsExist("name", "validateProgramIndicatorGroup.action", {id: getFieldValue('id')});

    $('#piAvailable').selected({
        url: '../api/programIndicators.json',
        target: $('#piSelected'),
        search: $('#piAvailableSearch'),
        iterator: 'programIndicators'
    });
});
