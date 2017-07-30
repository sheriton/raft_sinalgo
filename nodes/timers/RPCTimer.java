package projects.raft.nodes.timers;

import projects.raft.nodes.nodeImplementations.RaftNode;

public class RPCTimer extends RaftTimer {

	public RaftNode rpcNode;
	
	public RPCTimer(RaftNode node)
	{
		this.rpcNode = node;
	}
	
	@Override
	public void raftFire(RaftNode node) {
		node.OnRPCTimeout(this.rpcNode);
	}

}
