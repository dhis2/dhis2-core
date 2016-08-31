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
  jQuery.getJSON('getTrackedEntity.action', { id: context.id },
    function( json ) {
      setInnerHTML('nameField', json.trackedEntity.name);
      setInnerHTML('descriptionField', json.trackedEntity.description);
      setInnerHTML('idField', json.trackedEntity.uid);

      showDetails();
    });
}

// -----------------------------------------------------------------------------
// Remove Attribute
// -----------------------------------------------------------------------------

function removeTrackedEntity( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeTrackedEntity.action');
}
