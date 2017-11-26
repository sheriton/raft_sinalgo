/*
 Copyright (c) 2007, Distributed Computing Group (DCG)
                    ETH Zurich
                    Switzerland
                    dcg.ethz.ch

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the
   distribution.

 - Neither the name 'Sinalgo' nor the names of its contributors may be
   used to endorse or promote products derived from this software
   without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package projects.raft.nodes.nodeImplementations;


import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import projects.raft.Tabela;
import projects.raft.nodes.messages.AppendEntries;
import projects.raft.nodes.messages.AppendEntriesAnswer;
import projects.raft.nodes.messages.MsgGenerica;
import projects.raft.nodes.messages.VoteRequest;
import projects.raft.nodes.messages.VoteRequestAnswer;
import projects.raft.nodes.timers.ElectionTimer;
import projects.raft.nodes.timers.RPCTimer;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.runtime.nodeCollection.NodeCollectionInterface;
import sinalgo.tools.statistics.Distribution;


/**
 * The class to simulate the sample2-project.
 */
public class RaftNode extends Node implements Comparable<RaftNode> {

	
	/** indicador de mensagens recebidas e reenviadas */
	private Tabela tabela;
	
	
	/* variables */
	private int numberOfNodes = 0;
	private int seqId = 0;
	private NodeCollectionInterface listAllNodes;
	
	/* Persistent state */
	public int currentTerm = 1;
	private RaftNode votedFor = null; 
	private int votes = 0;
	public List<LogEntry> logEntries = new ArrayList<LogEntry>();
	
	/* Non-persistent state */
	public RaftState state = RaftState.FOLLOWER;
	public int commitIndex = -1;
	public int matchIndex = -1;

	private Hashtable<Node, Integer> matchIndexTable = new Hashtable<Node, Integer>();

	/* timers */
	private int dElection;
	private ElectionTimer electionTimer;
	
	private int dVote;
	private RPCTimer globalRPCTimer = null;
	private Hashtable<Node, RPCTimer> rpcTimers = new Hashtable<Node, RPCTimer>();
	
	private Hashtable<Node, Integer> tableOfMessages = new Hashtable<Node, Integer>();
	private Hashtable<Node, Boolean> tableOfVotes = new Hashtable<Node, Boolean>();
	
	private Random dRandom = Distribution.getRandom();
	
	public RaftNode()
	{
		this.tabela = new Tabela();
	}
	
	/**
	 * Inicializa as listas de indices
	 * @param numberOfNodes
	 */
	public void initialize(NodeCollectionInterface listAllNodes) 
	{
		this.numberOfNodes = listAllNodes.size();
		this.listAllNodes = listAllNodes;
		
		// dVote é o tempo que o leader vai aguardar até enviar um heartbeat
		dVote = numberOfNodes * 2;
		
		// dElection é o tempo que um follower aguarda até receber um heartbeat
		dElection = 2 * dVote;
		
		// inicializa a tabela de rpcTimers, existe um rpcTimer para cada servidor na rede
		for (Node node : listAllNodes)
		{
			matchIndexTable.put(node, -1); // indica que não existe match no início
			tableOfMessages.put(node, -1);
			tableOfVotes.put(node, false);

			rpcTimers.put(node, new RPCTimer((RaftNode)node));
		}
		
		// inicia um election timer aleatório para esperar por um heartbeat ou um comando
		setElectionTimeout(random(1F, 2F) * numberOfNodes);
	}
	
	/**
	 * Simulates a request command from client.<br>
	 * Should be called randomically from CustomGlobal.<br>
	 * @param command
	 */
	public void clientRequest(String command) 
	{
		// se não é lider, ignora o comando
		if (this.state != RaftState.LEADER)
			return;
		
		// insere o novo item no próprio log
		LogEntry logEntry = new LogEntry(currentTerm, command);		
		logEntries.add(logEntry);
	}

	
	/**
	 * Atualiza o currentTerm para o termo atual, volta para o estado Follower
	 * e inicia o election timer
	 * @param term
	 */
	private void stepdown(int term)
	{
		this.currentTerm = term;
		this.state = RaftState.FOLLOWER;
		this.votedFor = null;
		
		this.matchIndex = this.commitIndex;
		
		setElectionTimeout(random(1F, 2F) * dElection);
	}
	
	/**
	 * Método para criar um número aleatório entre i e j, se i < j
	 * ou entre j e i se j < i
	 * @param i
	 * @param j
	 * @return
	 */
	private double random(double i, double j)
	{
		if (j < i)
		{
			double t = j;
			j = i;
			i = t;
		}
		return i + (1F - dRandom.nextDouble())*(j - i);
	}
	
	/**
	 * Armazena um novo comando no log
	 * @param entries
	 */
	private void storeEntries(List<LogEntry> entries)
	{
		if (entries.size() == 0)
			return;
		
		int nextIndex = this.matchIndex + 1;
		
		for (int i = 0; i < entries.size(); i++)
		{
			if (nextIndex >= this.logEntries.size())
				this.logEntries.add(entries.get(i));
			else 
				this.logEntries.set(nextIndex, entries.get(i));
		}
		
		this.matchIndex += entries.size();		
	}
	
	/**
	 * Envia um comando do log para um nó
	 * @param node - nó para onde o comando será enviado
	 * @param index	- índice do comando no vetor de log
	 */
	private void sendAppendEntries(Node node, int index)
	{
		if (index >= this.logEntries.size())
			return;
		
		AppendEntries appendEntries = new AppendEntries();
		appendEntries.setSequencial(this.seqId++);
		appendEntries.setTerm(this.currentTerm);
		appendEntries.setNoOrigem(this);
		appendEntries.setNoDestino((RaftNode)node);
		
		appendEntries.setLogEntries(new ArrayList<LogEntry>());
		appendEntries.getLogEntries().add(this.logEntries.get(index));
		
		appendEntries.setPrevLogIndex(index == 0 ? -1 : index - 1);
		appendEntries.setPrevLogTerm(index == 0 ? -1 : this.logEntries.get(index - 1).term);
		appendEntries.setCommitIndex(this.commitIndex);
		
		broadcast(appendEntries);
	}
	
	/**
	 * Envia um heartbeat para cada um dos nós conectados <br>
	 * cada nó recebe como heartbeat o último matchIndex conhecido pelo lider
	 */
	private void sendHeartbeat()
	{
		for (Node node : this.listAllNodes)
		{
			if (node.ID == this.ID)
				continue;
			
			AppendEntries hearbeat = new AppendEntries();
			hearbeat.setSequencial(this.seqId++);
			hearbeat.setTerm(this.currentTerm);
			hearbeat.setNoOrigem(this);
			hearbeat.setNoDestino((RaftNode)node);
			
			hearbeat.setLogEntries(new ArrayList<LogEntry>());
			
			hearbeat.setPrevLogIndex(this.matchIndexTable.get(node));
			hearbeat.setPrevLogTerm(this.logEntries.size() == 0 ? -1 : this.logEntries.get(this.logEntries.size() - 1).term);
			hearbeat.setCommitIndex(this.commitIndex);
			
			broadcast(hearbeat);
		}
	}
	
	
	private VoteRequest newVoteRequest(RaftNode node)
	{
		// tem q enviar o RequestVote para todos
		VoteRequest voteRequest = new VoteRequest();
		voteRequest.setTerm(this.currentTerm);
		voteRequest.setLeader(this);
		voteRequest.setNoOrigem(this);
		voteRequest.setNoDestino(node);
		voteRequest.setSequencial(this.seqId++);
		voteRequest.setLastLogIndex(this.logEntries.size() - 1);
		voteRequest.setLastLogTerm(this.logEntries.size() == 0 ? -1 : this.logEntries.get(this.logEntries.size() - 1).term);
	
		return voteRequest;
	}

	/***
	 * Trata as mensagens recebidas
	 */
	@Override
	public void handleMessages(Inbox inbox) {
		while (inbox.hasNext())
		{
			MsgGenerica msg = (MsgGenerica) inbox.next();
			if(!roteamento(msg)) continue;
			
			msg.acao(this);
		}
	}
	
	/**
	 * Realiza o roteamento das mensagens<br>
	 * @param msg mensagem a ser roteada
	 * @return verdadeiro se a mensagem (também) deve ser tratada localmente
	 */
	private boolean roteamento(MsgGenerica msg) {
		// verifica se deve repassar a mensagem para os outros nós
		// a mensagem será roteada se:
		// - (1) não tiver sido originada nesse nó (verificar noOrigem da msg)
		// - (2) não tiver sido recebida anteriormente por esse nó (verificar sequência e tipo da msg)
		// - (3) o destino não for unicamente esse nó (verificar noDestino da msg)
		if(msg.getNoOrigem().ID != this.ID &&	// (1)
		   msg.getSequencial() > tabela.getSequencia(msg.getNoOrigem().ID, msg.getClass().getSimpleName())) // (2)
		{	
			// mensagem nova de outro nó
			// acrescenta na lista de msg já recebidas
			tabela.setSequencia(msg.getNoOrigem().ID, msg.getClass().getSimpleName(), msg.getSequencial());
			if(msg.getNoDestino() == null) 
			{
				// mensagem em broadcast
				// repasssa a mensagem e avisa que deve ser tratada localmente
				this.broadcast(msg);
				return true;
			}
			else 
			{
				// mensagem em unicast
				// verifica se é para esse nó
				if(msg.getNoDestino().ID == this.ID) 
				{
					return true;
				}
				else 
				{
					// mensagem em unicast para outro nó
					// deve ser repassada
					// não deve ser tratada localmente
					this.broadcast(msg);
					return false;
				}
			}
		}
		else 
		{
			// mensagem velha
			// já está na tabela de mensagens
			// não deve ser repassada
			// não deve ser tratada localmente
			return false;
		}
	}
	
	
	
	public void OnReceiveVoteRequest(VoteRequest voteRequest)
	{
		if(voteRequest.getTerm() > this.currentTerm)
			stepdown(voteRequest.getTerm());
		
		if(voteRequest.getTerm() == this.currentTerm && votedFor == null)
		{
			int lastLogIndex = this.logEntries.size() - 1;
			int lastLogTerm = lastLogIndex < 0 ? -1 : this.logEntries.get(lastLogIndex).term;
			
			/* Verifica se o último log do candidato é mais recente que o último log que foi recebido
			 * Seção 5.4.1 do artigo.
			 * Se os últimos termos do log forem diferentes, o log de maior termo será considerado como mais atual.
			 * Se os últimos termos forem diferentes, então o log de maior índice será considerado como mais atual.
			 * 
			 * O Raft prevê que caso o voteRequest vier de um servidor que não possuí o log mais atual (more up-to-date), 
			 * então os outros servidores não vão lhe dar seus votos.
			 */
			if ((voteRequest.getLastLogTerm() > lastLogTerm) || 
				(voteRequest.getLastLogTerm() == lastLogTerm && voteRequest.getLastLogIndex() >= lastLogIndex))
			{
				this.votedFor = voteRequest.getLeader();
				setElectionTimeout(random(1F, 2F) * dElection);
			}
		}
		
		VoteRequestAnswer voteAns = new VoteRequestAnswer();
		voteAns.setTerm(voteRequest.getTerm());
		voteAns.setVoteGranted(this.votedFor != null && voteRequest.getLeader().ID == this.votedFor.ID);
		voteAns.setNoDestino(voteRequest.getLeader());
		voteAns.setNoOrigem(this);
		voteAns.setSequencial(this.seqId++);
		broadcast(voteAns);
	}

	public void OnReceiveVoteAnswer(VoteRequestAnswer voteRequestAnswer)
	{
		if (voteRequestAnswer.getTerm() > this.currentTerm)
			stepdown(voteRequestAnswer.getTerm());
		
		if (voteRequestAnswer.getTerm() == this.currentTerm && this.state == RaftState.CANDIDATE) 
		{
			if (voteRequestAnswer.isVoteGranted())
			{
				this.tableOfVotes.put(voteRequestAnswer.getNoOrigem(), true);
				this.votes++;
			}
			
			if (votes > this.numberOfNodes / 2) // ganhou a eleição
			{
				this.state = RaftState.LEADER;
				
				// assume que todos os nós estão no mesmo ponto do log
				for (Node node : this.listAllNodes)
				{
					this.matchIndexTable.put(node, this.logEntries.size() - 1);
				}

				// envia o heartbeat para cada nó
				sendHeartbeat();
				
				setRPCTimeout();
			}
		}
	}
	
	
	
	public void OnReceiveAppendEntries(AppendEntries appendEntries)
	{	
		if (appendEntries.getTerm() > this.currentTerm) // recebeu mensagem de um novo termo
		{
			stepdown(appendEntries.getTerm());
		}
		
		if (this.state == RaftState.CANDIDATE && appendEntries.getTerm() == this.currentTerm) // recebeu mensagem de um mesmo termo
		{
			stepdown(appendEntries.getTerm());
		}
		
		if (appendEntries.getTerm() < this.currentTerm) // se a mensagem é de um termo anterior, recusa e informa o termo atual
		{
			AppendEntriesAnswer appendRep = new AppendEntriesAnswer();
			appendRep.setTerm(this.currentTerm);
			appendRep.setMatchIndex(-1);
			appendRep.setNoOrigem(this);
			appendRep.setNoDestino(appendEntries.getNoOrigem());
			appendRep.setSequencial(this.seqId++);

			appendRep.setSuccess(false);
			
			broadcast(appendRep);
		}
		else 
		{
			if (this.votedFor == null || this.votedFor.ID != appendEntries.getNoOrigem().ID)
				this.votedFor = appendEntries.getNoOrigem(); 	// se recebeu um appendEntries com um termo mais alto, então o cara que enviou é o lider e ponto
		
			AppendEntriesAnswer appendRep = new AppendEntriesAnswer();
			appendRep.setTerm(this.currentTerm);
			appendRep.setNoOrigem(this);
			appendRep.setNoDestino(appendEntries.getNoOrigem());
			appendRep.setSequencial(this.seqId++);
			
			// se o prevLogIndex da mensagem é igual ao commited Index
			if (appendEntries.getPrevLogIndex() == this.matchIndex)
			{
				storeEntries(appendEntries.getLogEntries());
				this.commitIndex = Math.min(appendEntries.getCommitIndex(), this.matchIndex);
				appendRep.setSuccess(true);
				appendRep.setMatchIndex(this.matchIndex);
			}
			else 
			{
				appendRep.setSuccess(false);
			}
			
			broadcast(appendRep);
			
			// desativa o election timer
			this.electionTimer.deactivate();
			
			// recomeça a contagem
			setElectionTimeout(random(1F, 2F) * dElection);
		}
	}

	public void OnReceiveAppendEntriesAnswer(AppendEntriesAnswer appendEntriesAnswer)
	{
		if (appendEntriesAnswer.getTerm() > this.currentTerm)
		{
			stepdown(appendEntriesAnswer.getTerm());
		}
		else if (this.state == RaftState.LEADER && appendEntriesAnswer.getTerm() == this.currentTerm)
		{
			if (appendEntriesAnswer.isSuccess())
			{
				int lastMatchIndex = appendEntriesAnswer.getMatchIndex();
				
				// se o matchIndex recebido é maior que o commitedIndex, então verifica se o item foi comitado
				if (lastMatchIndex > commitIndex)
				{
					int count = 1;
					for (int matchIndex : matchIndexTable.values())
					{
						if (matchIndex >= lastMatchIndex)
							count++;
					}
					
					if (count > numberOfNodes / 2)
						this.commitIndex = lastMatchIndex;
				}
				
				// se retornou sucesso, atualiza o matchIndex do nó
				matchIndexTable.put(appendEntriesAnswer.getNoOrigem(), lastMatchIndex);
				
				// se o log do nó é menor que o log do lider, envia o próximo comando para o nó
				if (lastMatchIndex < this.logEntries.size() - 1)
				{
					sendAppendEntries(appendEntriesAnswer.getNoOrigem(), lastMatchIndex + 1);
				}
			}
			else 
			{
				// se o nó não aceitou o appendEntries, decrementa o matchIndex 
				int lastMatchIndex = matchIndexTable.get(appendEntriesAnswer.getNoOrigem());
				matchIndexTable.put(appendEntriesAnswer.getNoOrigem(), lastMatchIndex - 1);
			}
		}
	}

	
	
	private void setElectionTimeout(double t)
	{
		// só para garantir que 2 timers não vão acontecer de forma concorrente no mesmo nó
		if (electionTimer != null)
			electionTimer.deactivate();
					
		this.electionTimer = new ElectionTimer();			
		this.electionTimer.startRelative(t, this);
	}

	public void OnElectionTimeout() 
	{
		if (this.state != RaftState.LEADER)
		{
			if (this.globalRPCTimer != null)
				this.globalRPCTimer.deactivate();
			
			// inicia um election timer
			setElectionTimeout(random(1F, 2F) * dElection);
		
			// zera os votos
			for (Node node : listAllNodes)
			{
				tableOfVotes.put(node, false);
			}
			
			// incrementa o termo e se torna candidato
			this.currentTerm++;
			this.state = RaftState.CANDIDATE;
			this.votedFor = this;
			this.votes = 1;
			
			this.tableOfVotes.put(this, true); // próprio voto
			
			// envia pedido de voto para todos os nós
			broadcast(newVoteRequest(null));
			
			//setRPCTimeout(); // no algoritmo ele espera menos tempo e tenta enviar os pedidos outra vez, eu vou tentar uma só vez por conta do broadcast que é muito caro
		}
	}
	
	private void setRPCTimeout()
	{
		globalRPCTimer = new RPCTimer(null);
		globalRPCTimer.startRelative(dVote, this);
	}
	
	public void OnRPCTimeout(RaftNode node) 
	{
		if (this.state == RaftState.CANDIDATE)
		{
			// esse trecho não deve ser executado, pois comentei a linha que inicia o contador RPC ao iniciar a eleição (com broadcast isso fica muito caro)
			RPCTimer timer = rpcTimers.get(node);
			if (timer != null)
				timer.deactivate();
			
			timer = new RPCTimer(node);
			timer.startRelative(dVote, this);
			
			/* envia um voteRequest se não recebeu ok da maioria dpois de dVote */
			broadcast(newVoteRequest(null));
		}
		else if (this.state == RaftState.LEADER)
		{
			sendHeartbeat();
			
			setRPCTimeout();
		}
	}

	
	/* ****************************************** */
	/* Menus para interação do usuário do sinalgo */
	/* ****************************************** */
	private static int iMsg = 0;
	@NodePopupMethod(menuText = "Send Command")
	public void sendCommand() {
		if (this.state == RaftState.LEADER)
			this.clientRequest("cmd " + iMsg++);
	}

	
	/* ***************************************** */
	/* Daqui pra baixo são os métodos do sinalgo */
	/* ***************************************** */
	@Override
	public void init() {
	}

	@Override
	public void neighborhoodChange() 
	{
	}

	@Override
	public void preStep() {
		// occurs before step
	}

	@Override
	public void postStep() {
	}
	
	@Override
	public String toString() {
		return Integer.toString(this.ID) + ":" + Integer.toString(this.currentTerm);
	}
	
	public void draw(Graphics g, PositionTransformation pt, boolean highlight) {
		this.drawingSizeInPixels = this.defaultDrawingSizeInPixels;
		
		if (this.state == RaftState.FOLLOWER)
		{
			this.setColor(Color.BLACK);
		}
		else if (this.state == RaftState.CANDIDATE)
		{
			this.setColor(Color.GREEN);
		}
		else if (this.state == RaftState.LEADER)
		{
			this.setColor(Color.RED);
		}
		
		drawNodeAsDiskWithText(g, pt, highlight, Integer.toString(this.ID) + ":" + Integer.toString(this.currentTerm) + ":" + Integer.toString(this.commitIndex), 1, Color.WHITE);
	}

	public void drawToPostScript(EPSOutputPrintStream pw, PositionTransformation pt) {
		drawToPostScriptAsDisk(pw, pt, drawingSizeInPixels/2, getColor());
	}

	public int compareTo(RaftNode tmp) {
		if(this.ID < tmp.ID) {
			return -1;
		} else {
			if(this.ID == tmp.ID) {
				return 0;
			} else {
				return 1;
			}
		}
	}

	@Override
	public void checkRequirements() throws WrongConfigurationException {
		// TODO Auto-generated method stub
		
	}
	
}
