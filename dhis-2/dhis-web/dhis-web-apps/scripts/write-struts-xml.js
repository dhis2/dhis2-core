const fs = require('fs-extra')
const path = require('path')
const xml2js = require('xml2js')

const log = require('@vardevs/log')({
    level: 2,
    prefix: 'WEBAPPS'
})

const { appName } = require('./lib/sanitize')

const root = process.cwd()
const pkg =  require(path.join(root, 'package.json'))
const deps = pkg.dependencies

const strutsXMLPath = path.join(root, 'src', 'main', 'resources', 'struts.xml')
const targetXML = path.join(root, 'target', 'classes', 'struts.xml')

/**
 * The blacklist contains package names (from package.json) which should
 * not have struts definitions generated.
 */
const blacklist = [
    'core-resource-app',
    'user-profile-app'
]

try {
    const xml = fs.readFileSync(strutsXMLPath, 'utf8')
    const parser = new xml2js.Parser()
    const builder = new xml2js.Builder({
        xmldec: {
            version: '1.0',
            encoding: 'UTF-8',
            standalone: false
        },
        doctype: {
            pubID: "-//Apache Software Foundation//DTD Struts Configuration 2.0//EN",
            sysID: "http://struts.apache.org/dtds/struts-2.0.dtd"
        }
    })
    parser.parseString(xml, function (err, res) {
        if (err) {
            log.error('Error parsing XML')
            process.exit(1)
        }

        for (let name in deps) {
            if (blacklist.includes(name)) {
                log.info('Skip blacklisted app', name)
                continue
            }
            let truncName = appName(name)

            res.struts.package.push(
                { '$':
                    {
                        name: truncName,
                        extends: 'dhis-web-commons',
                        namespace: '/' + truncName
                    },
                    action: [
                        {
                            '$': {
                                name: 'index',
                                class: 'org.hisp.dhis.commons.action.NoAction'
                            },
                            result: [
                                { _: 'index.html', '$': { name: 'success', type: 'redirect' } }
                            ]
                        }
                    ]
                }
            )
        }

        const moddedXML = builder.buildObject(res)
        try {
            fs.writeFileSync(targetXML, moddedXML, { encoding: 'utf8' })
        } catch (err) {
            log.error('Failed to write', err)
            process.exit(1)
        }
    })
} catch (err) {
    log.error('Failed to read', strutsXMLPath, err)
    process.exit(1)
}
