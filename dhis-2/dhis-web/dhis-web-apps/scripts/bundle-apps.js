/**
 * deps
 */

const path = require('path')

const clone_app = require('./git.js')
//const generate_index = require('./write-index-html.js')
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

    for (const app of apps) {
        clone_app(app, path.join(build_dir, artifact))
    }
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
