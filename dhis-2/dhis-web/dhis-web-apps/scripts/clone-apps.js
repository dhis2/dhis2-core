const { spawn } = require('child_process')
const fs = require('fs')
const path = require('path')

const {
    sanitize_app_name,
    split_repo_path,
    ex_clone_path
} = require('./lib/sanitize')

const retries = {
    rename: {},
    checkout: {}
}

const completed = []

function retry (phase, app) {
    if (!retries[phase][app]) {
        retries[phase][app] = 0
    }
    console.log(`[${phase}] [${app}] failed .. retry: ${retries[phase][app]}`)
    return ++retries[phase][app]
}


function rename (app_path, target, complete_callback) {
    const app_name = require(path.join(app_path, 'package.json')).name
    const sane_name = sanitize_app_name(app_name)
    const app_target = path.join(target, sane_name)

    console.log(`[rename] '${app_path}' => '${app_target}'`)
    
    fs.rename(app_path, app_target, function (err) {
        if (err) {
            const attempt = retry('rename', app_name)

            if (attempt > 10) {
                throw err
            }

            return rename(app_path, target, complete_callback)
        }
        console.log(`[rename] [${app_name}] OK!`)
        return complete_callback(sane_name)
    })
}

function fetch (name, path, callback) {
    const git = spawn('git', [
        'fetch', '--deepen', '10'
    ], {
        cwd: path
    })

    git.stdout.on('data', data => console.log(`[fetch] [${name}] stdout: ${data}`))
    git.stderr.on('data', data => console.log(`[fetch] [${name}] stderr: ${data}`))
    git.on('close', code => {
        console.log(`[fetch] [${name}] exit code: ${code}`)
        if (code > 0) {
            throw new Error('fetch deepen failed')
        }

        return callback()
    })
}


function checkout (name, path, hash, rename_callback) {
    const treeish = hash ? hash : 'master'
    const git = spawn('git', [
        'checkout', treeish
    ], {
        cwd: path
    })

    git.stdout.on('data', data => console.log(`[checkout] [${name}] stdout: ${data}`))
    git.stderr.on('data', data => console.log(`[checkout] [${name}] stderr: ${data}`))

    git.on('close', code => {
        console.log(`[checkout] [${name}] git checkout for '${treeish}' exit code: ${code}`)

        if (code > 0) {
            const attempt = retry('checkout', name)

            if (attempt > 100) {
                throw new Error(`[checkout] could not find '${treeish}' in the last 1000 commits .. are you sure it's there?`)
            }

            return fetch(name, path, () => {
                return checkout(name, path, hash, rename_callback)
            })
        }

        return rename_callback()
    })
}

function clone (name, url, target, hash, rename_callback) {
    const clone_path = path.join(target, name)

    const git = spawn('git', [
        'clone', '--depth', '1', '--no-single-branch', url, clone_path
    ])

    git.stdout.on('data', data => console.log(`[clone] [${name}] stdout: ${data}`))
    git.stderr.on('data', data => console.log(`[clone] [${name}] stderr: ${data}`))
    git.on('close', code => {
        console.log(`[clone] [${name}] git clone for '${url}' exit code: ${code}`)

        if (code > 0) {
            throw new Error('Something went very wrong with the clone.')
        }

        return checkout(name, clone_path, hash, rename_callback)
    })
}


module.exports = function clone_apps (repos, target, complete_callback) {
    for (const repo of repos) {
        const { url, hash } = split_repo_path(repo)
        const repo_name = ex_clone_path(url)
        const clone_path = path.join(target, repo_name)

        clone(repo_name, url, target, hash, () => {
            rename(clone_path, target, (name) => {
                completed.push(name)

                if (completed.length === repos.length) {
                    return complete_callback(completed)
                }
            })
        })
    }
}
