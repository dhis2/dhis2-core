jQuery( document ).ready( function()
{
    validation2( 'addIndicatorTypeForm', function( form )
    {
        form.submit();
    }, {
        'rules' : getValidationRules( "indicatorType" )
    } );

    checkValueIsExist( "name", "validateIndicatorType.action" );
} );
