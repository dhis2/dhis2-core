$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

// -----------------------------------------------------------------------------
// Show Indicator Group Set details
// -----------------------------------------------------------------------------

function showIndicatorGroupSetDetails( context ) {
  jQuery.post('../dhis-web-commons-ajax-json/getIndicatorGroupSet.action',
    { id: context.id }, function( json ) {
      setInnerHTML('nameField', json.indicatorGroupSet.name);
      setInnerHTML('memberCountField', json.indicatorGroupSet.memberCount);
	  setInnerHTML('idField', json.indicatorGroupSet.uid);

      showDetails();
    });
}

// -----------------------------------------------------------------------------
// Delete Indicator Group Set
// -----------------------------------------------------------------------------

function deleteIndicatorGroupSet( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, "deleteIndicatorGroupSet.action");
}

function showUpdateIndicatorGroupSetForm( context ) {
  location.href = 'openUpdateIndicatorGroupSet.action?id=' + context.id;
}
