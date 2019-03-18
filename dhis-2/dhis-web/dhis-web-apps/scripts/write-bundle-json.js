const { promisify } = require('util')
const path = require('path')

const write_file = promisify(require('fs').writeFile)
const mkdir = promisify(require('fs').mkdir)
                 
module.exports = async function generate_bundle (apps, jsonPath) {
    let list = []
    for (const app of apps) {
        const lock = `${app.url}#${app.sha}`
        list.push(lock)
    }

    try {
        const rel_path = path.relative(process.cwd(), path.dirname(jsonPath))
        try {
            await mkdir(rel_path)
        } catch (err) {
            if (err.code === 'EEXIST') {
                console.log(`[bundle] ${rel_path} exists already`)
            } else {
                console.error(`[bundle] could not create ${rel_path}`, err)
                process.exit(1)
            }
        }

        const json = JSON.stringify(list, null, 4)
        await write_file(jsonPath, json, { encoding: 'utf8' })
        console.log(`[bundle] apps-bundle.json written`)
    } catch (err) {
        console.error('[bundle] generate apps-bundle.json failed', err)
        process.exit(1)
    }
}
