/**
 * get-zip.js
 */

const axios = require('axios');
const AdmZip = require('adm-zip');
const { rename_app } = require('./git.js')
const {
    scrub,
    sanitize_app_name,
    split_repo_path,
    ex_clone_path,
    ex_zip_path
} = require('./lib/sanitize')
const path = require('path')
const fs = require('fs');


async function downloadAndExtractZip(url, extractPath) {

    // download zip with axios and include error handling
    try {
        const response = await axios({
            url,
            method: 'GET',
            responseType: 'arraybuffer'
        });
        console.log(`[get-zip] here we are getting the zip file`,url)
        const zip = new AdmZip(response.data);
        zip.extractAllTo(extractPath, true);
    } catch (err) {
        // download failed
        console.error(`[get-zip] there was a problem with the zip download operation`, err)
        process.exit(1)
    }

}

async function get_zip (url, extractPath) {

    const temp_path = path.join(extractPath ,ex_zip_path(url))
    console.log(`temp_path[get-zip] temp_path`,temp_path)

    try {
        await downloadAndExtractZip(url, temp_path)
        
        // manifest.webapp is a json file
        const mf = path.join(temp_path, 'manifest.webapp')
        const mfjson = mf + '.json'
        fs.copyFileSync(mf, mfjson)

        const manifest = require(mfjson)
        const build_date = manifest.manifest_generated_at
        const pkg_name = manifest.short_name
        const version = manifest.version

        const not_applicable = 'Bundled from zip file'
        const ref = not_applicable
        const sha = ''

        const name = scrub(pkg_name)
        const web_name = await rename_app(pkg_name, temp_path, extractPath) 

        return {
            ref,
            sha,
            build_date,
            pkg_name,
            version,
            name,
            web_name,
            url,
            path: path.join(extractPath, web_name),
        }

    } catch (err) {
        throw err
    }
}


module.exports = get_zip;
