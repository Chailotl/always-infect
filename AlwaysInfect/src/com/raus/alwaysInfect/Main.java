package com.raus.alwaysInfect;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin
{
	private static Main main;
	
	public Main()
	{
		main = this;
	}
	
	@Override
	public void onEnable()
	{
		// Safe default stuff
		saveDefaultConfig();
		
		// Register command
		this.getCommand("alwaysinfect").setExecutor(new ReloadCommand());
		
		// Listeners
		getServer().getPluginManager().registerEvents(new DeathListener(), this);
	}
	
	@Override
	public void onDisable()
	{
		
	}
	
	public static Main getInstance()
	{
		return main;
	}
}