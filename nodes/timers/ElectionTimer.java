package projects.raft.nodes.timers;

import projects.raft.nodes.nodeImplementations.RaftNode;

public class ElectionTimer extends RaftTimer {

	@Override
	public void raftFire(RaftNode node) {
		node.OnElectionTimeout();
	}

}
