$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showUpdateProgramIndicatorGroupForm( context ) {
  location.href = 'showUpdateProgramIndicatorGroupForm.action?id=' + context.id;
}

function showProgramIndicatorGroupDetails( context ) {
  jQuery.getJSON('getProgramIndicatorGroup.action', { id: context.id },
    function( json ) {
      setInnerHTML('nameField', json.programIndicatorGroup.name);
      setInnerHTML('descriptionField', json.programIndicatorGroup.description);
      setInnerHTML('noAttributeField', json.programIndicatorGroup.noAttribute);
      setInnerHTML('idField', json.programIndicatorGroup.uid);

      showDetails();
    });
}

// -----------------------------------------------------------------------------
// Remove Attribute
// -----------------------------------------------------------------------------

function removeProgramIndicatorGroup( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeProgramIndicatorGroup.action');
}
