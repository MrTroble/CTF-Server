/*-*****************************************************************************
 * Copyright 2018 MrTroble
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package io.github.troblecodings.ctf_server;

import java.net.Socket;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author MrTroble
 *
 */
public class SocketInput implements Runnable {

	private Socket socket;

	/**
	 * Thread runnable for coherent input detection
	 * 
	 * @param socket
	 */
	public SocketInput(Socket socket) {
		this.socket = socket;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@SuppressWarnings("resource")
	@Override
	public void run() {
		try {
			Scanner scanner = new Scanner(socket.getInputStream());
			if(scanner.hasNextLine()) {
				String str = scanner.nextLine();
				if(!str.equals(ServerApp.SERVER_PW)) {
					ServerApp.LOGGER.println(socket + " Wrong admin password! Using as reciver!");
					return;
				}
			}
			ServerApp.LOGGER.println(socket + " Admin access granted!");
			while (scanner.hasNextLine()) {
				String input = scanner.nextLine();
				ServerApp.LOGGER.println(socket + " sendet " + input);
				String command = input.split(" ")[0];
				String arg = input.replaceFirst(command + " ", "");
				processData(command, arg.split(":"));
			}
			ServerApp.LOGGER.println(socket + " disconnected from the server");
			ServerApp.sockets.remove(socket);
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace(ServerApp.LOGGER);
		}
	}

	private void processData(String command, String[] args) throws Exception {
		switch (command) {
		case "disable":
			MatchPane.MATCHES.get(Integer.valueOf(args[2])).killPlayer(args, true);;
			ServerApp.sendToAll("lock " + args[0] + ":" + args[1] + ":" + args[2]);
			Timer tm = new Timer();
			tm.schedule(new TimerTask() {
				
				@Override
				public void run() {
					try {
						MatchPane.MATCHES.get(Integer.valueOf(args[2])).killPlayer(args, false);;
						ServerApp.sendToAll("unlock " + args[0] + ":" + args[1] + ":" + args[2]);
					} catch (Exception e) {
						e.printStackTrace();
					}					
				}
			}, 10000);
			break;
		case "match_end":
			MatchPane.MATCHES.get(Integer.valueOf(args[args.length - 1])).onMatchFinished(args[0] + ":");
			break;
		}
	}

}
