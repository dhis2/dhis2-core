const {module, inject} = angular.mock;
const {spy} = sinon;

describe('Tracker Controllers: ColumnDisplayController', () => {
    let columnDisplayController;
    let mockScope;
    let mock$modelInstance;

    beforeEach(module('d2Controllers'));
    beforeEach(inject(($injector) => {
        const $controller = $injector.get('$controller');

        mockScope = $injector.get('$rootScope').$new();
        mock$modelInstance = {
            close: spy(),
        };

        columnDisplayController = $controller('ColumnDisplayController', {
            $scope: mockScope,
            $modalInstance: mock$modelInstance,
            hiddenGridColumns: 1,
            gridColumns: 2,
        });
    }));

    it('should be a registered in the module', () => {
        expect(columnDisplayController).to.not.be.undefined;
    });

    it('should set the gridColumns onto the $scope', () => {
        expect(mockScope.gridColumns).to.equal(2);
    });

    it('should set the hiddenGridColumns onto the $scope', () => {
        expect(mockScope.hiddenGridColumns).to.equal(1);
    });

    it('should set a close() method onto the $scope', () => {
        expect(mockScope.close).to.be.a('function');
    });

    it('should call close() on the $modalInstance when $scope.close() is called', () => {
        mockScope.close();

        expect(mock$modelInstance.close).to.be.calledOnce;
    });

    it('should call close() on the $modalInstance with the number of of gridColumns', () => {
        mockScope.close();

        expect(mock$modelInstance.close).to.be.calledWith(2);
    });

    it('should create the showHideColumns method onto the $scope', () => {
        expect(mockScope.showHideColumns).to.be.a('function');
    });

    it('should descrease the hiddenGridColumns when the passed column is visible', () => {
        const column = {show: true};

        mockScope.showHideColumns(column);

        expect(mockScope.hiddenGridColumns).to.equal(0);
    });

    it('should increase the hiddenGridColumns when the passed column is visible', () => {
        const column = {show: false};

        mockScope.showHideColumns(column);

        expect(mockScope.hiddenGridColumns).to.equal(2);
    });
});
