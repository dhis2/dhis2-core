jQuery( document ).ready( function()
{
    validation2( 'updateDataElementGroupSet', function( form )
    {
        form.submit();
    }, {
        'beforeValidateHandler' : function() {
            beforeSubmit();
            $("#degSelected").find("option").attr("selected", "selected");
        },
        'rules' : getValidationRules( "dataElementGroupSet" )
    } );

    checkValueIsExist( "name", "validateDataElementGroupSet.action", {
        id : getFieldValue( 'id' )
    } );

    var nameField = document.getElementById( 'name' );
    nameField.select();
    nameField.focus();
} );
