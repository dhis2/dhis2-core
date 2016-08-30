jQuery(document).ready(function() {
    jQuery('name').focus();

    validation2('updateAttributeGroupForm', function( form ) {
        form.submit();
    }, {
        'beforeValidateHandler': function() {
            $("#teaSelected").find("option").attr("selected", "selected");
            if( jQuery("#teaSelected option").length > 0 ) {
                setFieldValue('attributeList', 'true');
            }
        },
        'rules': getValidationRules("trackedEntityAttributeGroup")
    });

    checkValueIsExist("name", "validateAttributeGroup.action", {id: getFieldValue('id')});

    $('#teaAvailable').selected({
        url: '../api/trackedEntityAttributes.json?filter=trackedEntityAttributeGroup:null',
        target: $('#teaSelected'),
        search: $('#teaAvailableSearch'),
        iterator: 'trackedEntityAttributes'
    });
});
