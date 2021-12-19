package com.chai.alwaysInfect;

import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DeathListener implements Listener
{
	private Random rand = new Random();
	
	@EventHandler
	public void onPreDeath(EntityDamageByEntityEvent event)
	{
		EntityType killer = event.getDamager().getType();
		
		// Villager hurt by zombie
		if (event.getEntityType().equals(EntityType.VILLAGER)
			&& (killer.equals(EntityType.ZOMBIE) || killer.equals(EntityType.ZOMBIE_VILLAGER)
				|| killer.equals(EntityType.HUSK) || killer.equals(EntityType.DROWNED)))
		{
			// Get health
			Villager victim = (Villager)event.getEntity();
			
			// Would he get killed by this attack?
			if (victim.getHealth() - event.getDamage() <= 0)
			{
				// Cancel event
				event.setCancelled(true);
				
				// Chance to let villager die normally
				if (rand.nextDouble() > Main.getInstance().getConfig().getDouble("chance"))
				{
					victim.damage(100);
					return;
				}
				
				// Spawn our own zombie villager
				Location loc = victim.getLocation();
				World world = victim.getWorld();
				ZombieVillager zombie = (ZombieVillager)world.spawnEntity(loc, EntityType.ZOMBIE_VILLAGER);
				
				// Play villager death and infect sound
				world.playSound(loc, Sound.ENTITY_VILLAGER_DEATH, 1, 1);
				world.playSound(loc, Sound.ENTITY_ZOMBIE_INFECT, 1, 1);
				victim.setInvulnerable(true); // To prevent the villager being hit multiple times in the next tick
				
				// The command needs to be delayed one tick for the zombie to fully spawn
				Bukkit.getScheduler().runTaskLater(Main.getInstance(), () ->
				{
					// Store original attributes, I could hard code them in but this is future-proofed
					double speed = zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
					double armor = zombie.getAttribute(Attribute.GENERIC_ARMOR).getValue();
					double follow = zombie.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).getValue();
					double health = zombie.getHealth();
					
					// Hiding console feedback
					Boolean sendCommandFeedback = world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
					world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
					
					// Transfer data using minecraft commands because spigot doesn't have api for this yet
					UUID vUUID = victim.getUniqueId();
					UUID zUUID = zombie.getUniqueId();
					String cmd = String.format("data modify entity @e[nbt={UUIDMost:%sL,UUIDLeast:%sL},limit=1] {} merge from entity @e[nbt={UUIDMost:%sL,UUIDLeast:%sL},limit=1]",
								zUUID.getMostSignificantBits(), zUUID.getLeastSignificantBits(), vUUID.getMostSignificantBits(), vUUID.getLeastSignificantBits());
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
					
					// Return gamerule to original value
					world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, sendCommandFeedback);
					
					// Not all data transfers properly so we need to normalize it
					zombie.setBaby(!victim.isAdult());
					zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
					zombie.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
					zombie.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(follow);
					zombie.setHealth(health);
					zombie.setInvulnerable(false);
					
					// Remove old villager
					victim.remove();
				}, 1L);
			}
		}
	}
}