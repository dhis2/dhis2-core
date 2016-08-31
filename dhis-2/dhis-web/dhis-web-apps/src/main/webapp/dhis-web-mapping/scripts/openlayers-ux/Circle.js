OpenLayers.Control.Circle = OpenLayers.Class(OpenLayers.Control, {
    feature: null,
    layer: null,
    radius: 5,
    origin: null,
    sides: 40,
    angle: null,
    snapAngle: null,
    dragControl: null,

    initialize: function(options) {
        OpenLayers.Control.prototype.initialize.apply(this, arguments);
    },

    activate: function() {
        var activated = OpenLayers.Control.prototype.activate.call(this);
        if (activated) {
            var options = {
                displayInLayerSwitcher: false,
                calculateInRange: function() { return true; }
            };
            //this.layer = new OpenLayers.Layer.Vector(this.CLASS_NAME, options);
            //this.dragControl = new OpenLayers.Control.DragFeature(this.layer);
            //this.map.addControl(this.dragControl);
            //this.dragControl.activate();
            this.map.addLayer(this.layer);
        }
        return activated;
    },

    deactivate: function() {
        var deactivated = OpenLayers.Control.prototype.deactivate.call(this);
        if (deactivated) {
            if (this.layer.map != null) {
                this.layer.destroy(false);
                if (this.feature) {
                    this.feature.destroy();
                }
            }
            this.layer = null;
            this.feature = null;

            //this.dragControl.destroy();
            //this.dragControl = null;
        }
        return deactivated;
    },

    createGeometry: function() {
        this.angle = Math.PI * ((1 / this.sides) - (1 / 2));
        if(this.snapAngle) {
            this.angle += this.snapAngle * (Math.PI / 180);
        }
        this.feature.geometry = OpenLayers.Geometry.Polygon.createRegularPolygon(
            this.origin, this.radius, this.sides, this.snapAngle
        );
    },

    modifyGeometry: function() {
        var angle, dx, dy, point;
        var ring = this.feature.geometry.components[0];
        if (ring.components.length != (this.sides + 1)) {
            this.createGeometry();
            ring = this.feature.geometry.components[0];
        }
        for (var i = 0; i < this.sides; ++i) {
            point = ring.components[i];
            angle = this.angle + (i * 2 * Math.PI / this.sides);
            point.x = this.origin.x + (this.radius * Math.cos(angle));
            point.y = this.origin.y + (this.radius * Math.sin(angle));
            point.clearBounds();
        }
    },

    updateCircle: function(origin, radius) {
        this.origin = new OpenLayers.Geometry.Point(origin.lon, origin.lat);
        this.radius = radius * 1; // gives m when projection is 4326
        if (!this.feature) {
            this.feature = new OpenLayers.Feature.Vector();
            this.createGeometry();
            this.layer.addFeatures([this.feature], {
				silent: true
			});
        }
        else {
            this.modifyGeometry();
        }

        this.layer.drawFeature(this.feature, this.style);
    },

    CLASS_NAME: "Meteorage.Circle"
});

//OpenLayers.Geometry.Polygon.prototype.getCentroid = function() {
    //var centroid = this.components[0].getCentroid();
    //return centroid;
//};

//OpenLayers.Geometry.LinearRing.prototype.getCentroid = function() {
    //if (this.components && (this.components.length > 2)) {
        //var sumX = 0.0;
        //var sumY = 0.0;
        //for (var i = 0; i < this.components.length - 1; i++) {
            //var b = this.components[i];
            //var c = this.components[i+1];
            //sumX += (b.x + c.x) * (b.x * c.y - c.x * b.y);
            //sumY += (b.y + c.y) * (b.x * c.y - c.x * b.y);
        //}
        //var x = sumX / (6 * Math.abs(this.getArea()));
        //var y = sumY / (6 * Math.abs(this.getArea()));
    //}
    //return new OpenLayers.Geometry.Point(x, y);
//};
