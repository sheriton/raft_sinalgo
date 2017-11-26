/**
 * @author Meslin
 *
 */
package projects.raft.nodes.messages;

import projects.raft.nodes.nodeImplementations.RaftNode;
import sinalgo.nodes.messages.Message;

public abstract class MsgGenerica extends Message {
	/** origem da mensagem */
	protected RaftNode noOrigem;
	/** destino da mensagem */
	protected RaftNode noDestino;
	/** contador de mensagens, utilizado para que os nós não repassem mensagens repetidas */
	protected int sequencial;

	/** guarda o term em que a mensagem foi criada */
	protected int term;
	
	/**
	 * 
	 */
	public MsgGenerica() {
	}
	
	/**
	 * Reage à mensagem
	 * @param node
	 */
	public abstract void acao (RaftNode node);

	public final RaftNode getNoOrigem() {
		return this.noOrigem;
	}

	public final void setNoOrigem(RaftNode noOrigem) {
		this.noOrigem = noOrigem;
	}

	public final RaftNode getNoDestino() {
		return this.noDestino;
	}

	public final void setNoDestino(RaftNode noDestino) {
		this.noDestino = noDestino;
	}

	public final int getSequencial() {
		return this.sequencial;
	}

	public final void setSequencial(int sequencial) {
		this.sequencial = sequencial;
	}

	public final int getTerm() {
		return this.term;
	}
	
	public final void setTerm(int term) {
		this.term = term;
	}
	
	@Override
	public String toString() {
		return " nó origem: " + this.getNoOrigem() + ", nó destino: " + this.getNoDestino() + ", seq: " + this.getSequencial(); 
	}
}

