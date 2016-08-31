jQuery(document).ready(function() {
    validation2('addIndicatorGroupSet', function( form ) {
        form.submit();
    }, {
        'beforeValidateHandler': function() {
            listValidator('ingValidator', 'ingSelected');
            $("#ingSelected").find("option").attr("selected", "selected");
        },
        'rules': getValidationRules("indicatorGroupSet")
    });
});
