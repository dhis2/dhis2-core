jQuery( document ).ready( function()
{
    validation2( 'updateIndicatorGroupSet', function( form )
    {
        form.submit();
    }, {
        'beforeValidateHandler' : function()
        {
            $("#ingSelected").find("option").attr("selected", "selected");
            listValidator( 'ingValidator', 'ingSelected' );
        },
        'rules' : getValidationRules( "indicatorGroupSet" )
    } );
} );
