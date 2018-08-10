#!/usr/bin/env node

const fs = require('fs-extra')
const path = require('path')

const pkg =  require('./package.json')

console.log('process args', process.argv)
const targetDir = process.argv[2]

try {
    fs.accessSync(targetDir)
    console.log('target dir:', targetDir)
} catch (err) {
    console.log('no target dir!')
    fs.ensureDirSync(targetDir)
}

console.log('here we go', pkg.dependencies)

for (let name in pkg.dependencies) {
    const targetName = 'dhis-web-' + name.replace('-test', '')
    const src = path.join('./node_modules', name, 'build')
    const dest = path.join(targetDir, targetName)

    fs.copySync(src, dest)
    console.log('copied', src, dest)
}
