jQuery(document).ready(function() {
  tableSorter('userList');

  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive',
    listItemProps: ['id', 'uid', 'name', 'type', 'username']
  });
});
