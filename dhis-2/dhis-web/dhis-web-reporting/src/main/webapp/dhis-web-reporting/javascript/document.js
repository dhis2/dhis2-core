
function removeDocument( context ) {
  removeItem(context.id, context.name, i18n_confirm_remove_report, "removeDocument.action");
}

function toggleExternal() {
  var external = getListValue("external");

  if( external == "true" ) {
    document.getElementById("fileDiv").style.display = "none";
    document.getElementById("urlDiv").style.display = "block";
    $('#url').attr('class', '{validate:{required:true}}');
    $('#upload').attr('class', '');
  } else {
    document.getElementById("fileDiv").style.display = "block";
    document.getElementById("urlDiv").style.display = "none";
    if( byId('id').value == "" ) {
      $('#upload').attr('class', '{validate:{required:true}}');
      $('#url').attr('class', '');
    }
  }
}

function displayAddDocumentForm( context ) {
  location.href = 'displayAddDocumentForm.action?id=' + context.id;
}

function runDocument( context ) {
  console.log(context);

  if( context.external ) {
    location.href = context.url;
  } else {
    location.href = '../api/documents/' + context.uid + '/data';
  }
}
