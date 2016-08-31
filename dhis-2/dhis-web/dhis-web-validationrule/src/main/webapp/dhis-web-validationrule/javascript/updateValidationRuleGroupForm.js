jQuery( document ).ready( function()
{
    jQuery( "#name" ).focus();
    
    validation2( 'updateValidationRuleGroupForm', function( form )
    {
        form.submit();
    }, {
        'beforeValidateHandler' : function()
        {
            selectAllById( 'groupMembers' );
            selectAllById( 'userGroupsToAlert' );
        },
        'rules' : getValidationRules( "validationRuleGroup" )
    } );

    checkValueIsExist( "name", "validateValidationRuleGroup.action", {
        id : getFieldValue( 'id' )
    } );
} );
