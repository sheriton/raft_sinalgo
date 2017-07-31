package projects.raft.nodes.messages;

import java.util.List;

import projects.raft.nodes.nodeImplementations.*;
import sinalgo.nodes.messages.Message;

public class AppendEntries extends MsgGenerica {

	private int prevLogIndex;
	private int prevLogTerm;
	private List<LogEntry> logEntries; // no artigo, ele envia todos os commandos que faltam
	private int commitIndex;
	
	public int getPrevLogIndex() {
		return prevLogIndex;
	}

	public void setPrevLogIndex(int prevLogIndex) {
		this.prevLogIndex = prevLogIndex;
	}

	public int getPrevLogTerm() {
		return prevLogTerm;
	}

	public void setPrevLogTerm(int prevLogTerm) {
		this.prevLogTerm = prevLogTerm;
	}

	public List<LogEntry> getLogEntries() {
		return logEntries;
	}

	public void setLogEntries(List<LogEntry> logEntries) {
		this.logEntries = logEntries;
	}

	public int getCommitIndex() {
		return commitIndex;
	}

	public void setCommitIndex(int commitIndex) {
		this.commitIndex = commitIndex;
	}

	@Override
	public Message clone() {
		
		AppendEntries msg = new AppendEntries();
		msg.term = this.term;
		msg.noOrigem = this.noOrigem;
		msg.noDestino = this.noDestino;
		msg.sequencial = this.sequencial;
		
		msg.prevLogIndex = this.prevLogIndex;
		msg.prevLogTerm = this.prevLogTerm;
		msg.logEntries = this.logEntries;
		msg.commitIndex = this.commitIndex;
		
		return msg;
	}

	@Override
	public void acao(RaftNode node) {
		node.OnReceiveAppendEntries(this);
	}

}
