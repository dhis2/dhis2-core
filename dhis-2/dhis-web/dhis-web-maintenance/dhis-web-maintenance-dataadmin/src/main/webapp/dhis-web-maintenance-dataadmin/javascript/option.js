
// -----------------------------------------------------------------------------
// Update Option
// -----------------------------------------------------------------------------

function showUpdateOptionForm( context ) {
  location.href = 'showUpdateOptionForm.action?optionId=' + context.id + '&optionSetId=' + getFieldValue('optionSetId');
}

// -----------------------------------------------------------------------------
// Remove Option
// -----------------------------------------------------------------------------

function removeOptionSet( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeOption.action?optionSetId=' + getFieldValue('optionSetId'));
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

// -----------------------------------------------------------------------------
// Add options constant
// -----------------------------------------------------------------------------

function showOptionDetails( context ) {
  jQuery.get('getOption.action', { optionId: context.id },
    function( json ) {
      setInnerHTML('nameField', json.option.name);
      setInnerHTML('codeField', json.option.code);
      setInnerHTML('idField', json.option.uid);
      showDetails();
    });
}