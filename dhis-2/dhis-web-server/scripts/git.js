const { promisify } = require('util')

const exec = promisify(require('child_process').exec)
const rename = promisify(require('fs').rename)
const path = require('path')

const {
    scrub,
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


async function checkout (name, path, treeish) {
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

async function clone (name, url, clone_path, treeish) {
    console.log(`[clone] [${name}] clone started, using ${treeish}`)
    let retries = 0
    try { // Naively try as branch first, this will fail if treeish is a commit-sha
        await exec(`git clone --depth 1 --branch ${treeish} ${url} ${clone_path}`)
        console.log(`[clone] [${name}] shallow clone successful`)
    } catch (err) {
        console.log(`[clone] [${name}] shallow clone failed. Starting clone with --no-single-branch. This may take some time`)
        while (true) {
            try { // Clone other branches as well, in case of commit sha
                await exec(`git clone --depth 1 --no-single-branch ${url} ${clone_path}`)
                console.log(`[clone] [${name}] clone successful`)
                return
            } catch (err) {
                if (retries >= 10) {
                    console.error(`[clone] [${name}] there was a problem with the clone operation`, err)
                    process.exit(1)
                }
                console.log(`[clone] [${name}] failed, retrying`)
                await exec(`rm -rf ${clone_path}`)
                retries++
            }
        }
    }
}

async function get_sha (repo_path) {
    let refspec
    try {
        const { stdout } = await exec(`git rev-parse --verify HEAD`, { cwd: repo_path})
        refspec = stdout.trim()
    } catch (err) {
        console.error(`[get_sha] could not fetch refspec for ${repo_path}, using fallback`, err)
        refspec = 'n/a'
    }
    return refspec
}

async function get_commit_date (repo_path, sha) {
    let date
    try {
        const { stdout } = await exec(`git show --no-patch --no-notes --pretty='%cd' ${sha}`, { cwd: repo_path})
        date = stdout.trim()
    } catch (err) {
        console.error(`[get_commit_date] could not fetch commit date for ${repo_path}, using fallback`, err)
        refspec = 'n/a'
    }
    return date
}

async function clone_app (repo, target, default_branch) {
    const { url, hash } = split_repo_path(repo)
    const treeish = hash ? hash : default_branch
    const repo_name = ex_clone_path(url)

    const clone_path = path.join(target, repo_name)

    try {
        await clone(repo_name, url, clone_path, treeish)
        const ref = await checkout(repo_name, clone_path, treeish)
        const sha = await get_sha(clone_path)
        const build_date = await get_commit_date(clone_path, sha)

        const pkg = require(path.join(clone_path, 'package.json'))
        const pkg_name = pkg.name
        const name = scrub(pkg_name)
        const web_name = await rename_app(pkg_name, clone_path, target)

        return {
            ref,
            sha,
            build_date,
            pkg_name,
            version: pkg.version,
            name,
            web_name,
            url,
            path: path.join(target, web_name),
        }
    } catch (err) {
        throw err
    }
}

module.exports = { clone_app, get_sha }
