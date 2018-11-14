/**
 * deps
 */

const path = require('path')

const clone_apps = require('./clone-apps.js')
const generate_index = require('./write-index-html.js')
//const generate_struts = require('./write-struts-html.js')





/**
 * setup
 */

const root = process.cwd()
const apps = require(path.join(root, process.env.APPS))
const artifact = process.env.ARTIFACT_ID
const build_dir = process.env.BUILD_DIR





/**
 * functions
 */

function main(opts = {}) {
    const { apps, artifact, build_dir, root } = opts

    clone_apps(
        apps,
        path.join(build_dir, artifact),
        (apps) => {
            console.log('all completed', apps)

            generate_index(
                apps,
                path.join(build_dir, artifact),
                path.join(root, 'src', 'main', 'webapp', 'dhis-web-apps', 'template.html'),
                path.join(build_dir, artifact, artifact, 'index.html')
            )

            //generate_struts(
            //    apps,
            //    path.join(build_dir, artifact),
            //    path.join(root, 'src', 'main', 'resources', 'struts.xml'),
            //    path.join(build_dir, 'classes', 'struts.xml')
            //)
        }
    )

}





/**
 * start it
 */

main({
    apps,
    artifact,
    build_dir,
    root
})
