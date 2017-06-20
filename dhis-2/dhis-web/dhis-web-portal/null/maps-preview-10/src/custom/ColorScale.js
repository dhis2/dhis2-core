import isArray from 'd2-utilizr/lib/isArray';
import isObject from 'd2-utilizr/lib/isObject';
import colorbrewer from '../custom/colorbrewer';

const createColorScale = function(value, model) {
    const classes = model.get('classes');
    const colors = colorbrewer[model.getId()][classes];
    const scale = colors.map(color => '<li style="background:' + color + '" />').join('');
    return '<ul class="color-scale-' + classes +'">' + scale + '</ul>';
};

const colorStore = Ext.create('Ext.data.Store', {
    fields: ['id', 'classes', {name: 'scale', persist: false, convert: createColorScale}],
    data: [
        {id: 'YlOrBr',         classes: 5},
        {id: 'Reds',           classes: 5},
        {id: 'YlGn',           classes: 5},
        {id: 'Greens',         classes: 5},
        {id: 'Blues',          classes: 5},
        {id: 'BuPu',           classes: 5},
        {id: 'RdPu',           classes: 5},
        {id: 'PuRd',           classes: 5},
        {id: 'Greys',          classes: 5},
        {id: 'YlOrBr_reverse', classes: 5},
        {id: 'Reds_reverse',   classes: 5},
        {id: 'YlGn_reverse',   classes: 5},
        {id: 'Greens_reverse', classes: 5},
        {id: 'Blues_reverse',  classes: 5},
        {id: 'BuPu_reverse',   classes: 5},
        {id: 'RdPu_reverse',   classes: 5},
        {id: 'PuRd_reverse',   classes: 5},
        {id: 'Greys_reverse',  classes: 5},
        {id: 'PuOr',           classes: 5},
        {id: 'BrBG',           classes: 5},
        {id: 'PRGn',           classes: 5},
        {id: 'PiYG',           classes: 5},
        {id: 'RdBu',           classes: 5},
        {id: 'RdGy',           classes: 5},
        {id: 'RdYlBu',         classes: 5},
        {id: 'Spectral',       classes: 5},
        {id: 'RdYlGn',         classes: 5},
        {id: 'Paired',         classes: 5},
        {id: 'Pastel1',        classes: 5},
        {id: 'Set1',           classes: 5},
        {id: 'Set3',           classes: 5}
    ]
});

// Mapping between palette strings and ids
const paletteIdMap = (function() {
    const map = {};
    let palette;

    for (let id in colorbrewer) {
        if (colorbrewer.hasOwnProperty(id)) {
            for (let key in colorbrewer[id]) {
                if (colorbrewer[id].hasOwnProperty(key)) {
                    palette = colorbrewer[id][key].join(',');
                    map[palette] = id;
                }
            }
        }
    }

    return map;
}());

// ColorScale
Ext.define('Ext.ux.field.ColorScale', {
    extend: 'Ext.form.field.ComboBox',
    alias: 'widget.colorscale',
    cls: 'gis-combo',
    editable: false,
    valueField: 'id',
    displayField: 'scale',
    queryMode: 'local',
    forceSelection: true,
    tpl: '<tpl for="."><div class="x-boundlist-item color-scale">{scale}</div></tpl>',
    store: colorStore,
    classes: 5, // Default
    listeners: {
        afterRender: function() {
            this.setClasses(this.classes);
            this.reset();
        }
    },
    reset: function() { // Set first scale
        if (this.scheme) {
            this.setValue(this.scheme, true);
        } else {
            this.setValue(this.store.getAt(0).data.id, true);
        }
    },
    getValue: function() {
        if (this.scheme && this.findRecordByValue(this.scheme)) {
            return colorbrewer[this.scheme][this.findRecordByValue(this.scheme).get('classes')];
        }
    },
    setValue: function(value, doSelect) {
        this.self.superclass.setValue.call(this, value, doSelect);

        if (isArray(value)) {
            value = value[0];
        }

        if (isObject(value)) {
            value = value.getId();
        }

        // Assume palette string
        if (typeof value === 'string' && value.includes('#')) {
            value = paletteIdMap[value];
        }

        this.scheme = value;

        if (value && this.inputEl) {
            if (!this.colorEl) {
                this.colorEl = document.createElement('div');
                this.colorEl.className = 'color-scale';
                this.colorEl.style.width = this.inputEl.getWidth() + 'px';
                this.inputEl.insertSibling(this.colorEl);
            }

            const model = this.findRecordByValue(value);

            if (model) {
                this.colorEl.innerHTML = model.get('scale');
            }
        }

        return this;
    },
    setClasses: function (classes) {
        this.classes = classes;
        this.store.each(function(model){
            model.set('classes', classes);
            model.set('scale', ''); // Triggers createColorScale convert function
        });

        if (this.colorEl) {
            this.colorEl.innerHTML = this.findRecordByValue(this.scheme).get('scale');
        }
    },
    setScale: function (scale) { // String of colors
        const colors = scale.split(',');
        const classes = colors.length;
        const self = this;
        let scheme;

        this.setClasses(classes);

        this.store.each(function(model){
            const id = model.getId();

            if (scale === (colorbrewer[id][classes] || []).join()) {
                scheme = id;
            }
        });

        if (scheme) {
            this.setValue(scheme);
        }
    }
});
