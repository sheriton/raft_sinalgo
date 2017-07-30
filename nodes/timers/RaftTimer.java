package projects.raft.nodes.timers;

import projects.raft.nodes.nodeImplementations.RaftNode;
import sinalgo.nodes.timers.Timer;

public abstract class RaftTimer extends Timer {

	private Boolean isActive = true;
	
	public final void deactivate() {
		this.isActive = false;
	}
	
	@Override
	public void fire() {
		if (!this.isActive)
			return;
	
		this.raftFire((RaftNode)this.node);
	}

	public abstract void raftFire(RaftNode node);
}
