package projects.raft.nodes.messages;

import projects.raft.nodes.nodeImplementations.RaftNode;
import sinalgo.nodes.messages.Message;

public class VoteRequestAnswer extends MsgGenerica {

	private boolean voteGranted;
	
	public boolean isVoteGranted() {
		return voteGranted;
	}

	public void setVoteGranted(boolean voteGranted) {
		this.voteGranted = voteGranted;
	}

	@Override
	public Message clone() {
		VoteRequestAnswer msg = new VoteRequestAnswer();
		msg.term = this.term;
		msg.noOrigem = this.noOrigem;
		msg.noDestino = this.noDestino;
		msg.sequencial = this.sequencial;
		
		msg.voteGranted = this.voteGranted;
		
		return msg;
	}

	@Override
	public void acao(RaftNode node) {
		node.OnReceiveVoteAnswer(this);
	}

}
