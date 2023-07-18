## Common for all endpoints

### `*.parameter.PagingAndSortingCriteriaAdapter.page`

Get given number of pages. Defaults to `1`.

### `*.parameter.PagingAndSortingCriteriaAdapter.pageSize`

Get given number of entries per page. Defaults to `50`.

### `*.parameter.PagingAndSortingCriteriaAdapter.totalPages`

Get the total number of pages by specifying `totalPages=true`. Defaults to `false`.

### `*.parameter.PagingAndSortingCriteriaAdapter.skipPaging`

Get all entries by specifying `skipPaging=true`. Defaults to `false`, meaning that by default requests are paginated.

**Be aware that the performance is directly related to the amount of data requested. Larger pages will take more time to
return.**

### `*.parameter.PagingAndSortingCriteriaAdapter.order`

`<propertyName1:sortDirection>[,<propertyName2:sortDirection>...]`

Get entries in given order. Valid `sortDirection`s are `asc` and `desc`. `propName` is case-sensitive, `sortDirection`
is case-insensitive.
