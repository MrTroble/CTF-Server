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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;

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
	private boolean isRunning = false,isPause = false, isLoaded = false;
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
			dialog.setResultConverter(bt ->  {
				if(bt.equals(ButtonType.APPLY))return plans.getSelectionModel().getSelectedItem().getText();
				return null;
			});
			ObservableList<Label> itms = plans.getItems();
			itms.clear();
			itms.add(new Label("List of matches"));

			try {
				Files.list(ServerApp.path_plan).filter(pth -> {return Files.isRegularFile(pth) && pth.toString().endsWith(".json");}).forEach(pth -> {
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
					if(tp == ButtonType.OK) {
						try {
							loaded_file = Paths.get(ServerApp.path_plan.toString(), stri + ".json");
							String str = new String(Files.readAllBytes(loaded_file));
							JSONObject obj = new JSONObject(str);
							if(this.fillWithJson(obj)) {}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});;
			});
		});
		start = new Button("Start");
		start.setOnAction(evn -> {
			start.setDisable(true);
			if(!this.isPause)this.start();
			else ServerApp.sendToAll("match_unpause " + matchid);
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
		ServerApp.sendToAll("match_start " + matchid);
		last = new Date().getTime();
		long ne = 0;
		long delta = 240000;
		while (delta > 0) {
			if(this.isPause) {
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
			cmatch.getJSONObject("1").put("result", rs.replace(":", "").equals("red_win") ? 1:0);
			cmatch.getJSONObject("2").put("result", rs.replace(":", "").equals("blue_win") ? 1:0);
			Path pth = Paths.get(ServerApp.path_history.toString(), team_a.getText() + " vs " + team_b.getText() + ".json");
			if(Files.exists(pth))Files.delete(pth);
			BufferedWriter writer = Files.newBufferedWriter(Files.createFile(pth));
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
			if(rs != "time:")alert.setHeaderText((rs.replace(":", "").equals("blue_win") ? "Blue":"Red") + " has won the match on field " + matchid);
			else alert.setHeaderText("Time has run out on field " + matchid);
			((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(ServerApp.ICON);
			alert.show();
		});
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
			onMatchFinished("blue_win:");
		}
		team_dead = true;
		for (int i = 4; i < 8; i++) {
			if (!sorted.get(i).isDisabled()) {
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
		JSONObject jred = (JSONObject) json.getJSONObject("1");
		JSONObject jblue = (JSONObject) json.getJSONObject("2");
		team_a.setText(jred.getString("name"));
		team_b.setText(jblue.getString("name"));
		JSONArray pred = jred.getJSONArray("players");
		JSONArray pblue = jblue.getJSONArray("players");
		int i = 1;
		for (Object str : pred.toList()) {
			this.add(new PlayerLabel(str.toString(), Color.RED), 1, i);
			ServerApp.sendToAll("set_name red:" + i + ":" + str.toString() + ":" + matchid);
			i++;
		}
		i = 1;
		for (Object str : pblue.toList()) {
			this.add(new PlayerLabel(str.toString(), Color.AQUA), 2, i);
			ServerApp.sendToAll("set_name blue:" + i + ":" + str.toString() + ":" + matchid);
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
