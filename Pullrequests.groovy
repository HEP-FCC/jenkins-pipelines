#!groovy

@Library('fcc-pipelines')
import cern.root.pipeline.*

properties([
    parameters([
        string(name: 'ghprbPullId', defaultValue: '61'),
        string(name: 'ghprbGhRepository', defaultValue: ''),
        string(name: 'ghprbCommentBody', defaultValue: '@phsft-bot build'),
        string(name: 'ghprbTargetBranch', defaultValue: 'master'),
        string(name: 'ghprbActualCommit', defaultValue: ''),
        string(name: 'sha1', defaultValue: ''),
        string(name: 'VERSION', defaultValue: 'master', description: 'Branch to be built'),
        //string(name: 'EXTERNALS', defaultValue: 'ROOT-latest', description: ''),
        //string(name: 'EMPTY_BINARY', defaultValue: 'true', description: 'Boolean to empty the binary directory (i.e. to force a full re-build)'),
        string(name: 'ExtraCMakeOptions', defaultValue: '', description: 'Additional CMake configuration options of the form "-Doption1=value1 -Doption2=value2"'),
        string(name: 'MODE', defaultValue: 'pullrequests', description: 'The build mode'),
        string(name: 'PARENT', defaultValue: '', description: 'Trigger job name')
    ])
])

timestamps {
    def packageName = params.ghprbGhRepository.tokenize('/').last().toLowerCase()
    def buildJobName = packageName + "-pullrequests-build"

    if(!params.PARENT){
        params.PARENT = packageName.toUpperCase()  + "-pullrequests-trigger"
    }

    GitHub gitHub = new GitHub(this, PARENT, ghprbGhRepository, ghprbPullId, params.ghprbActualCommit)
    BotParser parser = new BotParser(this, params.ExtraCMakeOptions)
    GenericBuild build = new GenericBuild(this, buildJobName, params.MODE)

    build.addBuildParameter('PKG_NAME', "${packageName}")
    build.addBuildParameter('PKG_REFSPEC', '+refs/pull/*:refs/remotes/origin/pr/*')
    build.addBuildParameter('PKG_BRANCH', "origin/pr/${ghprbPullId}/merge")
    build.addBuildParameter('PKGTEST_BRANCH', "${params.ghprbTargetBranch}")
    build.addBuildParameter('GIT_COMMIT', "${params.sha1}")
    build.addBuildParameter('BUILD_NOTE', "PR #$ghprbPullId")

    currentBuild.setDisplayName("#$BUILD_NUMBER PR #$ghprbPullId")

    build.cancelBuilds('.*PR #' + ghprbPullId + '$')

    build.afterBuild({buildWrapper ->
        if (buildWrapper.result.result != 'SUCCESS' && currentBuild.result != 'ABORTED') {
            gitHub.postResultComment(buildWrapper)
        }
    })

    if (parser.isParsableComment(ghprbCommentBody.trim())) {
        parser.parse()
    }

    parser.postStatusComment(gitHub)
    parser.configure(build)

    gitHub.setPendingCommitStatus('Building')

    build.build()

    stage('Publish reports') {
        if (currentBuild.result == 'SUCCESS') {
            gitHub.setSucceedCommitStatus('Build passed')
        } else if (currentBuild.result != 'ABORTED') {
            gitHub.setFailedCommitStatus('Build failed')
        }

        if (currentBuild.result != null) {
            build.sendEmails()
        }
    }
}
