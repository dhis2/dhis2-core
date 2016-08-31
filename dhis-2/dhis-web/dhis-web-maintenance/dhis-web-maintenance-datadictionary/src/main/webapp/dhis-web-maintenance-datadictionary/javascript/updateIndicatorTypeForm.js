jQuery( document ).ready( function()
{
    validation2( 'updateIndicatorTypeForm', function( form )
    {
        form.submit();
    }, {
        'rules' : getValidationRules( "indicatorType" )
    } );

    var nameField = document.getElementById( 'name' );
    nameField.select();
    nameField.focus();
} );
