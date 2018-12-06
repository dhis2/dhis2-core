/**
 * deps
 */

const { promisify } = require('util')
const path = require('path')
const access = promisify(require('fs').access)
const mkdir = promisify(require('fs').mkdir)

const { clone_app, show_sha } = require('./git.js')
const generate_index = require('./write-index-html.js')
const generate_struts = require('./write-struts-xml.js')
const generate_bundle = require('./write-bundle-json.js')





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

async function main(opts = {}) {
    const { apps, artifact, build_dir, root } = opts

    // paths for resources
    const target_path = path.join(build_dir, artifact)
    const bundle_path = path.join(target_path, artifact, 'apps-bundle.json')
    const html_template_path = path.join(root, 'src', 'main', 'webapp', 'dhis-web-apps', 'template.html')
    const html_index_path = path.join(build_dir, artifact, artifact, 'index.html')
    const xml_template_path = path.join(root, 'src', 'main', 'resources', 'struts.xml')
    const xml_struts_path = path.join(build_dir, 'classes', 'struts.xml')

    try {
        await access(bundle_path)
        console.log(`${path.basename(bundle_path)} exists; re-use cached apps for bundle`)
        process.exit(0)
    } catch (err) {
        console.log(`${path.basename(bundle_path)} doesn't exist; run full bundle operation`)
    }

    try {
        await mkdir(target_path)
    } catch (err) {
        if (err.code === 'EEXIST') {
            console.log(`[bundle] ${target_path} exists already`)
        } else {
            console.error(`[bundle] could not create ${target_path}`, err)
            process.exit(1)
        }
    }

    const new_apps = []
    for (const app of apps) {
        const promise = clone_app(app, path.join(build_dir, artifact))
        new_apps.push(promise)
    }

    const final = await Promise.all(new_apps)
    console.dir(final)

    const core_sha = await show_sha(root)

    await generate_index(final, core_sha, html_template_path, html_index_path)
    await generate_struts(final, xml_template_path, xml_struts_path)
    await generate_bundle(final, bundle_path)
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
