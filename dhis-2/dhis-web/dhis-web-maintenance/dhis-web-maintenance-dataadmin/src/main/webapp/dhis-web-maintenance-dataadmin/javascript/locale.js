$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showLocaleDetails( context ) {
  jQuery.getJSON("getLocale.action", {
    id: context.id
  }, function( json ) {
    setInnerHTML('nameField', json.i18nLocale.name);
    setInnerHTML('localeField', json.i18nLocale.locale);
    setInnerHTML('idField', json.i18nLocale.uid);

    showDetails();
  });
}

// -----------------------------------------------------------------------------
// Remove I18nLocale
// -----------------------------------------------------------------------------

function removeLocale( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeLocale.action');
}
