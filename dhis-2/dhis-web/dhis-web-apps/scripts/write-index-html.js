const fs = require('fs')
const { execSync } = require('child_process')

function list_item (name) {
    return `
        <li>
            <a href="../${name}">
                ${name}
            </a>
        </li>`
}

function buildInfo () {
    let created = 'n/a'
    let sha = 'n/a'
    try {
        created = Date()
        sha = execSync('git rev-parse HEAD', { encoding: 'utf8' })
    } catch (e) {
        console.error(e)
    }
    return `
        <p>
            ${created}<br>
            ${sha}
        </p>`
}

module.exports = function generate_index (apps, appsPath, templatePath, indexPath) {
    let list = []
    for (const app of apps) {
        list.push(list_item(app))
    }

    try {
        const html = fs.readFileSync(indexPath, 'utf8')

        const targetHtml = html
            .replace('<!-- INJECT HTML HERE -->', list.join('\n'))
            .replace('<!-- INJECT BUILD INFO HERE -->', buildInfo())

        try {
            fs.writeFileSync(targetPath, targetHtml, { encoding: 'utf8' })
        } catch (err) {
            console.error('Failed to write', err)
            throw err
        }
    } catch (err) {
        console.error('Failed to write', err)
        throw err
    }
}
