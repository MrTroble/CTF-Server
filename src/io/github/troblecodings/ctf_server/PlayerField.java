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

import javafx.event.*;
import javafx.scene.control.TextField;

/**
 * @author MrTroble
 *
 */
public class PlayerField extends TextField implements EventHandler<ActionEvent>{

	private String team;
	private int index;
	
	/**
	 * @param team
	 * @param index
	 */
	public PlayerField(boolean team, int index) {
		this.team = team? "red":"blue";
		this.index = index;
		this.setText(this.team + index);
		this.setOnAction(this);
	}
	
	/* (non-Javadoc)
	 * @see javafx.event.EventHandler#handle(javafx.event.Event)
	 */
	@Override
	public void handle(ActionEvent event) {
		ServerApp.sendToAll("set_name " + team + ":" + index + ":" + this.getText());
	}
	
	public boolean isRed() {
		return this.team.equals("red");
	}
}
