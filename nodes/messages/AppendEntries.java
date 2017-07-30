package projects.raft.nodes.messages;

import java.util.List;

import projects.raft.nodes.nodeImplementations.*;
import sinalgo.nodes.messages.Message;

public class AppendEntries extends RaftMessage {

	public int prevLogIndex;
	public int prevLogTerm;
	public List<LogEntry> logEntries; // no artigo, ele envia todos os commandos que faltam
	public int commitIndex;
	
	@Override
	public Message clone() {
		
		AppendEntries msg = new AppendEntries();
		msg.term = this.term;
		msg.sender = this.sender;
		msg.target = this.target;
		msg.ttl = this.ttl;
		msg.seqId = this.seqId;
		
		msg.prevLogIndex = this.prevLogIndex;
		msg.prevLogTerm = this.prevLogTerm;
		msg.logEntries = this.logEntries;
		msg.commitIndex = this.commitIndex;
		
		return msg;
	}

}
