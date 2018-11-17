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
import java.util.Date;

/**
 * @author MrTroble
 *
 */
public class LoggerFile extends PrintStream {

	/**
	 * @param stream
	 */
	public LoggerFile(FileOutputStream stream) {
		super(stream);
	}
	
	/* (non-Javadoc)
	 * @see java.io.PrintStream#println(java.lang.String)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void println(String x) {
		Date date = new Date();
		x = "[" + date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds() + "]" + x;
		super.println(x);
	}
	
	/* (non-Javadoc)
	 * @see java.io.PrintStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] buf, int off, int len) {
		super.write(buf, off, len);
		System.out.write(buf, off, len);
		this.flush();
	}

}
