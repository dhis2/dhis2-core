// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

// -----------------------------------------------------------------------------
// Context Menu Actions
// -----------------------------------------------------------------------------

function showUpdateConcept( context ) {
  location.href = 'showUpdateConceptForm.action?id=' + context.id;
}

function showConceptDetails( context ) {
  $.post('getConcept.action', { id: context.id }, function( json ) {
    setInnerHTML('nameField', json.concept.name);
    showDetails();
  });
}

function removeConcept( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeConcept.action');
}
