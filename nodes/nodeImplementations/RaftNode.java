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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import projects.garciaMolinaInvitation.nodes.nodeImplementations.State;
import projects.raft.nodes.messages.AppendEntries;
import projects.raft.nodes.messages.AppendEntriesAnswer;
import projects.raft.nodes.messages.RaftMessage;
import projects.raft.nodes.messages.VoteRequest;
import projects.raft.nodes.messages.VoteRequestAnswer;
import projects.raft.nodes.timers.ElectionTimer;
import projects.raft.nodes.timers.RPCTimer;
import projects.sample5.nodes.messages.PayloadMsg;
import projects.sample5.nodes.nodeImplementations.FNode;
import projects.sample5.nodes.timers.PayloadMessageTimer;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.helper.NodeSelectionHandler;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.io.eps.EPSOutputPrintStream;
import sinalgo.nodes.Node;
import sinalgo.nodes.Node.NodePopupMethod;
import sinalgo.nodes.edges.Edge;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import sinalgo.runtime.Runtime;
import sinalgo.runtime.nodeCollection.NodeCollectionInfoInterface;
import sinalgo.runtime.nodeCollection.NodeCollectionInterface;
import sinalgo.tools.Tools;
import sinalgo.tools.statistics.Distribution;


/**
 * The class to simulate the sample2-project.
 */
public class RaftNode extends Node implements Comparable<RaftNode> {

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
	//private int[] nextIndex = null; // deve ser inicializado com o número total de nós
	//private int[] matchIndex = null;
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
	
	/**
	 * Inicializa as listas de indices
	 * @param numberOfNodes
	 */
	public void initialize(NodeCollectionInterface listAllNodes) {
		this.numberOfNodes = listAllNodes.size();
		this.listAllNodes = listAllNodes;
		
		// dVote é o tempo que o leader vai aguardar até enviar um heartbeat
		//dVote = (int)Math.pow(numberOfNodes, 2);
		dVote = numberOfNodes * 2;
		
		// dElection é o tempo que um follower aguarda até receber um heartbeat
		dElection = 2 * dVote;
		
		// initialize index vectors
		//nextIndex = new int[numberOfNodes];

		int i = 0;
		// inicializa a tabela de rpcTimers, existe um rpcTimer para cada servidor na rede
		for (Node node : listAllNodes)
		{
			//nextIndex[i] = 0;	// indica que o próximo índice é o primeiro
			
			matchIndexTable.put(node, -1); // indica que não existe match no início
			tableOfMessages.put(node, -1);
			tableOfVotes.put(node, false);

			rpcTimers.put(node, new RPCTimer((RaftNode)node));
		}
		
		// inicia o timer para esperar por um heartbeat ou um comando
		setElectionTimeout(random(1F, 2F) * numberOfNodes);
	}
	
	/**
	 * Simulates a request command from client.
	 * Should be called randomically from CustomGlobal
	 * @param command
	 */
	public void clientRequest(String command) {
		LogEntry logEntry = new LogEntry(currentTerm, command);		
		logEntries.add(logEntry);
		
		//sendAppendEntries();
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
	
	private void storeEntries(List<LogEntry> entries, int commitedIndex)
	{
		if (entries.size() == 0)
			return;
		
		for (int i = 0; i < entries.size(); i++)
		{
			if (i < this.logEntries.size() && !this.logEntries.get(i).equals(entries.get(i)))
			{
				this.logEntries.set(i, entries.get(i));
			}
			else if (i >= this.logEntries.size())
			{
				this.logEntries.add(entries.get(i));
			}
		}
		
		this.commitIndex = Math.min(commitedIndex, entries.size() - 1);
	}
	
	private void sendAppendEntries()
	{
		AppendEntries appendEntries = new AppendEntries();
		appendEntries.seqId = this.seqId++;
		appendEntries.term = this.currentTerm;
		appendEntries.sender = this;
		appendEntries.target = null;
		appendEntries.ttl = dVote;
		
		appendEntries.logEntries = new ArrayList<LogEntry>();
		for (LogEntry entry : this.logEntries)
		{
			appendEntries.logEntries.add(entry);
		}
		
		appendEntries.prevLogIndex = appendEntries.logEntries.size() - 1;
		appendEntries.prevLogTerm = appendEntries.logEntries.size() == 0 ? -1 : appendEntries.logEntries.get(appendEntries.logEntries.size() - 1).term;
		appendEntries.commitIndex = this.commitIndex;
		
		broadcast(appendEntries);
	}
	
	private void sendHeartbeat()
	{
		sendAppendEntries();
	}
	
	private VoteRequest newVoteRequest(RaftNode node)
	{
		// tem q enviar o RequestVote para todos
		VoteRequest voteRequest = new VoteRequest();
		voteRequest.term = this.currentTerm;
		voteRequest.leader = this;
		voteRequest.sender = this;
		voteRequest.target = node;
		voteRequest.seqId = this.seqId++;
		voteRequest.lastLogIndex = this.logEntries.size() - 1;
		voteRequest.lastLogTerm = this.logEntries.size() == 0 ? -1 : this.logEntries.get(this.logEntries.size() - 1).term;
		voteRequest.ttl = dVote; // se não chegou até a rodada em que o timer de espera da resposta expira, não precisa repassar, pq o remetente já vai enviar outra msg.
		
		return voteRequest;
	}

	@Override
	public void handleMessages(Inbox inbox) {
		while (inbox.hasNext())
		{
			RaftMessage msg = (RaftMessage)inbox.next();
		
			if (msg.sender == this) // se ele mesmo enviou a msg, não repassa
				continue;
			
			// controle de propagação de mensagens, além disso, mais abaixo ele volta a verificar se a msg é pra ele, se sim não propaga a msg.
			Integer seqId = tableOfMessages.get(msg.sender);
			if (seqId == null || msg.seqId > seqId)
				tableOfMessages.put(msg.sender, msg.seqId); // novo vizinho enviando mensagens, inclui na tabela
			else
				continue; 	// ignora a msg e continua o loop
				
			if (msg instanceof VoteRequest)
			{
				OnReceiveVoteRequest((VoteRequest)msg);	
			}
			else if (msg instanceof VoteRequestAnswer)
			{
				OnReceiveVoteAnswer((VoteRequestAnswer)msg);
			}
			else if (msg instanceof AppendEntries)
			{
				OnReceiveAppendEntries((AppendEntries)msg);	
			}
			else if (msg instanceof AppendEntriesAnswer)
			{
				OnReceiveAppendEntriesAnswer((AppendEntriesAnswer)msg);
			}
		}
	}
	
	private void OnReceiveVoteRequest(VoteRequest voteRequest)
	{
		if(voteRequest.term > this.currentTerm)
			stepdown(voteRequest.term);
		
		if(voteRequest.term == this.currentTerm && votedFor == null)
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
			if ((voteRequest.lastLogTerm > lastLogTerm) || 
				(voteRequest.lastLogTerm == lastLogTerm && voteRequest.lastLogIndex >= lastLogIndex))
			{
				this.votedFor = voteRequest.leader;
				setElectionTimeout(random(1F, 2F) * dElection);
			}
		}
		
		VoteRequestAnswer voteAns = new VoteRequestAnswer();
		voteAns.term = voteRequest.term;
		voteAns.voteGranted = this.votedFor != null && voteRequest.leader.ID == this.votedFor.ID;
		voteAns.target = voteRequest.leader;
		voteAns.sender = this;
		voteAns.seqId = this.seqId++;
		voteAns.ttl = dVote;
		broadcast(voteAns);
		
		
		// repassa a msg
		VoteRequest msgClone = (VoteRequest) voteRequest.clone();
		msgClone.ttl--;
		if (msgClone.ttl > 0)
			broadcast(msgClone);
	}

	private void OnReceiveVoteAnswer(VoteRequestAnswer voteRequestAnswer)
	{
		// se a msg não é pra mim repassa
		if (voteRequestAnswer.target.ID != this.ID)
		{
			VoteRequestAnswer msgClone = (VoteRequestAnswer) voteRequestAnswer.clone();
			msgClone.ttl--;
			if (msgClone.ttl > 0)
				broadcast(msgClone);
			
			return;
		}
		
		if (voteRequestAnswer.term > this.currentTerm)
			stepdown(voteRequestAnswer.term);
		
		if (voteRequestAnswer.term == this.currentTerm && this.state == RaftState.CANDIDATE) 
		{
			if (voteRequestAnswer.voteGranted)
				this.votes++;
			
			if (votes > this.numberOfNodes / 2) // ganhou a eleição
			{
				this.state = RaftState.LEADER;

				//sendHeartbeat(); // na verdade tem que ser um sendAppendEntries();
				sendAppendEntries();
				
				setRPCTimeout();
			}
		}
	}
	
	private void OnReceiveAppendEntries(AppendEntries appendEntries)
	{
		if (appendEntries.term > this.currentTerm)
		{
			stepdown(appendEntries.term);
		}
		
		if (this.state == RaftState.CANDIDATE && appendEntries.term == this.currentTerm)
		{
			stepdown(appendEntries.term);
		}
		
		if (appendEntries.term < this.currentTerm)
		{
			AppendEntriesAnswer appendRep = new AppendEntriesAnswer();
			appendRep.term = this.currentTerm;
			appendRep.sender = this;
			appendRep.target = appendEntries.sender;
			appendRep.ttl = dVote;
			appendRep.seqId = this.seqId++;

			appendRep.success = false;
			
			broadcast(appendRep);
		}
		else 
		{
			this.votedFor = appendEntries.sender; 	// se recebeu um appendEntries com um termo mais alto, então o cara que enviou é o lider e ponto
			
			int prevIndex = logEntries.size() - 1;
			int prevTerm = logEntries.size() == 0 ? -1 : logEntries.get(prevIndex).term;
			
			AppendEntriesAnswer appendRep = new AppendEntriesAnswer();
			appendRep.term = this.currentTerm;
			appendRep.sender = this;
			appendRep.target = appendEntries.sender;
			appendRep.ttl = dVote;
			appendRep.seqId = this.seqId++;
			
			appendRep.success = prevIndex <= appendEntries.prevLogIndex && prevTerm <= appendEntries.prevLogTerm;
			appendRep.matchIndex = appendEntries.prevLogIndex;
			
			storeEntries(appendEntries.logEntries, appendEntries.commitIndex);
			
			broadcast(appendRep);
			
			// desativa o election timer
			this.electionTimer.deactivate();
			
			// recomeça a contagem
			setElectionTimeout(random(1F, 2F) * dElection);
			
			// passa adiante
			AppendEntries appendMsg = (AppendEntries)appendEntries.clone();
			appendMsg.ttl--;
			broadcast(appendMsg);
		}
	}
	
	private void OnReceiveAppendEntriesAnswer(AppendEntriesAnswer appendEntriesAnswer)
	{
		if (appendEntriesAnswer.target.ID != this.ID)
		{
			AppendEntriesAnswer appendRep = (AppendEntriesAnswer)appendEntriesAnswer.clone();
			appendRep.ttl--;
			broadcast(appendRep);
			return;
		}

			
		if (appendEntriesAnswer.term > this.currentTerm)
		{
			stepdown(appendEntriesAnswer.term);
		}
		else if (this.state == RaftState.LEADER && appendEntriesAnswer.term == this.currentTerm)
		{
			int lastMatchIndex = matchIndexTable.get(appendEntriesAnswer.sender);
			
			if (lastMatchIndex < appendEntriesAnswer.matchIndex) // house alteração no matchIndex do nó
			{
				matchIndexTable.put(appendEntriesAnswer.sender, appendEntriesAnswer.matchIndex);
				
				// se o matchIndex retornado é maior que o commitIndex do lider, então verifica se pode comitar um novo comando
				if (appendEntriesAnswer.matchIndex > this.commitIndex)
				{
					int count = 0;
					for (int matchIndex : matchIndexTable.values())
					{
						if (matchIndex == appendEntriesAnswer.matchIndex)
							count++;
					}
					
					// se o matchIndex recebido ocorre mais da metade das vezes, então pode comitar um novo comando
					if (count > numberOfNodes / 2)
						this.commitIndex = appendEntriesAnswer.matchIndex;
				}
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
			
			setElectionTimeout(random(1F, 2F) * dElection);
		
			// zera os votos
			for (Node node : listAllNodes)
			{
				tableOfVotes.put(node, false);
			}
			
			this.currentTerm++;
			this.state = RaftState.CANDIDATE;
			this.votedFor = this;
			this.votes = 1;
			
			this.tableOfVotes.put(this, true); // meu próprio voto
			
			// envia pedido de voto para todos os nós
			broadcast(newVoteRequest(null));
			
//			setRPCTimeout(); // no algoritmo ele espera menos tempo e tenta enviar os pedidos outra vez, eu vou tentar uma só vez por conta do broadcast que é muito caro
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
	
	
	
	@NodePopupMethod(menuText = "Send Command")
	public void sendCommand() {
		if (this.state == RaftState.LEADER)
			this.clientRequest("cmd");
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
