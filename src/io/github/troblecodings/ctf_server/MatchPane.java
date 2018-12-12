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

import java.util.Date;

import org.json.*;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * @author MrTroble
 *
 */
public class MatchPane extends GridPane implements Runnable {

	private Label team_a = new Label("Team Red");
	private Label team_b = new Label("Team Blue");
	private Label time = new Label("Time: 0");
	private boolean isRunning = false;
	private boolean isLoaded = false;
	private Thread thr = new Thread(this);
	private long last = 0;
	private Button start;

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
		Button stop = new Button("Stop");
		start = new Button("Start");
		start.setOnAction(evn -> {
			start.setDisable(true);
			this.start();
			stop.setDisable(false);
		});
		start.setDisable(true);
		stop.setOnAction(ev -> {
			stop.setDisable(true);
			this.onMatchFinished();
		});
		stop.setDisable(true);
		this.add(stop, 0, 5);
		this.add(start, 1, 5);
		this.add(time, 2, 5);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		this.isRunning = true;
		last = new Date().getTime();
		long ne = 0;
		while ((ne = new Date().getTime()) < last + 360000) {
			long delta = (last + 360000) - ne;
			long min = (long) ((double) delta / (double) 60000);
			long sec = (long) ((double) (delta % 60000) / (double) 1000);
			Platform.runLater(() -> {
				this.time.setText("Time: " + min + "min " + sec + " sec");
			});
			try {
				// Performance?
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		onMatchFinished();
	}

	@SuppressWarnings("deprecation")
	private void onMatchFinished() {
		ServerApp.sendToAll("match_end");
		thr.stop();
		this.isRunning = false;
	}

	public void killPlayer(String[] args, boolean b) {
		ObservableList<Node> sorted = this.getChildren().filtered(nd -> {
			return nd instanceof PlayerLabel;
		});
		if (args[0].equals("red")) {
			sorted.get(Integer.valueOf(args[1]) - 1).setDisable(b);
		} else {
			sorted.get(Integer.valueOf(args[1]) + 3).setDisable(b);
		}
		boolean team_dead = true;
		for (int i = 0; i < 4; i++) {
			if (!sorted.get(i).isDisabled()) {
				team_dead = false;
				break;
			}
		}
		if (team_dead) {
			Platform.runLater(() -> {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Match won!");
				alert.setHeaderText("Red team has been eliminated!");
				((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(ServerApp.ICON);
				alert.showAndWait();
				onMatchFinished();
			});
		}
		team_dead = true;
		for (int i = 4; i < 8; i++) {
			if (!sorted.get(i).isDisabled()) {
				team_dead = false;
				break;
			}
		}
		if (team_dead) {
			Platform.runLater(() -> {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Match won!");
				alert.setHeaderText("Blue team has been eliminated!");
				((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(ServerApp.ICON);
				alert.showAndWait();
				onMatchFinished();
			});
		}
	}

	public boolean fillWithJson(JSONObject json) {
		if (this.isRunning) {
			Platform.runLater(() -> {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Can not load match!");
				alert.setHeaderText("Match is already in progress!");
				((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(ServerApp.ICON);
				alert.showAndWait();
			});
			return false;
		}
		this.getChildren().clear();
		this.init();
		JSONObject jred = (JSONObject) json.getJSONObject("1");
		JSONObject jblue = (JSONObject) json.getJSONObject("2");
		team_a.setText(jred.getString("name"));
		team_b.setText(jblue.getString("name"));
		JSONArray pred = jred.getJSONArray("players");
		JSONArray pblue = jblue.getJSONArray("players");
		int i = 1;
		for (Object str : pred.toList()) {
			this.add(new PlayerLabel(str.toString(), Color.RED), 1, i);
			i++;
		}
		i = 1;
		for (Object str : pblue.toList()) {
			this.add(new PlayerLabel(str.toString(), Color.AQUA), 2, i);
			i++;
		}
		start.setDisable(false);
		return true;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public boolean isLoaded() {
		return isLoaded;
	}

	public void start() {
		thr.start();
	}
}
