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

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;

import com.sun.net.httpserver.*;

/**
 * @author MrTroble
 *
 */
public class FileServer{

	private HttpServer server;
	private HttpContext app_context;
	private byte[] file;
	
	/**
	 * 
	 */
	public FileServer(Path fl,String ctx) {
		try {
			file = Files.readAllBytes(fl);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try {
			server = HttpServer.create(new InetSocketAddress(333), 100);
			server.setExecutor(null);
		} catch (IOException e) {
			e.printStackTrace();
		}

		app_context = server.createContext(ctx);
		app_context.setHandler(exch -> {
			exch.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + fl.getFileName());
			exch.sendResponseHeaders(200, file.length);
			OutputStream str = exch.getResponseBody();
			str.write(file);
			str.flush();
			str.close();
		});
		server.start();
	}
	
	public void stop() {
		this.server.stop(0);
	}
}
