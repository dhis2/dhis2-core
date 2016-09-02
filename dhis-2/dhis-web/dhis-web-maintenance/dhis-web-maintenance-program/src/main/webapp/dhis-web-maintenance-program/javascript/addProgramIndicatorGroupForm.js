jQuery(document).ready(function() {

    jQuery('name').focus();

    validation2('addProgramIndicatorGroupForm', function( form ) {
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

    $('#piAvailable').selected({
        url: '../api/programIndicators.json',
        target: $('#piSelected'),
        search: $('#piAvailableSearch'),
        iterator: 'programIndicators'
    });

    checkValueIsExist("name", "validateProgramIndicatorGroup.action");
});