#!groovy

properties([
    parameters([
        string(name: 'PODIO_REFSPEC', defaultValue: '', description: 'Refspec for PODIO repository'),
        string(name: 'PODIOTEST_REFSPEC', defaultValue: '', description: 'Refspec for PODIOtest repository'),
        string(name: 'PODIOTEST_BRANCH', defaultValue: 'master', description: 'Name of the PODIO branch to work with'),
        string(name: 'PODIO_BRANCH', defaultValue: 'master', description: 'Name of the podiotest branch to work with'),
        string(name: 'BUILD_NOTE', defaultValue: '', description: 'Note to add after label/compiler in job name'),
        string(name: 'BUILD_DESCRIPTION', defaultValue: '', description: 'Build description')
    ])
])


// Treat parameters as environment variables
for (ParameterValue p in params) {
    env[p.key] = p.value
}

// TODO: This should be avoided
env.GIT_URL = 'https://github.com/HEP-FCC/podio.git'

currentBuild.setDisplayName("#$BUILD_NUMBER $LABEL/$COMPILER $BUILD_NOTE")
currentBuild.setDescription("$BUILD_DESCRIPTION")

node(LABEL) {
    timestamps {
        stage('Checkout') {
            dir('podio') {
                retry(3) {
                    // TODO: Use the git step when it has implemented specifying refspecs
                    checkout([$class: 'GitSCM', branches: [[name: PODIO_BRANCH]], doGenerateSubmoduleConfigurations: false, extensions: [],
                            submoduleCfg: [], userRemoteConfigs: [[refspec: PODIO_REFSPEC, url: env.GIT_URL]]])
                }
            }

            // if (LABEL != 'windows10') {
            //     dir('roottest') {
            //         retry(3) {
            //             def rootTestUrl = 'https://github.com/root-project/roottest.git';
            //             // TODO: Use the git step when it has implemented specifying refspecs
            //             checkout([$class: 'GitSCM', branches: [[name: PODIOTEST_BRANCH]], doGenerateSubmoduleConfigurations: false, extensions: [],
            //                     submoduleCfg: [], userRemoteConfigs: [[refspec: PODIOTEST_REFSPEC, url: rootTestUrl]]])
            //         }
            //     }
            // }

            // dir('rootspi') {
            //     retry(3) {
            //         git url: 'https://github.com/root-project/rootspi.git'
            //     }
            // }
        }

        try {
            stage('Build') {
                if (LABEL == 'windows10') {
                    bat 'rootspi/jenkins/jk-all.bat'
                } else {
                    sh 'touch $WORKSPACE/controlfile'
                    sh 'cd podio'
                    sh 'source init.sh'
                    sh 'mkdir build'
                    sh 'cd build'
                    sh 'cmake -DCMAKE_INSTALL_PREFIX=../install -Dpodio_tests=ON ..'
                    sh 'make'
                    sh 'make install'
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
            sh 'rm -r $WORKSPACE/podio/build'
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
