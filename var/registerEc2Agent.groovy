import hudson.slaves.DumbSlave
import hudson.plugins.sshslaves.SSHLauncher
import hudson.slaves.RetentionStrategy
import hudson.model.Node
import jenkins.model.Jenkins

def call(String nodeName, String privateIP, String credentialsId) {
    def launcher = new SSHLauncher(privateIP, 22, credentialsId)
    def node = new DumbSlave(
        nodeName, "/home/ubuntu/jenkins", launcher
    )
    node.setLabelString(nodeName)
    node.setNumExecutors(1)
    node.setRetentionStrategy(new RetentionStrategy.Always())

    Jenkins.instance.addNode(node)
}
