// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showConstantDetails( context ) {
  jQuery.post('getConstant.action', { id: context.id },
    function( json ) {
      setInnerHTML('nameField', json.constant.name);
      setInnerHTML('shortNameField', json.constant.shortName);
      setInnerHTML('codeField', json.constant.code);
      setInnerHTML('valueField', json.constant.value);
      setInnerHTML('idField', json.constant.uid);
      showDetails();
    });
}

// -----------------------------------------------------------------------------
// Remove category constant
// -----------------------------------------------------------------------------

function removeConstant( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeConstant.action');
}

function showUpdateConstantForm( context ) {
  location.href = 'showUpdateConstantForm.action?id=' + context.id;
}