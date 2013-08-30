package regalowl.simplerandomspawn;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import regalowl.autoprotect.AutoProtect;
import regalowl.autoprotect.Claim;
import regalowl.autoprotect.ClaimHandler;



public class SimpleRandomSpawn extends JavaPlugin implements Listener {
	public static SimpleRandomSpawn srs;
	private YamlFile y;
	private AutoProtect ap = null;
	private ArrayList<String> inTransit = new ArrayList<String>();
	private  ArrayList<Material> avoid = new ArrayList<Material>();
	private  ArrayList<Biome> allowedBiomes = new ArrayList<Biome>();
	private BukkitTask saveTask;
	private StringFunctions sf;
	@Override
	public void onEnable() {
		srs = this;
		y = new YamlFile();
		sf = new StringFunctions();
		Plugin x = this.getServer().getPluginManager().getPlugin("AutoProtect");
		if (x != null & x instanceof AutoProtect) {
			ap = AutoProtect.ap;
		}
		getServer().getPluginManager().registerEvents(this, this);
		ArrayList<String> skipNames = sf.explode(y.config().getString("avoid_materials"), ",");
		for (String name:skipNames) {
			Material m = Material.matchMaterial(name);
			if (m == null) {continue;}
			avoid.add(m);
		}
		ArrayList<String> skipBiomes = sf.explode(y.config().getString("allowed_biomes"), ",");
		for (String name:skipBiomes) {
			Biome b = Biome.valueOf(name);
			if (b == null) {continue;}
			allowedBiomes.add(b);
		}
		saveTask = this.getServer().getScheduler().runTaskTimer(this, new Runnable() {
			public void run() {
				y.savePlayers();
			}
		}, 12000L, 12000L);
	}
	
	@Override
	public void onDisable() {
		saveTask.cancel();
		y.savePlayers();
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerJoinEvent event) {  
		if (!event.getPlayer().hasPlayedBefore()) {
			randomTeleport(event.getPlayer());
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player p = event.getPlayer();
		Location bl = p.getBedSpawnLocation();
		if (bl != null) {return;}
		Location ps = getSpawn(p);
		if (ps == null) {
			new RandomTeleport(p);
		} else {
			new Teleport(p, ps);
		}
	}
	
	@EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			if (inTransit.contains(player.getName())) {
				event.setCancelled(true);
			}
		}
	}
	
	
	public void randomTeleport(Player p) {
		if (!inTransit.contains(p.getName())) {
			inTransit.add(p.getName());
		}
		p.sendMessage(ChatColor.GREEN + y.config().getString("message"));
		boolean acceptable = false;
		int counter = 0;
		Location randomSpawn = p.getLocation();
		while (!acceptable) {
			counter++;
			if (counter > 250) {
				return;
			}
			double x = getRandomInRange(y.config().getInt("x_start"), y.config().getInt("x_end")) + .5;
			double z = getRandomInRange(y.config().getInt("z_start"), y.config().getInt("z_end")) + .5;
			randomSpawn.setWorld(p.getWorld());
			randomSpawn.setX(x);
			randomSpawn.setZ(z);
			Block b = getSpawnBlock(randomSpawn, p);
			if (b == null) {continue;}
			randomSpawn.setY(b.getY() + 3);
			boolean fs = p.getAllowFlight();
			p.setAllowFlight(true);
			p.setFlying(true);
			p.setFlySpeed(0);
			if (p.getPassenger() != null) {p.getPassenger().eject();}
			if (p.getVehicle() != null) {p.getVehicle().eject();}
			p.teleport(randomSpawn);
			saveSpawn(p, randomSpawn);
			new Release(p, fs);
			acceptable = true;
		}
	}
	
	
	
	private class RandomTeleport {
		private Player p;
		RandomTeleport(Player player) {
			p = player;
			SimpleRandomSpawn.srs.getServer().getScheduler().runTaskLater(SimpleRandomSpawn.srs, new Runnable() {
			    public void run() {
			    	randomTeleport(p);
			    }
			}, 0L);
		}
	}
	
	private class Teleport {
		private Player p;
		private Location l;
		Teleport(Player player, Location location) {
			p = player;
			l = location;
			SimpleRandomSpawn.srs.getServer().getScheduler().runTaskLater(SimpleRandomSpawn.srs, new Runnable() {
			    public void run() {
			    	if (!l.getChunk().isLoaded()) {
			    		l.getChunk().load(true);
			    	}
					if (p.getPassenger() != null) {p.getPassenger().eject();}
					if (p.getVehicle() != null) {p.getVehicle().eject();}
			    	p.teleport(l);
			    }
			}, 0L);
		}
	}
	
	private class Release {
		private Player p;
		private boolean fs;
		Release(Player player, boolean flightStatus) {
			p = player;
			fs = flightStatus;
			SimpleRandomSpawn.srs.getServer().getScheduler().runTaskLater(SimpleRandomSpawn.srs, new Runnable() {
			    public void run() {
			    	p.setFlySpeed(0.1f);
					p.setFlying(false);
					p.setAllowFlight(fs);
					inTransit.remove(p.getName());
					if (isUnderground(p.getLocation())) {
						randomTeleport(p);
					}
			    }
			}, 20L);
		}
	}
	
	private void saveSpawn(Player p, Location l) {
		y.players().set(p.getName() + ".x", l.getX());
		y.players().set(p.getName() + ".y", l.getY());
		y.players().set(p.getName() + ".z", l.getZ());
		y.players().set(p.getName() + ".world", l.getWorld().getName());
	}
	
	private Location getSpawn(Player p) {
		String test = y.players().getString(p.getName() + ".world");
		if (test == null) {return null;}
		try {
			double px = y.players().getDouble(p.getName() + ".x");
			double py = y.players().getDouble(p.getName() + ".y");
			double pz = y.players().getDouble(p.getName() + ".z");
			World pw = Bukkit.getWorld(y.players().getString(p.getName() + ".world"));
			Location l = p.getLocation();
			l.setX(px);
			l.setY(py);
			l.setZ(pz);
			l.setWorld(pw);
			return l;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Block getSpawnBlock(Location l, Player p) {
		Block b = getFirstSolidBlock(l);
		if (b == null) {return null;}
		if (!allowedBiomes.contains(b.getBiome())) {return null;}
		if (avoid.contains(b.getType())) {return null;}
		if (!hasSpace(b)) {return null;}
		if (ap != null) {
			ClaimHandler ch = ap.getClaimHandler();
			Claim c = ch.getClaim(l);
			if (c != null && !c.getOwner().equalsIgnoreCase(p.getName())) {
				return null;
			}
		}
		return b;
	}
	
	
	public boolean hasSpace(Block b) {
		for (int i = 0; i < 7; i++) {
			b = b.getRelative(BlockFace.UP);
			if (b.getType() != Material.AIR) {
				return false;
			}
		}
		return true;
	}
	
	public Block getFirstSolidBlock(Location l) {
		int y = 128;
		Block nb = l.getWorld().getBlockAt(l.getBlockX(), y, l.getBlockZ());
		while (!(nb.getType() == Material.AIR)) {
			if (y == 0) {return null;}
			nb = l.getWorld().getBlockAt(nb.getX(), y, nb.getZ());
			y--;
		}
		while (!nb.getType().isSolid()) {
			if (y == 0) {return null;}
			nb = l.getWorld().getBlockAt(nb.getX(), y - 1, nb.getZ());
			y--;
		}
		return nb;
	}
	
	public int getRandomInRange(int c1, int c2) {
		int diff = Math.abs(c2 - c1);
		Random generator = new Random();
		int random = generator.nextInt(diff + 1);
		if (c1 < c2) {
			return (c1 + random);
		} else {
			return (c2 + random);
		}
	}

	public boolean isUnderground(Location l) {
		if (l.getBlock().getType().isSolid() || l.getBlock().getRelative(BlockFace.UP).getType().isSolid()) {
			return true;
		}
		return false;
	}


}
