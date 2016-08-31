// -----------------------------------------------------------------------------
// $Id: shortName.js 1251 2006-01-31 06:43:27Z torgeilo $
// -----------------------------------------------------------------------------

function nameChanged()
{
    var nameField = document.getElementById( 'name' );
    var shortNameField = document.getElementById( 'shortName' );
    var maxLength = parseInt( shortNameField.maxLength );

    if ( previousName != nameField.value && nameField.value.length <= maxLength
            && ( shortNameField.value == previousName || shortNameField.value.length == 0 ) )
    {
        shortNameField.value = nameField.value;
    }

    previousName = nameField.value;
}
