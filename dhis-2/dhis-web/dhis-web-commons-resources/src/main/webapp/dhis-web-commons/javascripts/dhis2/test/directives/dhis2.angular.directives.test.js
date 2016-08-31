describe('Directives: d2Drirectives', () => {

    const {module, inject} = angular.mock;
    const {spy} = sinon;
    beforeEach(module('d2Directives'));

    describe('Directive: d2SetFocus', () => {
        let mock$scope;
        let element;
        let $timeout;
        let render;

        beforeEach(inject(($injector) => {
            const $compile = $injector.get('$compile');
            const $rootScope = $injector.get('$rootScope');
            $timeout = $injector.get('$timeout');
            mock$scope = $rootScope.$new();
            mock$scope.isFocused = false;
            render = (elm) =>
            {
                elm[0].focus = spy();
                $compile(elm)(mock$scope);
                mock$scope.$digest();
                $timeout.flush();
            };
        }));

        it('should set the focus when the attribute property is set to true', () => {
            var elm = angular.element('<input d2-set-focus="true" />');
            render(elm);
            expect(elm[0].focus).to.be.calledOnce;
        });

        it('should not set focus when the attribute property is set to false', () => {
            var elm = angular.element('<input d2-set-focus="false" />');
            render(elm);
            expect(elm[0].focus).to.not.be.called;
        });
    });


    describe('Directive: d2Enter', () => {
        let mock$scope;
        let element;
        let $timeout;
        let render;

        beforeEach(inject(($injector) => {
            const $compile = $injector.get('$compile');
            const $rootScope = $injector.get('$rootScope');

            mock$scope = $rootScope.$new();
            mock$scope.search = function() {

            };
            mock$scope.message="testMessage";
            render = (elm) => {
                mock$scope.search = spy();
                $compile(elm)(mock$scope);
                mock$scope.$digest();
            };
        }));

        it('should call the resgistered function on key press event', () => {
            var elm = angular.element('<input type="text" d2-enter="search(message)"/>');
            render(elm);
            var e = jQuery.Event("keydown");
            e.which = 13; // # Some key code value
            elm .trigger(e);
            expect(mock$scope.search).to.be.calledOnce;
        });
    });

    describe('Directive: d2PopOver', () => {
        let mock$scope;
        let element;
        let $timeout;
        let render;
        let mySpy;
        let testContentString = "Sample Content";
        beforeEach(inject(($injector) => {
            const $compile = $injector.get('$compile');
            const $rootScope = $injector.get('$rootScope');
            mock$scope = $rootScope.$new();
            render = (elm) => {
                elm[0].popover = function(obj) {
                    expect(obj.content[0].innerHTML).to.equal(testContentString);
                    expect(obj.html).to.equal(true);
                    expect(obj.placement).to.equal("bottom");
                    expect(obj.title).to.equal("testDetails");
                    expect(obj.trigger).to.equal("hover");
                }
                mySpy = spy(elm[0], "popover");
                $compile(elm)(mock$scope);
                mock$scope.$digest();
            };
        }));

        it('should call the popover function, with correct arguments, when loaded', () => {
            var elm = angular.element('<d2-pop-over content="testContent" template="popover.html" details="testDetails"></d2-pop-over>'+
            '<script type="text/ng-template" id="popover.html"><p>'+testContentString+'</p></script>');
            render(elm);
            expect(mySpy).to.have.been.calledOnce;
        });
    });

    /*TODO: Test not added for d2Sortable*/

    describe('Directive: serversidePaginator', () => {
        let mock$scope;
        let element;
        let render;
        beforeEach(inject(($injector) => {
        const $compile = $injector.get('$compile');
            const $rootScope = $injector.get('$rootScope');
            $timeout = $injector.get('$timeout');
            mock$scope = $rootScope.$new();

            render = (elm) =>
            {
                $compile(elm)(mock$scope);
                mock$scope.$digest();
            };
        }));
        /* TODO ../dhis-web-commons/angular-forms/serverside-pagination.html is not loaded. */
        /* works if the path in specified as ../../../dhis-web-commons/angular-forms/serverside-pagination.html */
        xit('should set the focus when the attribute property is set to true', () => {
            var elm = '<serverside-paginator></serverside-paginator>';
            render(elm);
            expect(mock$scope.paginator).to.be.defined();
        });
    });



    describe('Directive: d2CustomForm', () => {
        let mock$scope;
        let render;
        let $compile;
        beforeEach(inject(($injector) => {
            $compile = $injector.get('$compile');
            mock$scope = $injector.get('$rootScope').$new();
        }));
        it('should watch and process the customForm scope variable', () => {
            let elm = angular.element('<d2-custom-form></d2-custom-form>');
            $compile(elm)(mock$scope);
            mock$scope.customForm= {htmlCode:'<div>TestContentOne</div>'};
            mock$scope.$digest();
            expect(elm[0].innerHTML).to.equal('<div class="ng-scope">TestContentOne</div>');
        });
    });


    /*TODO: The directive purpose and implenentation needs to be discussed before writing the tests.*/

    xdescribe('Directive: d2ContextMenu', () => {
        let mock$scope;
        let element;
        let $timeout;
        let render;

        beforeEach(inject(($injector) => {
            const $compile = $injector.get('$compile');
            const $rootScope = $injector.get('$rootScope');
            mock$scope = $rootScope.$new();
            render = (elm) => {
                $compile(elm)(mock$scope);
                mock$scope.$digest();
            };
        }));

        it('should call the resgistered function on click event', () => {
            var elm = angular.element('<div id="contextMenu"></div><div d2-context-menu></div>');
            render(elm);
            var contextMenu =  elm.find('#contextMenu');
            contextMenu.show = spy();
            console.log(elm[0].innerHTML);

            var e = jQuery.Event("click");

            elm .trigger(e);
            console.log(elm[0].innerHTML);


        });
    });
});