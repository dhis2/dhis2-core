$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showUpdateCategoryOptionGroupSetForm( context ) {
  location.href = 'showUpdateCategoryOptionGroupSetForm.action?id=' + context.id;
}

function showCategoryOptionGroupSetDetails( context ) {
  jQuery.getJSON('getCategoryOptionGroupSet.action', { id: context.id },
    function( json ) {
      setInnerHTML('nameField', json.categoryOptionGroupSet.name);
      setInnerHTML('descriptionField', json.categoryOptionGroupSet.description);
      setInnerHTML('memberCountField', json.categoryOptionGroupSet.memberCount);
      setInnerHTML('idField', json.categoryOptionGroupSet.uid);

      showDetails();
    });
}

// -----------------------------------------------------------------------------
// Remove CategoryOption
// -----------------------------------------------------------------------------

function removeCategoryOptionGroupSet( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeCategoryOptionGroupSet.action');
}
