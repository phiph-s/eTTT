package de.melays.ettt.game.lobby;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import de.melays.ettt.Main;
import de.melays.ettt.PlayerTools;
import de.melays.ettt.game.Arena;
import de.melays.ettt.tools.ScoreBoardTools;

public class Lobby {

	Main main;
	
	LobbyMode mode;
	Arena arena = null;
	Location lobby;
	
	int min;
	int max;
	
	int start;
	int full;
	
	//Players
	public ArrayList<Player> players = new ArrayList<Player>();
	HashMap<Player , ScoreBoardTools> scoreboard = new HashMap<Player , ScoreBoardTools>();
	
	public Lobby (Main main , Location loc) {
		this.main = main;
		this.lobby = loc;
		min = main.getConfig().getInt("bungeecord.players.min");
		max = main.getConfig().getInt("bungeecord.players.max");
		start = main.getConfig().getInt("game.countdowns.lobby.start");
		full = main.getConfig().getInt("game.countdowns.lobby.full");
		counter = start;
	}
	
	public void setMode (LobbyMode mode) {
		this.mode = mode;
	}
	
	public void setArena (Arena arena) {
		this.arena = arena;
		min = arena.min;
		max = arena.max;
	}
	
	public void broadcast (String msg) {
		for (Player p : players) {
			p.sendMessage(Main.c(msg));
		}
	}
	
	//Loop
	
	int counter = 0;
	int id;
	
	public void startLoop() {
		id = Bukkit.getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {

			@Override
			public void run() {
				
				for (Player p : players) {
					updateScoreBoard(p);
				}
				
				if (counter > full & players.size() >= max) {
					counter = full;
					broadcast(main.getMessageFetcher().getMessage("game.countdown.lobby.shortend", true));
				}
				
				if (counter <= start && players.size() < min) {
					counter = start;
				}
				else if (counter == 0) {
					broadcast(main.getMessageFetcher().getMessage("game.countdown.lobby.start", true));
					Bukkit.getScheduler().cancelTask(id);
					counter = start;
					moveToArena();
				}
				else {
					if ((counter >= 30 && counter % 15 == 0) || (counter < 30 && counter % 10 == 0) || counter <= 5) {
						broadcast(main.getMessageFetcher().getMessage("game.countdown.lobby.lobby", true).replaceAll("%seconds%", counter + ""));
					}
					
					counter -= 1;
				}
			}
			
		}, 0, 20);
	}
	
	//Player methods
	
	public void createScoreboard(Player p) {
		ScoreBoardTools tools = new ScoreBoardTools(p , Main.c(main.getSettingsFile().getConfiguration().getString("game.scoreboard.lobby.title")));
		List<String> lines = main.getSettingsFile().getConfiguration().getStringList("game.scoreboard.lobby.content");
		int value = lines.size();
		String arenaplz = "Voting";
		if (arena != null) {
			arenaplz = arena.display;
		}
		for (String s : lines) {
			if (s.equals("time-line")) {
				String divider = Main.c(main.getSettingsFile().getConfiguration().getString("game.scoreboard.lobby.timer.divider"));
				String minutes = Main.c(main.getSettingsFile().getConfiguration().getString("game.scoreboard.lobby.timer.minutes"));
				String seconds = Main.c(main.getSettingsFile().getConfiguration().getString("game.scoreboard.lobby.timer.seconds"));
				int sec = (counter) % 60;
				int min = ((counter) - sec) / 60;
				minutes = minutes.replaceAll("%minutes%", String.format("%02d", min) + "");
				seconds = seconds.replaceAll("%seconds%", String.format("%02d", sec) + "");
				tools.addLine("timer", minutes, divider , seconds, value);
			}
			else {
				tools.addNormalLine(s.replaceAll("%arena%", arenaplz) , value);
			}
			value -= 1;
		}
		tools.set();
		this.scoreboard.put(p, tools);
	}
	
	public void updateScoreBoard(Player p) {
		String minutes = Main.c(main.getSettingsFile().getConfiguration().getString("game.scoreboard.lobby.timer.minutes"));
		String seconds = Main.c(main.getSettingsFile().getConfiguration().getString("game.scoreboard.lobby.timer.seconds"));
		int sec = (counter) % 60;
		int min = ((counter) - sec) / 60;
		minutes = minutes.replaceAll("%minutes%", String.format("%02d", min) + "");
		seconds = seconds.replaceAll("%seconds%", String.format("%02d", sec) + "");
		scoreboard.get(p).editPrefix("timer", minutes);
		scoreboard.get(p).editSuffix("timer", seconds);
	}
	
	public void join (Player p) {
		if (!this.contains(p)) {
			players.add(p);
			PlayerTools.resetPlayer(p);
			p.setGameMode(GameMode.valueOf(main.getConfig().getString("gamemodes.lobby").toUpperCase()));
			p.teleport(lobby);
			broadcast(main.getMessageFetcher().getMessage("game.join", true).replaceAll("%player%", p.getName()));
			
			createScoreboard(p);
			
			//Give Items
			if (this.mode == LobbyMode.VOTING && main.getSettingsFile().getConfiguration().getBoolean("game.items.vote.enabled")) {
				p.getInventory().setItem(main.getSettingsFile().getConfiguration().getInt("game.items.vote.slot") , main.getItemManager().getItem("lobby.vote"));
			}
			if (main.getSettingsFile().getConfiguration().getBoolean("game.items.roleselector.enabled")) {
				p.getInventory().setItem(main.getSettingsFile().getConfiguration().getInt("game.items.roleselector.slot") , main.getItemManager().getItem("lobby.roleselector"));
			}
			if (main.getSettingsFile().getConfiguration().getBoolean("game.items.leave.enabled")) {
				p.getInventory().setItem(main.getSettingsFile().getConfiguration().getInt("game.items.leave.slot") , main.getItemManager().getItem("lobby.leave"));
			}
		}
	}
	
	public void remove (Player p) {
		PlayerTools.resetPlayer(p);
		players.remove(p);
	}
	
	public boolean contains (Player p) {
		return players.contains(p);
	}
	
	public void moveToArena() {
		@SuppressWarnings("unchecked")
		ArrayList<Player> all = (ArrayList<Player>) players.clone();
		players.clear();
		arena.receiveFromLobby(all);
	}
	
}
