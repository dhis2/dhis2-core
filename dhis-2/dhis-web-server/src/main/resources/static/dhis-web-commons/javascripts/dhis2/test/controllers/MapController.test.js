const {module, inject} = angular.mock;
const {spy, stub} = sinon;

describe('Tracker Controllers: MapController', () => {
    let mapController;
    let mockScope;
    let mock$modelInstance;
    let mockCurrentSelection;

    beforeEach(module('d2Controllers'));
    beforeEach(inject(($injector) => {
        const $controller = $injector.get('$controller');

        mockScope = $injector.get('$rootScope').$new();
        mock$modelInstance = {
            close: spy(),
        };
        mockCurrentSelection = {
            getLocation: stub().returns('theCapturedLocation'),
        };

        mapController = $controller('MapController', {
            $scope: mockScope,
            $modalInstance: mock$modelInstance,
            CurrentSelection: mockCurrentSelection,
            DHIS2URL: 'http://localhost:8080/dhis',
            location: 'theInjectedLocation',
        });
    }));

    it('should be a registered in the module', () => {
        expect(mapController).to.not.be.undefined;
    });

    it('should create a home() function on the $scope', () => {
        expect(mockScope.home).to.be.a('function');
    });

    it('should create a close() function on the scope', () => {
        expect(mockScope.close).to.be.a('function');
    });

    it('should create a captureCoordinate function on the scope', () => {
        expect(mockScope.captureCoordinate).to.be.a('function');
    });

    it('should set the injected location onto the $scope', () => {
        expect(mockScope.location).to.equal('theInjectedLocation');
    });

    it('should call close() on the $modelInstance when calling close() on the $scope', () => {
        mockScope.close();

        expect(mock$modelInstance.close).to.be.calledOnce;
    });

    it('should call close on the $modelInstance when calling captureCoordinate on the $scope', () => {
        mockScope.captureCoordinate();

        expect(mock$modelInstance.close).to.be.calledOnce;
    });

    it('should set the location on $scope to the location from CurrentLocation.getLocation()', () => {
        mockScope.captureCoordinate();

        expect(mockScope.location).to.equal('theCapturedLocation');
    });
});
