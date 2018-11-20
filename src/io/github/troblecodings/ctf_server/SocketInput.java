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

import java.net.*;
import java.util.Scanner;

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
	@Override
	public void run() {
		try {
			Scanner scanner = new Scanner(socket.getInputStream());
			while (socket.isConnected()) {
				if (scanner.hasNextLine()) {
					String input = scanner.nextLine();
					SSLServer.LOGGER.println(socket + " send data " + input);
				}
			}
			SSLServer.LOGGER.println(socket + " disconnected from the server");
			SSLServer.sockets.remove(socket);
			scanner.close();
		} catch (Exception e) {
			e.printStackTrace(SSLServer.LOGGER);
		}
	}

}
