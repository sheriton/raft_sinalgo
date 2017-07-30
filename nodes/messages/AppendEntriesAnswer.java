package projects.raft.nodes.messages;

import sinalgo.nodes.messages.Message;

public class AppendEntriesAnswer extends RaftMessage {
	
	public boolean success;
	public int matchIndex;
	
	@Override
	public Message clone() {
		
		AppendEntriesAnswer msg = new AppendEntriesAnswer();
		msg.term = this.term;
		msg.sender = this.sender;
		msg.target = this.target;
		msg.ttl = this.ttl;
		msg.seqId = this.seqId;
		
		msg.success = this.success;
		msg.matchIndex = this.matchIndex;
		
		return msg;
	}

}
