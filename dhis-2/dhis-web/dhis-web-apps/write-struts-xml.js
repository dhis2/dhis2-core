const fs = require('fs-extra')
const path = require('path')
const xml2js = require('xml2js')

const pkg =  require('./package.json')
const deps = pkg.dependencies

const strutsXMLPath = path.join('src', 'main', 'resources', 'struts.xml')
const targetXML = path.join('target', 'classes', 'struts.xml')

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
            console.error('Error parsing XML')
            process.exit(1)
        }

        for (let name in deps) {
            let truncName = name
                .replace('-app', '')      // strip the trailing `-app` from name
                .replace('-test', '') // just for testing purposes for the PoC

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
                                { _: '#', '$': { name: 'success', type: 'redirect' } }
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
            console.error('Failed to write', err)
            process.exit(1)
        }
    })
} catch (err) {
    console.error('Failed to read', strutsXMLPath)
    process.exit(1)
}
