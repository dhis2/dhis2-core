
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
 * @return string
 */
function sanitize_app_name (id = '', prefix = 'dhis-web-') {
    return prefix + scrub(id)
}

/**
 * A repo path can contain a '#' after the URL to the repo, which can
 * signifiy a commit-ish or tree-ish.
 *
 * Git itself doesn't respect this addition so we need to manually parse
 * it.
 *
 * Examples:
 * ---------
 * git@github.com:d2-ci/ui.git#foobar => { 
 *      url: git@github.com:d2-ci/ui.git, 
 *      hash: foobar
 * }
 *
 * @return object
 */
function split_repo_path (path) {
    const frags = path.split('#')
    
    return {
        url: frags[0],
        hash: frags[1]
    }
}

/**
 *  Get the target destination for a given repo url
 *
 *  git@github.com:d2-ci/ui.git     => ui
 *  git@github.com:d2-ci/ui         => ui
 *  https://github.com/d2-ci/ui     => ui
 *  git://github.com/d2-ci/ui.git   => ui
 *  git://github.com/d2-ci/ui       => ui
 *
 */
function ex_clone_path (url) {
    return url.split('/').reverse()[0].split('.git')[0]
}

/**
 *  Get the target destination for a given repo url
 *
 *  https://apps.dhis2.org/api/v2/apps/28823170-1203-46d1-81d5-eea67abae41c/download/app-management_100.2.32.zip => app-management
 *  https://github.com/dhis2/capture-app/releases/download/v100.53.0/capture-100.53.0.zip => capture
 *
 */
function ex_zip_path (url) {
    return url.split('/').reverse()[0].split('.zip')[0]
}

module.exports = {
    scrub,
    sanitize_app_name,
    split_repo_path,
    ex_clone_path,
    ex_zip_path
}
