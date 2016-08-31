jQuery( document ).ready( function()
{
    jQuery( "#name" ).focus();

    validation2( 'addValidationRuleForm', function( form )
    {
        form.submit();
    }, {
        'rules' : getValidationRules( "validationRule" )
    } );

    checkValueIsExist( "name", "validateValidationRule.action" );
} );
