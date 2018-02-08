
#!/usr/bin/env groovy

/**
 * Identify healthy nodes and promote the master
 */
def call(String credential) {
  echo('Identify healthy nodes and promote the master')
  if (credential?.trim()) {
    def healthyNodes = []
    def unhealthyNodes = []
    def checkSSHConnection = ''
    def sshConnectionStatus = ''
    env.RDMI_UNHEALTHY_SSH_NODES = ''
    // Check SSH connection to promote master node
    def nodes = env.RDMI_NODES.tokenize(',')
    for (int i = 0; i < nodes.size(); ++i) {
      def currentNode = nodes[i].trim()
      checkSSHConnection =  """set +e; ssh -q -o StrictHostKeyChecking=no -o ConnectTimeout=3 ${RDMI_USER}@$currentNode; echo \$?; set -e;""".trim()
      sshagent (credentials: [credential]) {
        sshConnectionStatus = sh (script: checkSSHConnection, returnStdout: true).trim()
      }
      if (sshConnectionStatus == "0") { // 0 = SSH CONNECTION IS OK!
        healthyNodes << currentNode
        // First healty node becomes the master
        if (!env.RDMI_MASTER_NODE?.trim()) {
          env.RDMI_MASTER_NODE = currentNode
          env.RDMI_MAIN_SSH_CONNECTION = "ssh -o StrictHostKeyChecking=no -l ${RDMI_USER} ${RDMI_MASTER_NODE}"
        }
      }
      else {
        unhealthyNodes << currentNode
      }
    }
    if (healthyNodes.empty) {
      error('SSH: Unable to connect to any node')
    }
    else {
      env.RDMI_HEALTHY_SSH_NODES = healthyNodes.join(',')
    }
    if (!unhealthyNodes.empty) {
      env.RDMI_UNHEALTHY_SSH_NODES = unhealthyNodes.join(',')
    }
  }
  else {
    error('SSH: Credential is empty or null')
  }
}
