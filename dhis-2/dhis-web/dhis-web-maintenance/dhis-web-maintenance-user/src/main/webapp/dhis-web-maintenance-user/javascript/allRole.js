
jQuery(document).ready(function() {
  tableSorter('listTable');

  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

function showUpdateRoleForm( context ) {
  location.href = 'showUpdateRoleForm.action?id=' + context.id;
}
