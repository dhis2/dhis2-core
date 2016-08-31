jQuery( document ).ready( function ()
{

    selectionTreeSelection.setListenerFunction( organisationUnitModeSelected );
    selectionTreeSelection.setMultipleSelectionAllowed( false );
    selectionTree.clearSelectedOrganisationUnitsAndBuildTree();

    datePickerInRange( 'fromDate', 'toDate' );

    validation2( 'databrowser', function ( form )
    {
        validateBeforeSubmit( form );
    }, {
        'rules':getValidationRules( "dataBrowser" )
    } );
} );

var flag;
window.onload = modeHandler;
