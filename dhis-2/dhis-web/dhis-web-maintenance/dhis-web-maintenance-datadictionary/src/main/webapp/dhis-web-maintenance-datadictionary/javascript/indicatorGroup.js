$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

// -----------------------------------------------------------------------------
// Show Indicator Group details
// -----------------------------------------------------------------------------

function showIndicatorGroupDetails( context ) {
  jQuery.get('../dhis-web-commons-ajax-json/getIndicatorGroup.action',
    { id: context.id }, function( json ) {
      setInnerHTML('nameField', json.indicatorGroup.name);
      setInnerHTML('memberCountField', json.indicatorGroup.memberCount);
	  setInnerHTML('idField', json.indicatorGroup.uid);

      showDetails();
    });
}

// -----------------------------------------------------------------------------
// Remove indicator group
// -----------------------------------------------------------------------------

function removeIndicatorGroup( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeIndicatorGroup.action');
}

function showUpdateIndicatorGroupForm( context ) {
  location.href = 'showUpdateIndicatorGroupForm.action?id=' + context.id;
}
