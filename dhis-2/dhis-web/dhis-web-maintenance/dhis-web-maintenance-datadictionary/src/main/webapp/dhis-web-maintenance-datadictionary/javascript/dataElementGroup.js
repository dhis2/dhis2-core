$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

function beforeSubmit() {
  memberValidator = jQuery("#memberValidator");
  memberValidator.children().remove();

  jQuery.each(jQuery("#groupMembers").children(), function( i, item ) {
    item.selected = 'selected';
    memberValidator.append('<option value="' + item.value + '" selected="selected">' + item.value + '</option>');
  });
}

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showDataElementGroupDetails( context ) {
  jQuery.post('../dhis-web-commons-ajax-json/getDataElementGroup.action',
    { id: context.id }, function( json ) {

      setInnerHTML('nameField', json.dataElementGroup.name);
      setInnerHTML('shortNameField', json.dataElementGroup.shortName);
      setInnerHTML('codeField', json.dataElementGroup.code);
      setInnerHTML('memberCountField', json.dataElementGroup.memberCount);
      setInnerHTML('idField', json.dataElementGroup.uid);

      showDetails();
    });
}

// -----------------------------------------------------------------------------
// Remove data element group
// -----------------------------------------------------------------------------

function removeDataElementGroup( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, "removeDataElementGroup.action");
}

function showUpdateDataElementGroupForm( context ) {
  location.href = 'showUpdateDataElementGroupForm.action?id=' + context.id;
}
