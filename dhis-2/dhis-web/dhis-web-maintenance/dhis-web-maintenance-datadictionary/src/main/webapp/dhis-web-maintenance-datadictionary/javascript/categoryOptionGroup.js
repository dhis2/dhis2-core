$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showUpdateCategoryOptionGroupForm( context ) {
  location.href = 'showUpdateCategoryOptionGroupForm.action?id=' + context.id;
}

function showCategoryOptionGroupDetails( context ) {
  jQuery.getJSON('getCategoryOptionGroup.action', { id: context.id },
    function( json ) {
      setInnerHTML('nameField', json.categoryOptionGroup.name);
      setInnerHTML('codeField', json.categoryOptionGroup.code);
      setInnerHTML('shortNameField', json.categoryOptionGroup.shortName);
      setInnerHTML('memberCountField', json.categoryOptionGroup.memberCount);
      setInnerHTML('idField', json.categoryOptionGroup.uid);

      showDetails();
    });
}

// -----------------------------------------------------------------------------
// Remove CategoryOption
// -----------------------------------------------------------------------------

function removeCategoryOptionGroup( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeCategoryOptionGroup.action');
}
