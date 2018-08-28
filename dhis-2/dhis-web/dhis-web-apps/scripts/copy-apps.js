#!/usr/bin/env node

const fs = require('fs-extra')
const path = require('path')

const log = require('@vardevs/log')({
    level: 2,
    prefix: 'WEBAPPS'
})

console.log('cwd', process.cwd())
const root = process.cwd()

const pkg =  require(path.join(root, 'package.json'))
const deps = pkg.dependencies

log.info('process args', process.argv)
const targetDir = process.argv[2]

try {
    fs.accessSync(targetDir)
    log.info('target dir:', targetDir)
} catch (err) {
    log.error('no target dir!')
    fs.ensureDirSync(targetDir)
}

for (let name in deps) {
    const targetName = 'dhis-web-' + name
        .replace('-app', '')

    const src = path.join(root, './node_modules', name)
    const dest = path.join(targetDir, targetName)

    fs.copySync(src, dest)
    log.info('copied', src, dest)
}
