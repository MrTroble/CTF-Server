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

import java.io.PrintWriter;
import java.net.*;
import java.util.Scanner;

/**
 * @author MrTroble
 *
 */
public class SocketInput implements Runnable {

	private Socket socket;
	private PrintWriter writer;

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
	@Override
	public void run() {
		try {
			Scanner scanner = new Scanner(socket.getInputStream());
			this.writer = new PrintWriter(socket.getOutputStream());
			while (scanner.hasNextLine()) {
				String input = scanner.nextLine();
				SSLServer.LOGGER.println(socket + " send data " + input);
				String command = input.split(" ")[0];
				String arg = input.replaceFirst(command + " ", "");
				processData(command, arg.split(":"));
			}
			SSLServer.LOGGER.println(socket + " disconnected from the server");
			SSLServer.sockets.remove(socket);
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace(SSLServer.LOGGER);
		}
	}

	private void processData(String command, String[] args) throws Exception {
		switch (command) {
		case "disable":
			SSLServer.sendToAll("lockdown " + args[0] + ":" + args[1]);
			break;
		}
	}

}
