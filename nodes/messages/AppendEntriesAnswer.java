package projects.raft.nodes.messages;

import projects.raft.nodes.nodeImplementations.RaftNode;
import sinalgo.nodes.messages.Message;

public class AppendEntriesAnswer extends MsgGenerica {
	
	private boolean success;
	private int matchIndex;
	
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public int getMatchIndex() {
		return matchIndex;
	}

	public void setMatchIndex(int matchIndex) {
		this.matchIndex = matchIndex;
	}

	@Override
	public Message clone() {
		
		AppendEntriesAnswer msg = new AppendEntriesAnswer();
		msg.term = this.term;
		msg.noOrigem = this.noOrigem;
		msg.noDestino = this.noDestino;
		msg.sequencial = this.sequencial;
		
		msg.success = this.success;
		msg.matchIndex = this.matchIndex;
		
		return msg;
	}

	@Override
	public void acao(RaftNode node) {
		node.OnReceiveAppendEntriesAnswer(this);
	}

}
