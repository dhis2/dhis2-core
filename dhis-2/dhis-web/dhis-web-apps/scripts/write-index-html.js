const fs = require('fs-extra')
const path = require('path')

const log = require('@vardevs/log')({
    level: 2,
    prefix: 'WEBAPPS'
})

const { appName } = require('./lib/sanitize')

const root = process.cwd()
const pkg =  require(path.join(root, 'package.json'))
const deps = pkg.dependencies

const indexPath = path.join(root, 'src', 'main', 'webapp', 'dhis-web-apps', 'template.html')
const targetDir = path.join(root, 'target', 'dhis-web-apps', 'dhis-web-apps')
const targetPath = path.join(targetDir, 'index.html')

try {
    fs.accessSync(targetDir)
    log.info('target dir:', targetDir)
} catch (err) {
    log.error('no target dir!')
    fs.ensureDirSync(targetDir)
}

function listEl (name) {
    return `
        <li>
            <a href="../${name}">
                ${name}
            </a>
        </li>`
}

try {
    const html = fs.readFileSync(indexPath, 'utf8')

    const apps = []
    for (let name in deps) {
        apps.push(listEl(appName(name)))
    }

    const targetHtml = html.replace('<!-- INJECT HTML HERE -->', apps.join('\n'))

    try {
        fs.writeFileSync(targetPath, targetHtml, { encoding: 'utf8' })
    } catch (err) {
        console.error('Failed to write', err)
        process.exit(1)
    }
} catch (err) {
    console.error('Failed to write', err)
    process.exit(1)
}

