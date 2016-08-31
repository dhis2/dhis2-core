jQuery( document ).ready( function()
{
    jQuery( "#name" ).focus();

    changeRuleType();

    validation2( 'updateValidationRuleForm', function( form )
    {
        form.submit();
    }, {
        'rules' : getValidationRules( "validationRule" )
    } );

    checkValueIsExist( "name", "validateValidationRule.action", {
        id : getFieldValue( 'id' )
    } );
} );
