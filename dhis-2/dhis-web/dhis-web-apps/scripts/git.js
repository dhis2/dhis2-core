const { promisify } = require('util')

const exec = promisify(require('child_process').exec)
const rename = promisify(require('fs').rename)
const path = require('path')

const {
    sanitize_app_name,
    split_repo_path,
    ex_clone_path
} = require('./lib/sanitize')

async function rename_app (app_name, app_path, target) {
    const sane_name = sanitize_app_name(app_name)
    const app_target = path.join(target, sane_name)

    console.log(`[rename] [${app_name}] '${app_path}' => '${app_target}'`)

    let retries = 0
    while (retries <= 10) {
        try {
            await rename(app_path, app_target)
            return sane_name
        } catch (err) {
            if (retries >= 10) {
                console.error(`[rename] failed to rename ${app_name} to ${sane_name}`)
                process.exit(1)
            }
            retries++
        }
    }
}

async function fetch (name, path) {
    console.log(`[fetch] [${name}] fetching more commits...`)
    try {
        await exec(`git fetch --deepen 10`, { cwd: path })
    } catch (err) {
        console.error(`[fetch] problem when fetching ${name}`, err)
    }
}


async function checkout (name, path, hash) {
    const treeish = hash ? hash : 'master'

    let retries = 0
    while (retries <= 100) {
        try {
            await exec(`git checkout ${treeish}`, { cwd: path })
            console.log(`[checkout] [${name}] successfully checked out: ${treeish}`)
            return treeish
        } catch (err) {
            if (retries >= 100) {
                console.error(`[checkout] [${name}] could not find '${treeish}' in the last 1000 commits .. are you sure it's there?`)
                process.exit(1)
            }

            console.log(`[checkout] [${name}] failed to checkout: ${treeish}`)
            await fetch(name, path)
            retries++
        }
    }
}

async function clone (name, url, clone_path) {
    console.log(`[clone] [${name}] clone started`)
    try {
        await exec(`git clone --depth 1 --no-single-branch ${url} ${clone_path}`)
        console.log(`[clone] [${name}] clone successful`)
    } catch (err) {
        console.error(`[clone] [${name}] there was a problem with the clone operation`, err)
        process.exit(1)
    }
}

async function show_sha (repo_path) {
    try {
        const { stderr, stdout } = await exec(`git rev-parse --verify HEAD`, { cwd: repo_path})
        const refspec = stdout.trim()
        return refspec
    } catch (err) {
        console.error(`[show_sha] could not fetch refspec for ${repo_path}`, err)
        process.exit(1)
    }
}

async function clone_app (repo, target, complete_callback) {
    const { url, hash } = split_repo_path(repo)
    const repo_name = ex_clone_path(url)

    const clone_path = path.join(target, repo_name)

    try {
        await clone(repo_name, url, clone_path)
        const ref = await checkout(repo_name, clone_path, hash)
        const sha = await show_sha(clone_path)

        const pkg_name = require(path.join(clone_path, 'package.json')).name
        const web_name = await rename_app(pkg_name, clone_path, target)

        return {
            ref,
            sha,
            pkg_name,
            web_name,
            url,
            path: path.join(target, web_name),
        }
    } catch (err) {
        throw err
    }
}

module.exports = { clone_app, show_sha }
