package cern.root.pipeline

/**
 * Contains available and default build configurations.
 */
class BuildConfiguration {
    /**
     * @return The available platforms/labels that can be used.
     */
    @NonCPS
    static def getAvailablePlatforms() {
        return ['centos7', 'slc6']
    }

    /**
     * @return The available compilers that can be used.
     */
    @NonCPS
    static def getAvailableCompilers() {
        return ['gcc8']
    }

    /**
     * @return Build configuration for pull requests.
     */
    static def getPullrequestConfiguration() {
        return [
            //[label: 'slc6', compiler: 'gcc49', buildType: 'Release']
            //[label: 'mac1012', compiler: 'native', buildType: 'Debug'],
            //[label: 'slc6', compiler: 'gcc49', buildType: 'Debug'],
            //[label: 'slc6', compiler: 'gcc62', buildType: 'Debug'],
            [label: 'centos7', compiler: 'gcc8', buildType: 'Release']
            //[label: 'ubuntu14', compiler: 'native', buildType: 'Debug'],
            //[label: 'ubuntu14', compiler: 'native', buildType: 'Release'],
            //[label: 'windows10', compiler: 'vc15', buildType: 'Release']
        ]
    }

    /**
     * @return Build configuration for incrementals.
     */
    static def getIncrementalConfiguration() {
        return [
            [label: 'centos7', compiler: 'gcc8', buildType: 'Debug']
            //[label: 'slc6', compiler: 'gcc62', buildType: 'Debug']
        ]
    }

    /**
     * Checks if a specified configuration is valid or not.
     * @param compiler Compiler to check.
     * @param platform Platform to check.
     * @return True if recognized, otherwise false.
     */
    @NonCPS
    static boolean recognizedPlatform(String compiler, String platform) {
        return getAvailableCompilers().contains(compiler) && getAvailablePlatforms().contains(platform)
    }
}
