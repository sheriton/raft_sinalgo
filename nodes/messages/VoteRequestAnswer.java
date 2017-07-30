package projects.raft.nodes.messages;

import sinalgo.nodes.messages.Message;

public class VoteRequestAnswer extends RaftMessage {

	public boolean voteGranted;
	
	@Override
	public Message clone() {
		VoteRequestAnswer msg = new VoteRequestAnswer();
		msg.term = this.term;
		msg.sender = this.sender;
		msg.target = this.target;
		msg.ttl = this.ttl;
		msg.seqId = this.seqId;
		
		msg.voteGranted = this.voteGranted;
		
		return msg;
	}

}
