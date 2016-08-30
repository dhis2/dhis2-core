/* user.js already exists. This file is for user.vm */

jQuery(document).ready(function() {
  tableSorter('userList');
  selection.setOfflineLevel(1);
  selection.setListenerFunction(orgUnitSelected, true);

  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

function orgUnitSelected( orgUnitIds ) {
  window.location.href = "user.action";
}

function showUpdateUserForm( context ) {
  location.href = 'showUpdateUserForm.action?id=' + context.id;
}
