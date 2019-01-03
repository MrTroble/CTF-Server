package io.github.troblecodings.ctf_server;

import javafx.event.Event;
import javafx.event.EventType;

@SuppressWarnings("serial")
public class KillEvent extends Event{

	public static final EventType<KillEvent> KILL_EVENT_TYPE = new EventType<KillEvent>("KILL_EVENT_TYPE");
	
	public final boolean kill;
	public final String[] args;

	public KillEvent(String[] args, boolean kill) {
		super(KILL_EVENT_TYPE);
		this.kill = kill;
		this.args = args;
	}

}
