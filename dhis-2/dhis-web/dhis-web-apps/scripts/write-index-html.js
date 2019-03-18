const { promisify } = require('util')
const path = require('path')

const read_file = promisify(require('fs').readFile)
const write_file = promisify(require('fs').writeFile)
const mkdir = promisify(require('fs').mkdir)

function list_item (name) {
    return `
        <li>
            <a href="../${name}">
                ${name}
            </a>
        </li>`
}

function buildInfo (sha = 'n/a') {
    const created = Date()
    return `
        <p>
            ${created}<br>
            ${sha}
        </p>`
}

module.exports = async function generate_index (apps, core_sha, templatePath, indexPath) {
    let list = []
    for (const app of apps) {
        list.push(list_item(app.web_name))
    }

    try {
        const html = await read_file(templatePath, 'utf8')

        const indexHtml = html
            .replace('<!-- INJECT HTML HERE -->', list.join('\n'))
            .replace('<!-- INJECT BUILD INFO HERE -->', buildInfo(core_sha))

        const rel_path = path.relative(process.cwd(), path.dirname(indexPath))
        try {
            await mkdir(rel_path)
        } catch (err) {
            if (err.code === 'EEXIST') {
                console.log(`[index] ${rel_path} exists already`)
            } else {
                console.error(`[index] could not create ${rel_path}`, err)
                process.exit(1)
            }
        }

        await write_file(indexPath, indexHtml, { encoding: 'utf8' })
        console.log(`[index] index.html written`)
    } catch (err) {
        console.error('[index] generate index.html failed', err)
        process.exit(1)
    }
}
