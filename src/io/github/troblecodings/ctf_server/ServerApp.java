package io.github.troblecodings.ctf_server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.json.*;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Pair;
import jdk.nashorn.internal.parser.JSONParser;

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

/**
 * @author MrTroble
 *
 */
public class ServerApp extends Application implements Runnable{

	public static LoggerFile LOGGER;
	private static ServerSocket sslserver;
	public static ArrayList<Socket> sockets = new ArrayList<>();
	public static ExecutorService service;
	private static Path path_plan = Paths.get("game_plans");
	private static Path path_history = Paths.get("match_history");
	private static Path path_log = Paths.get("logs");
	private static ListView<Label> plans = new ListView<Label>();
	private static MatchPane matchpane = new MatchPane();

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		Date date = new Date();
		if(!Files.exists(path_log))Files.createDirectory(path_log);
		LOGGER = new LoggerFile(new FileOutputStream(
				new File(path_log.toFile(), "log-" + date.getMonth() + "-" + date.getDay() + "-" + (1900 + date.getYear()) + "-"
						+ date.getHours() + "-" + date.getMinutes() + "-" + date.getSeconds() + ".log")));
		if(!Files.exists(path_plan))Files.createDirectory(path_plan);
		if(!Files.exists(path_history))Files.createDirectory(path_history);
		launch(args);
	}
	
	public static void sendToAll(String nm) {
		Iterator<Socket> it = sockets.iterator();
		while(it.hasNext()) {
			Socket sk = it.next();
			PrintWriter wr;
			try {
				wr = new PrintWriter(sk.getOutputStream());
				wr.println(nm);
				wr.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			LOGGER.println(sk + " sended to: " + nm);
		}
	}

	/* (non-Javadoc)
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void start(Stage primaryStage) throws Exception {
		//Starting server
		Thread th = new Thread(this);
		th.start();
		
		GridPane root = new GridPane();
		Scene sc = new Scene(root, 1000, 600);
		primaryStage.setScene(sc);
		primaryStage.setResizable(false);
		primaryStage.setOnCloseRequest(ev -> {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setHeaderText("Shutdown?");
			alert.setContentText("Would your really like to shutdown the server?");
			alert.setTitle("Close?");
			alert.getButtonTypes().addAll(ButtonType.CANCEL);
			Optional<ButtonType> btn = alert.showAndWait();
			if(btn.isPresent() && btn.get() == ButtonType.OK) {
				try {
					th.stop();
					ServerApp.sslserver.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.exit(0);
			} else {
				ev.consume();
			}
		});
		root.setVgap(15);
		root.setHgap(15);
		
		GridPane players = new GridPane();
		players.setHgap(15);
		players.setVgap(15);
		players.setPrefSize(485, 285);
		players.setPadding(new Insets(15));
		for (int y = 1; y < 5; y++) {
			players.add(new Label("Player " + y), 0, y);
		}
		for (int x = 1; x < 3; x++) {
			players.add(new Label("Team " + (x==1?"Red":"Blue")), x, 0);
			for (int y = 1; y < 5; y++) {
				players.add(new PlayerField(x == 1, y), x, y);
			}
		}
		Button btn = new Button("Change");
		btn.setOnAction(ev -> {
			players.getChildren().filtered(nd -> {return nd instanceof PlayerField;}).forEach(nd -> {
				((TextField)nd).onActionProperty().getValue().handle(new ActionEvent());
			});    
		});
		players.add(btn, 1, 5);
		
		Button setup = new Button("New");
		setup.setOnAction(ev -> {
			Dialog<Pair<String, String>> alert = new Dialog<>();
			alert.setTitle("Create new match");
			GridPane pane = new GridPane();
			
			pane.add(new Label("Red teamname"), 0, 0);
			pane.add(new Label("Blue teamname"), 0, 1);
			
			TextField red = new TextField("teamname");
			TextField blue = new TextField("teamname");
			pane.add(red, 1, 0);
			pane.add(blue, 1, 1);
			pane.setVgap(15);
			pane.setHgap(15);
			pane.setPadding(new Insets(15));
			ButtonType type = new ButtonType("Create");
			alert.getDialogPane().getButtonTypes().addAll(type, ButtonType.CANCEL);
			alert.getDialogPane().setContent(pane);
			alert.setResultConverter(tp -> {
				if(tp == type)return new Pair<String, String>(red.getText(), blue.getText());
				return null;
			});
			alert.showAndWait().ifPresent(pr -> {
				JSONObject obj = new JSONObject();
				JSONObject jred = new JSONObject();
				jred.put("name", pr.getKey());
				JSONArray rarray = new JSONArray();
				players.getChildren().filtered(pd -> {return pd instanceof PlayerField && ((PlayerField)pd).isRed();}).forEach(nxt -> {
					rarray.put(((PlayerField)nxt).getText());
				});
				jred.put("players", rarray);
				JSONObject jblue = new JSONObject();
				JSONArray barray = new JSONArray();
				players.getChildren().filtered(pd -> {return pd instanceof PlayerField && !((PlayerField)pd).isRed();}).forEach(nxt -> {
					barray.put(((PlayerField)nxt).getText());
				});
				jblue.put("players", barray);
				jblue.put("name", pr.getValue());
				
				obj.put("1", jred);
				obj.put("2", jblue);
				try {
					PrintWriter writer = new PrintWriter(new File(path_plan.toFile(), pr.getKey() + " vs " + pr.getValue() + ".json"));
					obj.write(writer);
					writer.flush();
					writer.close();
					updatePlanList();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});;
		});
		players.add(setup, 0, 5);
		
		root.add(players, 0, 0);
		matchpane.setHgap(15);
		matchpane.setVgap(15);
		matchpane.setPrefSize(485, 285);
		matchpane.setPadding(new Insets(15));
		root.add(matchpane, 0, 1);
		
		updatePlanList();
		plans.setPrefSize(500, 300);
		root.add(plans, 1, 0);
		
		primaryStage.show();
	}
	
	private void updatePlanList() {
		ObservableList<Label> itms = plans.getItems();
		itms.clear();
		itms.add(new Label("List of matches"));
		try {
			Files.list(path_plan).filter(pth -> {return Files.isRegularFile(pth) && pth.toString().endsWith(".json");}).forEach(pth -> {
				Label lab = new Label(pth.getFileName().toString().replace(".json", ""));
				lab.setOnMouseClicked(evn -> {
					if(evn.getClickCount() > 1) {
						Alert alert = new Alert(AlertType.CONFIRMATION);
						alert.setTitle("Load?");
						alert.setHeaderText("Do you want to load this match?");
						alert.setContentText("Match: " + lab.getText());
						alert.showAndWait().ifPresent(tp -> {
							if(tp == ButtonType.OK) {
								plans.getItems().forEach(lb -> {lb.setDisable(false);});
								try {
									String str = new String(Files.readAllBytes(Paths.get(path_plan.toString(), lab.getText() + ".json")));
									JSONObject obj = new JSONObject(str);
									matchpane.fillWithJson(obj);
								} catch (IOException e) {
									e.printStackTrace();
								}
								lab.setDisable(true);
							}
						});;
					}
				});
				itms.add(lab);
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run(){
		try {
			sslserver = new ServerSocket(555);
			service =  Executors.newCachedThreadPool();
			LOGGER.println("Started server!");
			while (true) {
				Socket sk = sslserver.accept();
				LOGGER.println(sk + " connected to server");
				sockets.add(sk);
				service.submit(new SocketInput(sk));
			}
		} catch (Exception e) {
			e.printStackTrace(LOGGER);
		}
	}

}
