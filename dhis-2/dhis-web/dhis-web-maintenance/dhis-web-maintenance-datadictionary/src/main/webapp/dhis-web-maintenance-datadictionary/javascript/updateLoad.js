window.onload = function()
{
    var container = byId( 'selectedDataElements' );

    if ( container != null )
    {
        var buttons = container.getElementsByTagName( 'button' );
        for ( var i = 0, button; button = buttons[i]; i++ )
        {
            button.onclick = removeCDEDataElement;
        }
    }
};
