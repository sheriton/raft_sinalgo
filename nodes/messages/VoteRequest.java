package projects.raft.nodes.messages;

import projects.raft.nodes.nodeImplementations.RaftNode;
import sinalgo.nodes.messages.Message;

public class VoteRequest extends RaftMessage {
	
	public RaftNode leader;
	public int lastLogIndex;
	public int lastLogTerm;

	@Override
	public Message clone() {
		VoteRequest msg = new VoteRequest();
		msg.term = this.term;
		msg.sender = this.sender;
		msg.target = this.target;
		msg.ttl = this.ttl;
		msg.seqId = this.seqId;
		
		msg.leader = this.leader;
		msg.lastLogIndex = this.lastLogIndex;
		msg.lastLogTerm = this.lastLogTerm;
		
		return msg;
	}

}
