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

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * @author MrTroble
 *
 */
public class PlayerLabel extends Label {

	public final boolean banned;
	public final String team;
	public final int i;
	public final int id;

	/**
	 * 
	 */
	public PlayerLabel(String team, int i, int mid, String str, Color cl) {
		super(str);
		this.setBackground(new Background(new BackgroundFill(cl, new CornerRadii(5), new Insets(-5))));
		init();
		banned = false;
		this.team = team;
		this.i = i;
		this.id = mid;
	}
	
	public PlayerLabel() {
		super("BANNED");
		this.setBackground(new Background(new BackgroundFill(Color.DARKSALMON, new CornerRadii(5), new Insets(-5))));
		init();
		banned = true;
		this.team = "";
		this.i = 0;
		this.id = 0;
	}
	
	private void init() {
		this.setFont(Font.font(25));
		this.setOnMouseClicked(me -> {
			if(me.getButton() == MouseButton.PRIMARY && !this.getText().contains(" (R)") && !this.banned) {
				this.setText(this.getText() + " (R)");
				ServerApp.sendToAll("set_name red:" + i + ":" + this.getText() + ":" + this.id);
			}
			if(me.getButton() == MouseButton.SECONDARY) {
				this.setText(this.getText().replace(" (R)", ""));
				ServerApp.sendToAll("set_name red:" + i + ":" + this.getText() + ":" + this.id);
			}
		});
	}
	
}
