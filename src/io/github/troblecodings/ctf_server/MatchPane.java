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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * @author MrTroble
 *
 */
public class MatchPane extends GridPane implements Runnable {

	public static HashMap<Integer, MatchPane> MATCHES = new HashMap<Integer, MatchPane>();

	private Label team_a = new Label("Team Red");
	private Label team_b = new Label("Team Blue");
	private Label time = new Label("Time: 0");
	private boolean isRunning = false, isPause = false, isLoaded = false;
	private Thread thr = new Thread(this);
	private long last = 0;
	public Button start, stop = new Button("Pause");
	private Path loaded_file;
	private int matchid;
	private JSONObject cmatch;

	/**
	 * @param id
	 */
	public MatchPane(int matchid) {
		this.matchid = matchid;
		MATCHES.put(matchid, this);
		init();
	}

	private void init() {
		for (int y = 1; y < 5; y++) {
			this.add(new Label("Player " + y), 0, y);
		}
		this.add(new Label("Reserve 1"), 0, 5);
		this.add(new Label("Reserve 2"), 0, 6);

		this.add(team_a, 1, 0);
		this.add(team_b, 2, 0);
		Button load = new Button("Load");
		load.setOnAction(evt -> {
			Dialog<String> dialog = new Dialog<String>();
			ListView<Label> plans = new ListView<Label>();
			plans.setPrefSize(500, 300);
			dialog.getDialogPane().setContent(plans);
			((Stage) dialog.getDialogPane().getScene().getWindow()).getIcons().add(ServerApp.ICON);
			dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
			dialog.setResultConverter(bt -> {
				if (bt.equals(ButtonType.APPLY))
					return plans.getSelectionModel().getSelectedItem().getText();
				return null;
			});
			ObservableList<Label> itms = plans.getItems();
			itms.clear();
			itms.add(new Label("List of matches"));

			try {
				Files.list(ServerApp.path_plan).filter(pth -> {
					return Files.isRegularFile(pth) && pth.toString().endsWith(".json");
				}).forEach(pth -> {
					Label lab = new Label(pth.getFileName().toString().replace(".json", ""));
					itms.add(lab);
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
			dialog.showAndWait().ifPresent(stri -> {
				Alert alert = new Alert(AlertType.CONFIRMATION);
				alert.setTitle("Load?");
				alert.setHeaderText("Do you want to load this match?");
				alert.setContentText("Match: " + stri);
				((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(ServerApp.ICON);
				alert.showAndWait().ifPresent(tp -> {
					if (tp == ButtonType.OK) {
						try {
							loaded_file = Paths.get(ServerApp.path_plan.toString(), stri + ".json");
							String str = new String(Files.readAllBytes(loaded_file));
							JSONObject obj = new JSONObject(str);
							if (this.fillWithJson(obj)) {
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				;
			});
		});
		start = new Button("Start");
		start.setOnAction(evn -> {
			start.setDisable(true);
			if (!this.isPause)
				this.start();
			else
				ServerApp.sendToAll("match_unpause " + matchid);
			last = new Date().getTime();
			this.isPause = false;
			stop.setDisable(false);
		});
		start.setDisable(true);
		stop.setOnAction(ev -> {
			stop.setDisable(true);
			start.setDisable(false);
			ServerApp.sendToAll("match_pause " + matchid);
			this.isPause = true;
		});
		stop.setDisable(true);
		this.add(load, 0, 0);
		this.add(stop, 0, 7);
		this.add(start, 1, 7);
		this.add(time, 2, 7);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		this.isRunning = true;
		ServerApp.sendToAll("match_start " + matchid);
		last = new Date().getTime();
		long ne = 0;
		long delta = ServerApp.MINS * 60000;
		while (delta > 0) {
			if (this.isPause) {
				try {
					// Performance?
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			ne = new Date().getTime();
			delta -= ne - last;
			last = ne;
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
		onMatchFinished("time:");
	}

	@SuppressWarnings("deprecation")
	public void onMatchFinished(String rs) {
		this.isRunning = false;
		ServerApp.sendToAll("match_end " + rs + matchid);
		thr.stop();
		try {
			ServerApp.LOGGER.println(rs.replace(":", ""));
			Path pth = Paths.get(ServerApp.path_history.toString(),
					team_a.getText() + " vs " + team_b.getText() + ".json");
			int rs1 = rs.replace(":", "").equals("red_win") ? 3 : 0;
			int rs2 = rs.replace(":", "").equals("blue_win") ? 3 : 0;
			if (Files.exists(pth)){
				JSONObject obj = new JSONObject(new String(Files.readAllBytes(pth)));
				rs1 += obj.getJSONObject("1").getInt("result");
				rs2 += obj.getJSONObject("2").getInt("result");
			} else Files.createFile(pth);
			cmatch.getJSONObject("1").put("result", rs1);
			cmatch.getJSONObject("2").put("result", rs2);
			BufferedWriter writer = Files.newBufferedWriter(pth);
			cmatch.write(writer);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Platform.runLater(() -> {
			this.time.setText("End");
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Match end!");
			if (rs != "time:")
				alert.setHeaderText((rs.replace(":", "").equals("blue_win") ? "Blue" : "Red")
						+ " has won the match on field " + matchid);
			else
				alert.setHeaderText("Time has run out on field " + matchid);
			((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(ServerApp.ICON);
			alert.show();
		});
	}

	public void killPlayer(String[] args, boolean b) {
		ObservableList<Node> red_team = this.getChildren().filtered(nd -> {
			return nd instanceof PlayerLabel && ((PlayerLabel)nd).team == "red";
		});
		ObservableList<Node> blue_team = this.getChildren().filtered(nd -> {
			return nd instanceof PlayerLabel && ((PlayerLabel)nd).team == "blue";
		});
		if (args[0].equals("red")) {
			red_team.get(Integer.valueOf(args[1]) - 1).setDisable(b);
		} else {
			blue_team.get(Integer.valueOf(args[1]) - 1).setDisable(b);
		}
		boolean team_dead = true;
		for (int i = 0; i < 4; i++) {
			if (!red_team.get(i).isDisabled() && !((PlayerLabel)red_team.get(i)).banned) {
				team_dead = false;
				break;
			}
		}
		if (team_dead) {
			onMatchFinished("blue_win:");
		}
		team_dead = true;
		for (int i = 0; i < 4; i++) {
			if (!blue_team.get(i).isDisabled() && !((PlayerLabel)blue_team.get(i)).banned) {
				team_dead = false;
				break;
			}
		}
		if (team_dead) {
			onMatchFinished("red_win:");
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
		this.cmatch = json;
		this.getChildren().clear();
		this.init();
		JSONObject jred = json.getJSONObject("1");
		JSONObject jblue = json.getJSONObject("2");
		team_a.setText(jred.getString("name"));
		team_b.setText(jblue.getString("name"));
		JSONArray pred = jred.getJSONArray("players");
		JSONArray pblue = jblue.getJSONArray("players");
		int i = 1;
		List<String> bans = null;
		List<String> strikes = null;
		try {
			bans = Files.readAllLines(SocketInput.BANS);
			strikes = Files.readAllLines(SocketInput.STRIKES);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		red: for (Object str : pred.toList()) {
			for (String ins : bans) {
				if (ins.equals(str.toString())) {
					this.add(new PlayerLabel(), 1, i);
					ServerApp.sendToAll("ban " + "red:" + i + ":" + matchid);
					i++;
					continue red;
				}
			}
			for (String ins : strikes) {
				if (str.toString().equals(ins)) {
					this.add(new PlayerLabel( "red", i, matchid, str.toString() + " (S)", Color.RED), 1, i);
					ServerApp.sendToAll("set_name red:" + i + ":" + str.toString() + " (S):" + matchid);			
					i++;
					continue red;
				}
			}
			this.add(new PlayerLabel("red", i, matchid, str.toString(), Color.RED), 1, i);
			ServerApp.sendToAll("set_name red:" + i + ":" + str.toString() + ":" + matchid);
			i++;
		}
		i = 1;
		blue: for (Object str : pblue.toList()) {
			for (String ins : bans) {
				if (ins.equals(str.toString())) {
					this.add(new PlayerLabel(), 2, i);
					if(i < 5)ServerApp.sendToAll("ban " + "blue:" + i + ":" + matchid);
					i++;
					continue blue;
				}
			}
			for (String ins : strikes) {
				if (str.toString().equals(ins)) {
					this.add(new PlayerLabel("blue", i, matchid, str.toString() + " (S)", Color.AQUA), 2, i);
					if(i < 5)ServerApp.sendToAll("set_name blue:" + i + ":" + str.toString() + " (S):" + matchid);			
					i++;
					continue blue;
				}
			}
			this.add(new PlayerLabel("blue", i, matchid, str.toString(), Color.AQUA), 2, i);
			if(i < 5)ServerApp.sendToAll("set_name blue:" + i + ":" + str.toString() + ":" + matchid);
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
		thr = new Thread(this);
		thr.start();
	}
}
