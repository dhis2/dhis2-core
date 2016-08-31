$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showUpdateAttributeGroupForm( context ) {
  location.href = 'showUpdateAttributeGroupForm.action?id=' + context.id;
}

function showAttributeGroupDetails( context ) {
  jQuery.getJSON('getAttributeGroup.action', { id: context.id },
    function( json ) {
      setInnerHTML('nameField', json.attributeGroup.name);
      setInnerHTML('descriptionField', json.attributeGroup.description);
      setInnerHTML('noAttributeField', json.attributeGroup.noAttribute);
      setInnerHTML('idField', json.attributeGroup.uid);

      showDetails();
    });
}

// -----------------------------------------------------------------------------
// Remove Attribute
// -----------------------------------------------------------------------------

function removeAttributeGroup( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeAttributeGroup.action');
}
