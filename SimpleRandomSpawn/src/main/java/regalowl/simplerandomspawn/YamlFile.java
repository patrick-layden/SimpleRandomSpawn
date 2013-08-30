package regalowl.simplerandomspawn;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


public class YamlFile {

	private FileConfiguration config;
	private File configFile;
	private FileConfiguration players;
	private File playersFile;
    private Logger log;
    private SimpleRandomSpawn srs;
    
    YamlFile() {
    	srs = SimpleRandomSpawn.srs;
    	log = Logger.getLogger("Minecraft");
        configFile = new File(Bukkit.getServer().getPluginManager().getPlugin("SimpleRandomSpawn").getDataFolder(), "config.yml");  
        playersFile = new File(Bukkit.getServer().getPluginManager().getPlugin("SimpleRandomSpawn").getDataFolder(), "players.yml");  
        checkYml();
        config = new YamlConfiguration();
        players = new YamlConfiguration();
        loadYamls();
    }


	private void checkYml() {
		try {
			if (!configFile.exists()) {
				configFile.getParentFile().mkdirs();
				copy(this.getClass().getResourceAsStream("/config.yml"), configFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (!playersFile.exists()) {
				playersFile.getParentFile().mkdirs();
				copy(this.getClass().getResourceAsStream("/players.yml"), playersFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    private void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void loadYamls() {
        try {
            config.load(configFile);
        } catch (Exception e) {
	    	log.info("[SimpleRandomSpawn]Bad config.yml file, disabling plugin.");
	    	srs.getServer().getPluginManager().disablePlugin(srs);
        }
        try {
            players.load(playersFile);
        } catch (Exception e) {
	    	log.info("[SimpleRandomSpawn]Bad players.yml file, disabling plugin.");
	    	srs.getServer().getPluginManager().disablePlugin(srs);
        }
    }
    
    public void saveYamls() {
        try {
        	config.save(configFile); 
        	players.save(playersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void savePlayers() {
        try {
        	players.save(playersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
	public FileConfiguration config(){
		return config;
	}
	
	public FileConfiguration players(){
		return players;
	}
}
