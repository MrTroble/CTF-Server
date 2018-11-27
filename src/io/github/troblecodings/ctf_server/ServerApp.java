package io.github.troblecodings.ctf_server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javafx.application.Application;
import javafx.event.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.stage.*;

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
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		Date date = new Date();
		LOGGER = new LoggerFile(new FileOutputStream(
				new File("log-" + date.getMonth() + "-" + date.getDay() + "-" + (1900 + date.getYear()) + "-"
						+ date.getHours() + "-" + date.getMinutes() + "-" + date.getSeconds() + ".log")));		
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
	@Override
	public void start(Stage primaryStage) throws Exception {
		//Starting server
		Thread th = new Thread(this);
		th.start();
		
		GridPane root = new GridPane();
		Scene sc = new Scene(root, 1000, 600);
		primaryStage.setScene(sc);
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			
			@SuppressWarnings("deprecation")
			@Override
			public void handle(WindowEvent event) {
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
					event.consume();
				}
			}
		});
		
		GridPane players = new GridPane();
		
		for (int x = 0; x < 2; x++) {
			for (int y = 1; y < 5; y++) {
				players.add(new PlayerField(x == 0, y), x, y);
			}
		}
		Button btn = new Button("Change");
		btn.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {
				players.getChildren().filtered(nd -> {return nd instanceof PlayerField;}).forEach(nd -> {
					((TextField)nd).onActionProperty().getValue().handle(new ActionEvent());
				});;
			}
		});
		players.add(btn, 1, 5);
		root.add(players, 0, 0);
		
		primaryStage.show();
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
