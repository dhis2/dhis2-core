
function showUpdateValidationRuleGroupForm( context ) {
  location.href = 'showUpdateValidationRuleGroupForm.action?id=' + context.id;
}

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showValidationRuleGroupDetails( context ) {
  jQuery.post('getValidationRuleGroup.action', { 'id': context.id }, function( json ) {
    setInnerHTML('nameField', json.validationRuleGroup.name);
    setInnerHTML('descriptionField', json.validationRuleGroup.description);
    setInnerHTML('memberCountField', json.validationRuleGroup.memberCount);
    setInnerHTML('userGroupsToAlertCountField', json.validationRuleGroup.userGroupsToAlertCount);
	setInnerHTML('idField', json.validationRuleGroup.uid);

    showDetails();
  });
}

// -----------------------------------------------------------------------------
// Remove data element group
// -----------------------------------------------------------------------------

function removeValidationRuleGroup( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeValidationRuleGroup.action');
}

// -----------------------------------------------------------------------------
// Select lists
// -----------------------------------------------------------------------------

function initLists() {
  for( var id in groupMembers ) {
    $("#groupMembers").append($("<option></option>").attr("value", id).text(groupMembers[id]));
  }

  for( var id in availableValidationRules ) {
    $("#availableValidationRules").append(
      $("<option></option>").attr("value", id).text(availableValidationRules[id]));
  }

  for( var id in availableUserGroupsToAlert ) {
    $("#availableUserGroupsToAlert").append($("<option></option>").attr("value", id).text(availableUserGroupsToAlert[id]));
  }

  for( var id in selectedUserGroupsToAlert ) {
    $("#availableValidationRules").append(
      $("<option></option>").attr("value", id).text(selectedUserGroupsToAlert[id]));
  }
}

function filterGroupMembers() {
  var filter = document.getElementById('groupMembersFilter').value;
  var list = document.getElementById('groupMembers');

  list.options.length = 0;

  for( var id in groupMembers ) {
    var value = groupMembers[id];

    if( value.toLowerCase().indexOf(filter.toLowerCase()) != -1 ) {
      list.add(new Option(value, id), null);
    }
  }
}

function filterAvailableValidationRules() {
  var filter = document.getElementById('availableValidationRulesFilter').value;
  var list = document.getElementById('availableValidationRules');

  list.options.length = 0;

  for( var id in availableValidationRules ) {
    var value = availableValidationRules[id];

    if( value.toLowerCase().indexOf(filter.toLowerCase()) != -1 ) {
      list.add(new Option(value, id), null);
    }
  }
}

function addGroupMembers() {
  var list = document.getElementById('availableValidationRules');

  while( list.selectedIndex != -1 ) {
    var id = list.options[list.selectedIndex].value;

    list.options[list.selectedIndex].selected = false;

    groupMembers[id] = availableValidationRules[id];

    delete availableValidationRules[id];
  }

  filterGroupMembers();
  filterAvailableValidationRules();
}

function removeGroupMembers() {
  var list = document.getElementById('groupMembers');

  while( list.selectedIndex != -1 ) {
    var id = list.options[list.selectedIndex].value;

    list.options[list.selectedIndex].selected = false;

    availableValidationRules[id] = groupMembers[id];

    delete groupMembers[id];
  }

  filterGroupMembers();
  filterAvailableValidationRules();
}

function filterAvailableUserGroupsToAlert() {
  var filter = document.getElementById('availableUserGroupsToAlertFilter').value;
  var list = document.getElementById('availableUserGroupsToAlert');

  list.options.length = 0;

  for( var id in availableUserGroupsToAlert ) {
    var value = availableUserGroupsToAlert[id];

    if( value.toLowerCase().indexOf(filter.toLowerCase()) != -1 ) {
      list.add(new Option(value, id), null);
    }
  }
}

function filterSelectedUserGroupsToAlert() {
  var filter = document.getElementById('selectedUserGroupsToAlertFilter').value;
  var list = document.getElementById('selectedUserGroupsToAlert');

  list.options.length = 0;

  for( var id in selectedUserGroupsToAlert ) {
    var value = selectedUserGroupsToAlert[id];

    if( value.toLowerCase().indexOf(filter.toLowerCase()) != -1 ) {
      list.add(new Option(value, id), null);
    }
  }
}

function addSelectedUserGroupsToAlert() {
  var list = document.getElementById('selectedUserGroupsToAlert');

  while( list.selectedIndex != -1 ) {
    var id = list.options[list.selectedIndex].value;

    list.options[list.selectedIndex].selected = false;

    selectedUserGroupsToAlert[id] = availableUserGroupsToAlert[id];

    delete availableUserGroupsToAlert[id];
  }

  filterAvailableUserGroupsToAlert();
  filterSelectedUserGroupsToAlert();
}

function removeSelectedUserGroupsToAlert() {
  var list = document.getElementById('selectedUserGroupsToAlert');

  while( list.selectedIndex != -1 ) {
    var id = list.options[list.selectedIndex].value;

    list.options[list.selectedIndex].selected = false;

    availableUserGroupsToAlert[id] = selectedUserGroupsToAlert[id];

    delete selectedUserGroupsToAlert[id];
  }

  filterAvailableUserGroupsToAlert();
  filterSelectedUserGroupsToAlert();
}
