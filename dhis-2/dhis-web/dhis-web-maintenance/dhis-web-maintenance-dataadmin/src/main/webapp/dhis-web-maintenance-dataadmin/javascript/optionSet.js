// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showUpdateOptionSetForm( context ) {
  location.href = 'showUpdateOptionSetForm.action?id=' + context.id;
}

function showOptionSetDetails( context ) {
  jQuery.post('getOptionSet.action', { id: context.id },
    function( json ) {
      setInnerHTML('nameField', json.optionSet.name);
      setInnerHTML('optionCount', json.optionSet.optionCount);
      setInnerHTML('idField', json.optionSet.uid);
      showDetails();
    });
}

function showOptionList( context ) {
  location.href = 'option.action?optionSetId=' + context.id;
}

// -----------------------------------------------------------------------------
// Remove category constant
// -----------------------------------------------------------------------------

function removeOptionSet( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeOptionSet.action');
}

// -----------------------------------------------------------------------------
// Add options constant
// -----------------------------------------------------------------------------

function addOption() {
  var value = getFieldValue('option');

  if( value.length == 0 ) {
    markInvalid('option', i18n_specify_option_name);
  }
  else if( listContainsById('options', value, true) ) {
    markInvalid('option', i18n_option_name_already_exists);
  }
  else {
    addOptionById('options', value, value);
  }

  setFieldValue('option', '');
  $("#option").focus();
}

function updateOption() {
  var value = getFieldValue('option');
  jQuery('#options option:selected').val(value);
  jQuery('#options option:selected').text(value);

  setFieldValue('option', '');
  $("#option").focus();
}
