$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showUpdateRelationshipTypeForm( context ) {
  location.href = 'showUpdateRelationshipTypeForm.action?id=' + context.id;
}

function showRelationshipTypeDetails( context ) {
  jQuery.getJSON('getRelationshipType.action', { id: context.id }, function( json ) {
    setInnerHTML('aIsToBField', json.relationshipType.aIsToB);
    setInnerHTML('bIsToAField', json.relationshipType.bIsToA);
    setInnerHTML('descriptionField', json.relationshipType.description);
    setInnerHTML('idField', json.relationshipType.uid);

    showDetails();
  });
}

// -----------------------------------------------------------------------------
// Add RelationshipType
// -----------------------------------------------------------------------------

function validateAddRelationshipType() {
  $.postUTF8(
    'validateRelationshipType.action',
    {
      "aIsToB": getFieldValue('aIsToB'),
      "bIsToA": getFieldValue('bIsToA')
    },
    function( json ) {
      if( json.response == "success" ) {
        var form = document.getElementById('addRelationshipTypeForm');
        form.submit();
      } else if( json.response == "input" ) {
        setHeaderMessage(json.message);
      }
      else if( json.response == "error" ) {
        setHeaderMessage(json.message);
      }
    }
  );
}

// -----------------------------------------------------------------------------
// Update RelationshipType
// -----------------------------------------------------------------------------

function validateUpdateRelationshipType() {
  $.postJSON(
    'validateRelationshipType.action',
    {
      "id": getFieldValue('id'),
      "aIsToB": getFieldValue('aIsToB'),
      "bIsToA": getFieldValue('bIsToA')
    },
    function( json ) {
      if( json.response == "success" ) {
        var form = document.getElementById('updateRelationshipTypeForm');
        form.submit();
      } else if( json.response == "input" ) {
        setHeaderMessage(json.message);
      }
      else if( json.response == "error" ) {
        setHeaderMessage(json.message);
      }
    }
  );
}

// -----------------------------------------------------------------------------
// Remove RelationshipType
// -----------------------------------------------------------------------------	

function removeRelationshipType( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeRelationshipType.action');
}
