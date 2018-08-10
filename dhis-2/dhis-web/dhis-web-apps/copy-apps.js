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
    process.exit(1)
}

console.log('here we go', pkg.dependencies)

for (let name in pkg.dependencies) {
    console.log(name)
    let src = path.join('./node_modules', name)
    let dest = path.join(targetDir, name)
    fs.copySync(src, dest)
    console.log('copied', src, dest)
}
