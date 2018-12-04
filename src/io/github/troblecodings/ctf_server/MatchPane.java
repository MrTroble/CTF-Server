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

import java.util.*;

import org.json.*;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

/**
 * @author MrTroble
 *
 */
public class MatchPane extends GridPane implements Runnable {

	private Label team_a = new Label("Team Red");
	private Label team_b = new Label("Team Blue");
	private Label time = new Label("Time: 0");
	private long last = 0;
	
	/**
	 * 
	 */
	public MatchPane() {
		init();
	}
	
	private void init() {
		for (int y = 1; y < 5; y++) {
			this.add(new Label("Player " + y), 0, y);
		}
		this.add(team_a, 1, 0);
		this.add(team_b, 2, 0);
		Button btn = new Button("Start");
		btn.setOnAction(evn -> {
			this.start();
		});
		this.add(btn, 1, 5);
		this.add(time, 2, 5);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		last = new Date().getTime();
		long ne = 0;
		while((ne = new Date().getTime()) < last + 360000) {
			long delta = (last + 360000) - ne;
			Platform.runLater(() -> {
				this.time.setText("Time: " + (double)((double)delta / (double)1000));
			});
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void fillWithJson(JSONObject json) {
		this.getChildren().clear();
		this.init();
		JSONObject jred = (JSONObject) json.getJSONObject("1");
		JSONObject jblue = (JSONObject) json.getJSONObject("2");
		team_a.setText(jred.getString("name"));
		team_b.setText(jblue.getString("name"));
		JSONArray pred = jred.getJSONArray("players");
		JSONArray pblue = jblue.getJSONArray("players");
		int i = 1;
		for(Object str : pred.toList()) {
			this.add(new Label(str.toString()), 1, i);
			i++;
		}
		i = 1;
		for(Object str : pblue.toList()) {
			this.add(new Label(str.toString()), 2, i);
			i++;
		}
	}
	
	public void start() {
		Thread thr = new Thread(this);
		thr.start();
	}
}
