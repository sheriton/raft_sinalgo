/**
 * @author Meslin
 *
 */
package projects.raft.nodes.messages;

import projects.raft.nodes.nodeImplementations.RaftNode;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;

public abstract class MsgGenerica extends Message {
	/** origem da mensagem */
	protected Node noOrigem;
	/** destino da mensagem */
	protected Node noDestino;
	/** contador de mensagens, utilizado para que os n�s n�o repassem mensagens repetidas */
	protected int sequencial;

	/**
	 * 
	 */
	public MsgGenerica() {
	}
	
	/**
	 * Reage � mensagem
	 * @param node
	 */
	public abstract void acao (RaftNode node);

	public final Node getNoOrigem() {
		return this.noOrigem;
	}

	public final void setNoOrigem(Node noOrigem) {
		this.noOrigem = noOrigem;
	}

	public final Node getNoDestino() {
		return this.noDestino;
	}

	public final void setNoDestino(Node noDestino) {
		this.noDestino = noDestino;
	}

	public final int getSequencial() {
		return this.sequencial;
	}

	public final void setSequencial(int sequencial) {
		this.sequencial = sequencial;
	}
	
	@Override
	public String toString() {
		return " n� origem: " + this.getNoOrigem() + ", n� destino: " + this.getNoDestino() + ", seq: " + this.getSequencial(); 
	}
}

