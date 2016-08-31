
$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

function showDataElementCategoryComboDetails( context ) {
    jQuery.post( 'getDataElementCategoryCombo.action', { id: context.id },
		function ( json) {
			setInnerHTML( 'nameField', json.dataElementCategoryCombo.name );
			setInnerHTML( 'dataElementCategoryCountField', json.dataElementCategoryCombo.dataElementCategoryCount );
			setInnerHTML( 'idField', json.dataElementCategoryCombo.uid );

			showDetails();
	});
}

// -----------------------------------------------------------------------------
// Delete Category
// -----------------------------------------------------------------------------

function removeDataElementCategoryCombo( context ) {
    removeItem( context.id, context.name, i18n_confirm_delete, 'removeDataElementCategoryCombo.action' );
}

// ----------------------------------------------------------------------
// Validation
// ----------------------------------------------------------------------

function validateSelectedCategories( form ) {
    var url = "validateDataElementCategoryCombo.action?";
    url += getParamString( "selectedList", "selectedCategories" );

    jQuery.postJSON( url, {}, function( json )
    {
        if ( json.response == 'success' )
        {
            form.submit();
        } else
            markInvalid( 'selectedCategories', json.message );
    } );

}

function showUpdateDataElementCategoryComboForm( context ) {
  location.href = 'showUpdateDataElementCategoryComboForm.action?id=' + context.id;
}
