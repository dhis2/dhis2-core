// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showOrganisationUnitGroupDetails( context ) {
  jQuery.post('getOrganisationUnitGroup.action', { id: context.id },
    function( json ) {
      setInnerHTML('nameField', json.organisationUnitGroup.name);
      setInnerHTML('shortNameField', json.organisationUnitGroup.shortName);
      setInnerHTML('codeField', json.organisationUnitGroup.code);
      setInnerHTML('memberCountField', json.organisationUnitGroup.memberCount);
      setInnerHTML('idField', json.organisationUnitGroup.uid);

      showDetails();
    });
}

// -----------------------------------------------------------------------------
// Remove organisation unit group
// -----------------------------------------------------------------------------

function removeOrganisationUnitGroup( context ) {
  removeItem(context.id, context.name, confirm_to_delete_org_unit_group, 'removeOrganisationUnitGroup.action');
}

function openUpdateOrganisationUnitGroupForm( context ) {
  location.href = 'openUpdateOrganisationUnitGroup.action?id=' + context.id;
}
