package projects.raft.nodes.messages;

import projects.raft.nodes.nodeImplementations.RaftNode;
import sinalgo.nodes.messages.Message;

public abstract class RaftMessage extends Message {

	public RaftNode sender;
	public RaftNode target; 	// null for broadcast
	public int term;
	public int seqId;
	public int ttl;
}
