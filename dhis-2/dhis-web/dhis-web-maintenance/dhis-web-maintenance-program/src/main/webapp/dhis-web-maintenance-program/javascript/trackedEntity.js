$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showUpdateTrackedEntityForm( context ) {
  location.href = 'showUpdateTrackedEntityForm.action?id=' + context.id;
}

function showTrackedEntityDetails( context ) {
  jQuery.getJSON('getTrackedEntityType.action', { id: context.id },
    function( json ) {
      setInnerHTML('nameField', json.trackedEntityType.name);
      setInnerHTML('descriptionField', json.trackedEntityType.description);
      setInnerHTML('idField', json.trackedEntityType.uid);

      showDetails();
    });
}

// -----------------------------------------------------------------------------
// Remove Attribute
// -----------------------------------------------------------------------------

function removeTrackedEntityType( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeTrackedEntity.action');
}
