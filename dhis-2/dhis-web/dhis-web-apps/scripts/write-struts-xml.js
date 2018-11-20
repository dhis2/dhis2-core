const { promisify } = require('util')
const path = require('path')

const read_file = promisify(require('fs').readFile)
const write_file = promisify(require('fs').writeFile)
const mkdir = promisify(require('fs').mkdir)

/**
 * The blacklist contains package names (from package.json) which should
 * not have struts definitions generated.
 */
const blacklist = [
    'core-resource-app',
    'user-profile-app'
]

function list_item (name) {

    return `
    <package 
        name="${name}" 
        extends="dhis-web-commons"
        namespace="/${name}">
            <action name="index"
                class="org.hisp.dhis.commons.action.NoAction">
                <result
                    name="success"
                    type="redirect">
                        index.html
                </result>
        </action>
    </package>`
}

module.exports = async function generate_struts (apps, templatePath, strutsPath) {
    let list = []
    for (const app of apps) {
        if (blacklist.includes(app.pkg_name)) {
            continue
        }
        list.push(list_item(app.web_name))
    }

    try {
        const xml = await read_file(templatePath, 'utf8')

        const strutsXml = xml.replace('<!-- INJECT BUNDLED APPS HERE -->', list.join('\n'))

        const rel_path = path.relative(process.cwd(), path.dirname(strutsPath))

        try {
            await mkdir(rel_path)
        } catch (err) {
            if (err.code === 'EEXIST') {
                console.log(`[struts] ${rel_path} exists already`)
            } else {
                console.error(`[struts] could not create ${rel_path}`, err)
                process.exit(1)
            }
        }

        await write_file(strutsPath, strutsXml, { encoding: 'utf8' })
        console.log(`[struts] struts.xml written`)
    } catch (err) {
        console.error('[struts] generate struts.xml failed', err)
        process.exit(1)
    }
}
