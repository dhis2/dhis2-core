function onValueTypeChange(e) {
  var val = $(this).find(":selected").val();

  if( val == "option_set" ) {
    $('#optionSet').show();
  } else {
    $('#optionSet').hide();
  }
}

function onOptionSetChange(e) {
  var val = $(this).find(":selected").val();

  if( val != 0 ) {
    $('#valueType').css({
      color: 'white',
      backgroundColor: 'lightgrey'
    }).attr('disabled', true);
  } else {
    $('#valueType').css({
      color: 'black',
      backgroundColor: 'white'
    }).removeAttr('disabled');
  }
}

function showAttributeDetails(context) {
  jQuery.post('getAttribute.action', { id: context.id },
    function(json) {
      setInnerHTML('nameField', json.attribute.name);
      setInnerHTML('mandatoryField', json.attribute.mandatory);
      setInnerHTML('dataelementField', json.attribute.dataelement);
      setInnerHTML('indicatorField', json.attribute.indicator);
      setInnerHTML('organisationunitField', json.attribute.organisationunit);
      setInnerHTML('userField', json.attribute.user);
      setInnerHTML('valuetypeField', json.attribute.valueType);
      setInnerHTML('idField', json.attribute.uid);
      showDetails();
    });
}

function removeAttribute(context) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeAttribute.action');
}

function showUpdateAttributeForm(context) {
  location.href = 'showUpdateAttributeForm.action?id=' + context.id;
}