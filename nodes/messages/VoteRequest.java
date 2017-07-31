package projects.raft.nodes.messages;

import projects.raft.nodes.nodeImplementations.RaftNode;
import sinalgo.nodes.messages.Message;

public class VoteRequest extends MsgGenerica {
	
	private RaftNode leader;
	private int lastLogIndex;
	private int lastLogTerm;
	
	public RaftNode getLeader() {
		return leader;
	}

	public void setLeader(RaftNode leader) {
		this.leader = leader;
	}

	public int getLastLogIndex() {
		return lastLogIndex;
	}

	public void setLastLogIndex(int lastLogIndex) {
		this.lastLogIndex = lastLogIndex;
	}

	public int getLastLogTerm() {
		return lastLogTerm;
	}

	public void setLastLogTerm(int lastLogTerm) {
		this.lastLogTerm = lastLogTerm;
	}

	
	@Override
	public Message clone() {
		VoteRequest msg = new VoteRequest();
		msg.term = this.term;
		msg.noOrigem = this.noOrigem;
		msg.noDestino = this.noDestino;
		msg.sequencial = this.sequencial;
		
		msg.leader = this.leader;
		msg.lastLogIndex = this.lastLogIndex;
		msg.lastLogTerm = this.lastLogTerm;
		
		return msg;
	}

	@Override
	public void acao(RaftNode node) {
		node.OnReceiveVoteRequest(this);
	}

}
