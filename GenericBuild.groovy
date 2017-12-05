#!groovy

properties([
    parameters([
        string(name: 'PKG_NAME', defaultValue: '', description: 'Name of the package to be built'),
        string(name: 'PKG_REFSPEC', defaultValue: '', description: 'Refspec for PACKAGE repository'),
        string(name: 'PKGTEST_REFSPEC', defaultValue: '', description: 'Refspec for PACKAGE test repository'),
        string(name: 'PKGTEST_BRANCH', defaultValue: 'master', description: 'Name of the PACKAGE branch to work with'),
        string(name: 'PKG_BRANCH', defaultValue: 'master', description: 'Name of the podiotest branch to work with'),
        string(name: 'BUILD_NOTE', defaultValue: '', description: 'Note to add after label/compiler in job name'),
        string(name: 'BUILD_DESCRIPTION', defaultValue: '', description: 'Build description')
    ])
])

def packageName=params.PKG_NAME

// Treat parameters as environment variables
for (ParameterValue p in params) {
    env[p.key] = p.value
}

// TODO: This should be avoided
env.GIT_URL = 'https://github.com/HEP-FCC/' + packageName + '.git'

currentBuild.setDisplayName("#$BUILD_NUMBER $LABEL/$COMPILER $BUILD_NOTE")
currentBuild.setDescription("$BUILD_DESCRIPTION")

node(LABEL) {
    timestamps {
        stage('Checkout') {
            dir(packageName) {
                retry(3) {
                    // TODO: Use the git step when it has implemented specifying refspecs
                    checkout([$class: 'GitSCM', branches: [[name: PKG_BRANCH]], doGenerateSubmoduleConfigurations: false, extensions: [],
                            submoduleCfg: [], userRemoteConfigs: [[refspec: PKG_REFSPEC, url: env.GIT_URL]]])
                }
            }

            // if (LABEL != 'windows10') {
            //     dir('roottest') {
            //         retry(3) {
            //             def rootTestUrl = 'https://github.com/root-project/roottest.git';
            //             // TODO: Use the git step when it has implemented specifying refspecs
            //             checkout([$class: 'GitSCM', branches: [[name: PKGTEST_BRANCH]], doGenerateSubmoduleConfigurations: false, extensions: [],
            //                     submoduleCfg: [], userRemoteConfigs: [[refspec: PKGTEST_REFSPEC, url: rootTestUrl]]])
            //         }
            //     }
            // }

            dir('fcc-spi') {
                retry(3) {
                    checkout([$class: 'GitSCM', branches: [[name: 'build-scripts']], doGenerateSubmoduleConfigurations: false, extensions: [],
                          submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/HEP-FCC/fcc-spi.git']]])
                }
            }
        }

        try {
            stage('Build') {
                if (LABEL == 'windows10') {
                    bat 'rootspi/jenkins/jk-all.bat'
                } else {
                    sh 'fcc-spi/builds/' + packageName + '-build.sh'
                }
            }

            // if (LABEL != 'windows10') {
            //     stage('Test') {
            //         sh 'rootspi/jenkins/jk-all test'
            //
            //         def testThreshold = [[$class: 'FailedThreshold',
            //                 failureNewThreshold: '0', failureThreshold: '0', unstableNewThreshold: '0',
            //                 unstableThreshold: '0'], [$class: 'SkippedThreshold', failureNewThreshold: '',
            //                 failureThreshold: '', unstableNewThreshold: '', unstableThreshold: '']]
            //
            //         step([$class: 'XUnitBuilder',
            //                 testTimeMargin: '3000', thresholdMode: 1, thresholds: testThreshold,
            //                 tools: [[$class: 'CTestType',
            //                         deleteOutputFiles: true, failIfNotNew: false, pattern: 'build/Testing/*/Test.xml',
            //                         skipNoTestFiles: false, stopProcessingIfError: true]]])
            //
            //         if (currentBuild.result == 'FAILURE') {
            //             throw new Exception("Test result caused build to fail")
            //         }
            //     }
            // }
        } catch (err) {
            println 'Build failed because:'
            println err
            currentBuild.result = 'FAILURE'
        }


        stage('Clean up') {
            sh 'rm -r $WORKSPACE/' + packageName + '/build'
        }
        //stash includes: 'rootspi/jenkins/logparser-rules/*', name: 'logparser-rules'
    }
}

// Log-parser-plugin will look for rules on master node. Unstash the rules and parse the rules. (JENKINS-38840)
// node('master') {
//     stage('Generate reports') {
//         unstash 'logparser-rules'
//         step([$class: 'LogParserPublisher',
//                 parsingRulesPath: "${pwd()}/rootspi/jenkins/logparser-rules/ROOT-incremental-LogParserRules.txt",
//                 useProjectRule: false, unstableOnWarning: false, failBuildOnError: true])
//     }
// }
