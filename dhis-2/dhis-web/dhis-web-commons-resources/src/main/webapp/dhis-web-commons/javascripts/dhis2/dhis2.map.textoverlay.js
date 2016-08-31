// https://developers.google.com/maps/documentation/javascript/customoverlays
/** @constructor */
function Dhis2TextOverlay(pos, txt, cls, map) {

    // Initialize properties.
    this.pos = pos;
    this.txt_ = txt;
    this.cls_ = cls;
    this.map_ = map;

    // Div to contain text
    this.div_ = null;

    // Explicitly call setMap() on this overlay
    this.setMap(map);
}

Dhis2TextOverlay.prototype = new google.maps.OverlayView();


/**
 * onAdd is called when the map's panes are ready and the overlay has been
 * added to the map.
 */
Dhis2TextOverlay.prototype.onAdd = function () {

    // Create the DIV and set some basic attributes.
    var div = document.createElement('DIV');
    div.className = this.cls_;
    div.innerHTML = this.txt_;

    // Set the overlay's div_ property to this DIV
    this.div_ = div;
    var overlayProjection = this.getProjection();
    var position = overlayProjection.fromLatLngToDivPixel(this.pos);
    div.style.left = position.x + 'px';
    div.style.top = position.y + 'px';
    
    // Add the element to the "overlayLayer" pane.
    var panes = this.getPanes();
    panes.floatPane.appendChild(div);
};

Dhis2TextOverlay.prototype.draw = function () {

    // We use the south-west and north-east
    // coordinates of the overlay to peg it to the correct position and size.
    // To do this, we need to retrieve the projection from the overlay.
    var overlayProjection = this.getProjection();

    // Retrieve the southwest and northeast coordinates of this overlay
    // in latlngs and convert them to pixels coordinates.
    // We'll use these coordinates to resize the DIV.
    var position = overlayProjection.fromLatLngToDivPixel(this.pos);

    // Resize the div to fit the indicated dimensions.
    var div = this.div_;
    div.style.left = position.x + 'px';
    div.style.top = position.y + 'px';
};

// The onRemove() method will be called automatically from the API if
// we ever set the overlay's map property to 'null'
Dhis2TextOverlay.prototype.onRemove = function () {
    this.div_.parentNode.removeChild(this.div_);
    this.div_ = null;
};

// Set the visibility to 'hidden' or 'visible'.
Dhis2TextOverlay.prototype.hide = function () {
    if (this.div_) {
        this.div_.style.visibility = "hidden";
    }
};

Dhis2TextOverlay.prototype.show = function () {
    if (this.div_) {
        this.div_.style.visibility = "visible";
    }
};

Dhis2TextOverlay.prototype.toggle = function () {
    if (this.div_) {
        if (this.div_.style.visibility === "hidden") {
            this.show();
        }
        else {
            this.hide();
        }
    }
};

// Detach the map from the DOM via toggleDOM().
// Note that if we later reattach the map, it will be visible again,
// because the containing <div> is recreated in the overlay's onAdd() method.
Dhis2TextOverlay.prototype.toggleDOM = function () {
    if (this.getMap()) {
        this.setMap(null);
    }
    else {
        this.setMap(this.map_);
    }
};
