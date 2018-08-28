
function scrub(id = '') {
    return id
        .replace('@dhis2/')
        .replace('-app', '') 
}

/**
 * Sanitize the package name to something we can use in the bean
 * definitions, etc.
 *
 * Examples:
 * ---------
 * @dhis2/data-visualizer-app   => dhis-web-data-visualizer
 * data-visualizer-app          => dhis-web-data-visualizer
 * data-visualizer              => dhis-web-data-visualizer
 *
 * @return  string
 */
function appName(id = '', prefix = 'dhis-web-') {
    return prefix + scrub(id)
}

module.exports = {
    appName
}
