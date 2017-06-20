/* ColorButton */
Ext.define('Ext.ux.button.ColorButton', {
    extend: 'Ext.button.Button',
    alias: 'widget.colorbutton',
    disabledCls: 'gis-colorbutton-disabled',
    width: 109,
    height: 22,
    defaultValue: null,
    value: 'f1f1f1',
    getValue: function() {
        return this.value;
    },
    setValue: function(color) {
        this.value = color;
        if (Ext.isDefined(this.getEl())) {
            this.getEl().dom.style.background = '#' + color;
        }
    },
    reset: function() {
        this.setValue(this.defaultValue);
    },
    menu: {},
    menuHandler: function() {},
    initComponent: function() {
        var that = this;
        this.defaultValue = this.value;
        this.menu = Ext.create('Ext.menu.Menu', {
            showSeparator: false,
            items: {
                xtype: 'colorpicker',
                closeAction: 'hide',
                width: 163,
                height: 179,
                colors: [
                    'FFFFFF', 'FFDADA', 'FFEEDA', 'FFFFDA', 'DAFFDA', 'DAFFFF', 'DAEEFF', 'DADAFF', 'F7DAFF',
                    'E2E2E2', 'FFB6B6', 'FFDDB6', 'FFFFB6', 'B6FFB6', 'B6FFFF', 'B6DDFF', 'B6B6FF', 'F0B6FF',
                    'C6C6C6', 'FF9191', 'FFCC91', 'FFFF91', '91FF91', '91FFFF', '91CCFF', '9191FF', 'E991FF',
                    'AAAAAA', 'FF6D6D', 'FFBB6D', 'FFFF6D', '6DFF6D', '6DFFFF', '6DBBFF', '6D6DFF', 'E16DFF',
                    '8D8D8D', 'FF4848', 'FFAA48', 'FFFF48', '48FF48', '48FFFF', '48AAFF', '4848FF', 'DA48FF',
                    '717171', 'FF2424', 'FF9924', 'FFFF24', '24FF24', '24FFFF', '2499FF', '2424FF', 'D324FF',
                    '555555', 'FF0000', 'FF8800', 'FFFF00', '00FF00', '00FFFF', '0088FF', '0000FF', 'CC00FF',
                    '383838', 'BF0000', 'BF6600', 'E2E200', '00BF00', '00CCCC', '006CCC', '0000CC', 'A300CC',
                    '1C1C1C', '7F0000', '7F4400', 'C6C600', '007F00', '007F7F', '00447F', '00007F', '66007F',
                    '000000', '3F0000', '3F2200', '8D8D00', '003F00', '003F3F', '00223F', '00003F', '33003F'

                ],
                listeners: {
                    select: function(cp, color) {
                        that.setValue(color);
                        that.menu.hide();
                        that.menuHandler(cp, color);
                    }
                }
            }
        });
        this.callParent();
    },
    listeners: {
        render: function() {
            this.setValue(this.value);
        }
    }
});