def call(Map params = [url:"", sha:"", creds:"" ]) {

    if (url == null || url == "" || !url.contains("git://") ) {
        log.warn "[WARNING] Missing or not valid Git Url: ${url}"
    } else if (sha == null || sha == "") {
        log.warn "[WARNING] Missing SHA: ${sha}"
    } else if (creds == null || creds == "") {
        log.warn "[WARNING] Missing Credentials: ${creds}"
    }

    log.info url

    return [
        steps {
            checkout([$class                     : 'GitSCM',
                branches                         : [[name: params.sha]],
                doGenerateSubmoduleConfigurations: false,
                extensions                       : [],
                gitTool                          : 'Default',
                submoduleCfg                     : [],
                userRemoteConfigs                : [[credentialsId: params.creds, url: params.url]]
            ])
        }
    ]
}