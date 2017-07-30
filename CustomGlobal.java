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
package projects.raft;

import java.lang.reflect.Method;

import javax.swing.JOptionPane;

import projects.raft.nodes.nodeImplementations.RaftNode;
import projects.raft.nodes.nodeImplementations.RaftState;
import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.nodes.Node;
import sinalgo.runtime.AbstractCustomGlobal;
import sinalgo.tools.Tools;
import sinalgo.tools.logging.Logging;
import sinalgo.tools.statistics.Distribution;


/**
 * This class holds customized global state and methods for the framework. 
 * The only mandatory method to overwrite is 
 * <code>hasTerminated</code>
 * <br>
 * Optional methods to override are
 * <ul>
 * <li><code>customPaint</code></li>
 * <li><code>handleEmptyEventQueue</code></li>
 * <li><code>onExit</code></li>
 * <li><code>preRun</code></li>
 * <li><code>preRound</code></li>
 * <li><code>postRound</code></li>
 * <li><code>checkProjectRequirements</code></li>
 * </ul>
 * @see sinalgo.runtime.AbstractCustomGlobal for more details.
 * <br>
 * In addition, this class also provides the possibility to extend the framework with
 * custom methods that can be called either through the menu or via a button that is
 * added to the GUI. 
 */
public class CustomGlobal extends AbstractCustomGlobal{
	
	/* (non-Javadoc)
	 * @see runtime.AbstractCustomGlobal#hasTerminated()
	 */
	public boolean hasTerminated() {
		return false;
	}
	


	
	private boolean firstTime = true;
	int round = 1; // contador de rodadas
	
	boolean initCommand = false;
	int idCommand = 0;
	int roundStartCommand = 0;
	int roundEndCommand = 0;
	
	
	boolean initEleicao = false;
	int idEleicao = 0;
	int roundStartEleicao = 0;
	int roundEndEleicao = 0;
	
	
	Logging myLog = Logging.getLogger("T2DA.txt");
	Logging logEleicao = Logging.getLogger("Resumo_eleicao.txt");
	Logging logCommand = Logging.getLogger("Resumo_command.txt");

	int T = 0;
	int R = 0;
	int N = 0;
	
	/* (non-Javadoc)
	 * @see runtime.AbstractCustomGlobal#preRun()
	 */
	public void preRun() {
//		try 
//		{
//			T = Configuration.getIntegerParameter("MyRandomWayPoint/WaitingTime/constant");
//			R = Configuration.getIntegerParameter("UDG/rMax");
//		}
//		catch (CorruptConfigurationEntryException e)
//		{
//			Tools.fatalError(e.getMessage());
//		}
//		myLog.logln("N\tR\tT\tRodada\t#Followers\t#Candidates\t#Leaders\tCommited Index\t#Nodes Commited\tLeader log size\t#Nodes same size");
//		
//		logEleicao.logln("N\tR\tT\tidEleicao\tRodada inicio\tRodada fim\t#Rodadas");
//		logCommand.logln("N\tR\tT\tidCommand\tRodada inicio\tRodada fim\t#Rodadas");
	}
	
	
	
	public void preRound()
	{
		if (firstTime)
		{
			for (Node n : Tools.getNodeList()) {
				((RaftNode)n).initialize(Tools.getNodeList());
			}
			
			firstTime = false;
		}

//		// conta quantos nós estão em cada estado
//		int countCandidates = 0;
//		int countLeaders = 0;
//		int countFollowers = 0;
//		
//		RaftNode leader = null;
//		
//		for (Node n : Tools.getNodeList()) {
//			RaftNode rn = (RaftNode)n;
//
//			countCandidates += rn.state == RaftState.CANDIDATE ? 1 : 0;
//			countLeaders += rn.state == RaftState.LEADER ? 1 : 0;
//			countFollowers += rn.state == RaftState.FOLLOWER ? 1 : 0;
//			
//			if (rn.state == RaftState.LEADER && (leader == null || leader.currentTerm < rn.currentTerm))
//			{
//				leader = rn;
//			}
//		}
//		
//		// conta o tamanho do log do leader e qual o último índice commitado e conta quantos nós estão com o log correto
//		int leaderLogSize = -1;
//		int commitedIndex = -1;
//		int nodesCommited = 0;
//		int nodesSameSize = 0;
//		if (leader != null)
//		{
//			commitedIndex = leader.commitIndex;
//			leaderLogSize = leader.logEntries.size();
//			
//			for (Node n : Tools.getNodeList()) {
//				RaftNode rn = (RaftNode)n;
//
//				if (rn.commitIndex == commitedIndex)
//				{
//					nodesCommited++;
//				}
//				
//				if (rn.logEntries.size() == leaderLogSize)
//				{
//					nodesSameSize++;
//				}
//			}
//		}
//		
//		int numberOfNodes = Tools.getNodeList().size();
//		
//		if (!initEleicao && countCandidates > 0 && countLeaders == 0) // se não estava em processo de eleição e apareceram candidatos sem existir um lider, então inicia eleição
//		{
//			initEleicao = true;
//			idEleicao++;
//			roundStartEleicao = round;	
//		}
//		else if (initEleicao && countLeaders > 0) // terminou a eleição
//		{
//			roundEndEleicao = round;
//			initEleicao = false;
//			
//			logEleicao.logln(Integer.toString(numberOfNodes) + "\t"  
//					+ Integer.toString(R) + "\t"
//					+ Integer.toString(T) + "\t"
//					+ Integer.toString(idEleicao) + "\t"
//					+ Integer.toString(roundStartEleicao) + "\t"
//					+ Integer.toString(roundEndEleicao) + "\t"
//					+ Integer.toString(roundEndEleicao - roundStartEleicao)); 
//		}
//		
//		// para o log de command mesmo sem terminar
//		if (countLeaders == 0 && initCommand)
//		{
//			idCommand--;
//			initCommand = false;
//		}
//				
//		// pega um nó aleatório, se for lider, então gera um novo evento
//		Node node = Tools.getRandomNode();		
//		if (!initCommand && ((RaftNode)node).state == RaftState.LEADER)
//		{
//			((RaftNode)node).sendCommand();
//			initCommand = true;
//			idCommand++;
//			roundStartCommand = round;
//		}
//		
//		// se o tamanho das listas já foi pareado no consenso, então pode enviar um novo command
//		// só serve para ajudar nas contas
//		if (initCommand && nodesSameSize > Tools.getNodeList().size() / 2)
//		{
//			initCommand = false;
//			roundEndCommand = round;
//			
//			if (roundEndCommand != roundStartCommand)
//			{
//				logCommand.logln(Integer.toString(numberOfNodes) + "\t"  
//						+ Integer.toString(R) + "\t"
//						+ Integer.toString(T) + "\t"
//						+ Integer.toString(idCommand) + "\t"
//						+ Integer.toString(roundStartCommand) + "\t"
//						+ Integer.toString(roundEndCommand) + "\t" 
//						+ Integer.toString(roundEndCommand - roundStartCommand));
//			}
//		}
//		
//		myLog.logln(
//					Integer.toString(numberOfNodes) + "\t" +  
//					Integer.toString(R) + "\t" +
//					Integer.toString(T) + "\t" +
//					Integer.toString(round) + "\t" + 
//					Integer.toString(countFollowers) + "\t" +
//					Integer.toString(countCandidates) + "\t" +
//					Integer.toString(countLeaders) + "\t" +
//					Integer.toString(commitedIndex) + "\t" +
//					Integer.toString(nodesCommited) + "\t" +					
//					Integer.toString(leaderLogSize) + "\t" +
//					Integer.toString(nodesSameSize)
//				);
//		
//		round++;
	}
	
	/* (non-Javadoc)
	 * @see sinalgo.runtime.AbstractCustomGlobal#postRound()
	 */
	public void postRound() {
		
	}
	
	
	
	/* (non-Javadoc)
	 * @see sinalgo.runtime.AbstractCustomGlobal#includeGlobalMethodInMenu(java.lang.reflect.Method, java.lang.String)
	 */
	public String includeGlobalMethodInMenu(Method m, String defaultText) {
		if(m.getName().equals("reset")) {
			int size = Tools.getNodeList().size();
			if(size == 0) {
				return null; 
			} else {
				return "Reset all " + Tools.getNodeList().size() + " nodes"; // a context sensitive menu entry
			}
		}
		return defaultText; 
	}
}
