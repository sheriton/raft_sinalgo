package projects.raft.nodes.nodeImplementations;

public class LogEntry {
	
	public static int seq = 0;
	
	public int _seq;
	public int term;
	public String command;
	
	public LogEntry(int term, String command) {
		this._seq = LogEntry.seq++;
		this.term = term;
		this.command = command;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LogEntry)
		{
			return this._seq == ((LogEntry)obj)._seq;
		}

		return false;
	}
}
