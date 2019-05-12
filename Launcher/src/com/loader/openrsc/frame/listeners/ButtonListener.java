package com.loader.openrsc.frame.listeners;

import com.loader.openrsc.Constants;
import com.loader.openrsc.frame.AppFrame;
import com.loader.openrsc.util.ClientLauncher;
import com.loader.openrsc.util.Utils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;

public class ButtonListener implements ActionListener {
	@Override
	public void actionPerformed(final ActionEvent event) {
		final String action = event.getActionCommand().toLowerCase();
		switch (action) {
			case "rsc wiki": {
				Utils.openWebpage("https://classic.runescape.wiki");
				return;
			}
			case "bug reports": {
				Utils.openWebpage("https://goo.gl/forms/nnhSln7S81l4I26t2");
				return;
			}
			case "bot reports": {
				Utils.openWebpage("https://goo.gl/forms/AkBzpOzgAmzWiZ8H2");
				return;
			}
			case "discord": {
				Utils.openWebpage("https://discord.gg/94vVKND");
				return;
			}
			case "open rsc": {
				String ip = "game.openrsc.com";
				String port = "43594";
				FileOutputStream fileout;
				try {
					fileout = new FileOutputStream("Cache" + File.separator + "ip.txt");
					OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
					outputWriter.write(ip);
					outputWriter.close();
				} catch (Exception ignored) {
				}
				try {
					fileout = new FileOutputStream("Cache" + File.separator + "port.txt");
					OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
					outputWriter.write(port);
					outputWriter.close();
				} catch (Exception ignored) {
				}
				return;
			}
			case "rsc cabbage": {
				String ip = "cabbage.openrsc.com";
				String port = "43595";
				FileOutputStream fileout;
				try {
					fileout = new FileOutputStream("Cache" + File.separator + "ip.txt");
					OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
					outputWriter.write(ip);
					outputWriter.close();
				} catch (Exception ignored) {
				}
				try {
					fileout = new FileOutputStream("Cache" + File.separator + "port.txt");
					OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
					outputWriter.write(port);
					outputWriter.close();
				} catch (Exception ignored) {
				}
				return;
			}
			case "dev world": {
				String ip = "dev.openrsc.com";
				String port = "43599";
				FileOutputStream fileout;
				try {
					fileout = new FileOutputStream("Cache" + File.separator + "ip.txt");
					OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
					outputWriter.write(ip);
					outputWriter.close();
				} catch (Exception ignored) {
				}
				try {
					fileout = new FileOutputStream("Cache" + File.separator + "port.txt");
					OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
					outputWriter.write(port);
					outputWriter.close();
				} catch (Exception ignored) {
				}
				return;
			}
			case "single player": {
				String ip = "localhost";
				String port = "43594";
				FileOutputStream fileout;
				try {
					fileout = new FileOutputStream("Cache" + File.separator + "ip.txt");
					OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
					outputWriter.write(ip);
					outputWriter.close();
				} catch (Exception ignored) {
				}
				try {
					fileout = new FileOutputStream("Cache" + File.separator + "port.txt");
					OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
					outputWriter.write(port);
					outputWriter.close();
				} catch (Exception ignored) {
				}
				return;
			}
			case "rsc arch angel": {
				Utils.openWebpage("https://www.rscarchangel.com");
				return;
			}
			case "rsc revolution": {
				Utils.openWebpage("https://www.rscrevolution.com");
				return;
			}
			case "rsc dawn": {
				Utils.openWebpage("https://www.rscdawn.com");
				return;
			}
			case "minimize": {
				AppFrame.get().setState(1);
				return;
			}
			case "launch": {
				try {
					// Deletes the client.properties file that may persist unwanted settings between different gamess
					File f = new File(Constants.CONF_DIR + File.separator + "client.properties");
					f.delete();

					ClientLauncher.launchClient();
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
				return;
			}
			case "close": {
				System.exit(0);
				return;
			}
			default:
				break;
		}
		System.out.println(action);
	}
}
