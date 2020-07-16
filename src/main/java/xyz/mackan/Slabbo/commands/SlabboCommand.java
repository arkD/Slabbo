package xyz.mackan.Slabbo.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import xyz.mackan.Slabbo.GUI.ShopDeletionGUI;
import xyz.mackan.Slabbo.Slabbo;
import xyz.mackan.Slabbo.importers.ImportResult;
import xyz.mackan.Slabbo.importers.UShopImporter;
import xyz.mackan.Slabbo.types.Shop;
import xyz.mackan.Slabbo.utils.DataUtil;
import xyz.mackan.Slabbo.utils.ItemUtil;
import xyz.mackan.Slabbo.utils.ShopUtil;

import java.io.File;
import java.util.*;

@CommandAlias("slabbo")
@Description("Base command for slabbo")
public class SlabboCommand extends BaseCommand {
	public Shop getLookingAtShop (Player player) {
		Block lookingAt = player.getTargetBlock((Set<Material>) null, 6);

		String locationString = ShopUtil.locationToString(lookingAt.getLocation());

		if (Slabbo.shopUtil.shops.containsKey(locationString)) {
			return Slabbo.shopUtil.shops.get(locationString);
		}

		return null;
	}

	@HelpCommand
	public static void onCommand(CommandSender sender, CommandHelp help) {
		//sender.sendMessage("=====[ Slabbo ]=====");
		help.showHelp();
	}

	@Subcommand("reload")
	@Description("Reloads Slabbo")
	@CommandPermission("slabbo.reload")
	public void onReload (Player player) {
		player.sendMessage(Slabbo.localeManager.getString("general.general.reloading")+" Slabbo");
		for (String shopKey : Slabbo.shopUtil.shops.keySet()) {
			Shop shop = Slabbo.shopUtil.shops.get(shopKey);

			Item droppedItem = ItemUtil.findItemEntity(shop.location);

			if (droppedItem != null) {
				droppedItem.remove();
			}
		}

		Slabbo.chestLinkUtil.links = new HashMap<String, Shop>();
		Slabbo.shopUtil.shops = new HashMap<String, Shop>();
		Slabbo.shopUtil.shopsByOwnerId = new HashMap<UUID, List<Shop>>();

		Slabbo.shopUtil.loadShops();

		for (Map.Entry<String, Shop> shopEntry : Slabbo.shopUtil.shops.entrySet()) {
			String key = shopEntry.getKey();
			Shop shop = shopEntry.getValue();

			UUID itemUUID = UUID.randomUUID();

			shop.droppedItemId = itemUUID;

			Location dropLocation = shop.location.clone();

			dropLocation.add(0.5, 0.5, 0.5);

			ItemUtil.dropItem(dropLocation, shop.item, itemUUID);

			Slabbo.shopUtil.put(key, shop);
		}


		player.sendMessage("Slabbo "+Slabbo.localeManager.getString("general.general.reloaded")+"!");
	}

	@Subcommand("info")
	@Description("Shows information about Slabbo")
	@CommandPermission("slabbo.info")
	public void onInfo (Player sender) {
		sender.sendMessage("=====[ Slabbo Info ]=====");

		sender.sendMessage("Version: "+ Slabbo.getInstance().getDescription().getVersion());
		sender.sendMessage("Total Shops: "+Slabbo.shopUtil.shops.size());
		sender.sendMessage("Economy Provider: "+Slabbo.getEconomy().getName());

		sender.sendMessage("=====[ Slabbo Info ]=====");
	}

	@Subcommand("toggleadmin")
	@Description("Toggles the shop as being an item shop")
	@CommandPermission("slabbo.admin")
	public void onToggleAdmin (Player player) {
		Shop lookingAtShop = getLookingAtShop(player);
		if (lookingAtShop == null) {
			player.sendMessage(ChatColor.RED+Slabbo.localeManager.getString("error-message.general.not-a-shop"));
			return;
		}

		lookingAtShop.admin = !lookingAtShop.admin;

		if (lookingAtShop.admin) {
			player.sendMessage(ChatColor.GREEN+Slabbo.localeManager.getString("success-message.general.admin-create"));
		} else {
			player.sendMessage(ChatColor.GREEN+Slabbo.localeManager.getString("success-message.general-admin-destroy"));
		}

		Slabbo.shopUtil.shops.put(lookingAtShop.getLocationString(), lookingAtShop);

		DataUtil.saveShops();
	}

	@Subcommand("destroy")
	@Description("Destroys a shop")
	@CommandPermission("slabbo.destroy")
	public void onDestroyShop(Player player) {
		Shop lookingAtShop = getLookingAtShop(player);
		if (lookingAtShop == null) {
			player.sendMessage(ChatColor.RED+Slabbo.localeManager.getString("error-message.general.not-a-shop"));
			return;
		}

		boolean isShopOwner = lookingAtShop.ownerId.equals(player.getUniqueId());
		boolean canDestroyOthers = player.hasPermission("slabbo.destroy.others");

		if (!isShopOwner) {
			if (!canDestroyOthers) {
				player.sendMessage(ChatColor.RED+Slabbo.localeManager.getString("error-message.general.not-shop-owner"));
				return;
			}
		}

		ShopDeletionGUI deletionGUI = new ShopDeletionGUI(lookingAtShop);
		deletionGUI.openInventory(player);
	}

	@Subcommand("import")
	@Description("Imports shop from another plugin")
	@CommandPermission("slabbo.importshops")
	@CommandCompletion("ushops @importFiles")
	public void onImportShops(Player player, String type, String file) {
		File importFile = new File(Slabbo.getDataPath()+"/"+file);

		if (!importFile.exists()) {
			player.sendMessage(ChatColor.RED+Slabbo.localeManager.getString("error-message.import.file-not-found"));
			return;
		}

		ImportResult result;

		switch (type.toLowerCase()) {
			case "ushops":
				player.sendMessage(Slabbo.localeManager.getString("success-message.import.importing"));
				result = UShopImporter.importUShops(importFile);
				break;
			default:
				player.sendMessage(ChatColor.RED+Slabbo.localeManager.getString("error-message.import.plugin-not-supported"));
				return;
		}

		if (result == null) {
			player.sendMessage(ChatColor.RED+Slabbo.localeManager.getString("error-message.import.general-error"));
			return;
		}

		for (Shop shop : result.shops) {
			UUID itemUUID = UUID.randomUUID();

			Location dropLocation = shop.location.clone();

			dropLocation.add(0.5, 0.5, 0.5);

			ItemUtil.dropItem(dropLocation, shop.item, itemUUID);

			shop.droppedItemId = itemUUID;

			Slabbo.shopUtil.put(shop.getLocationString(), shop);
		}

		DataUtil.saveShops();

		HashMap<String, Object> replacementMap = new HashMap<String, Object>();

		replacementMap.put("count", result.shops.size());
		replacementMap.put("skipped", result.skippedShops.size());

		player.sendMessage(ChatColor.GREEN+Slabbo.localeManager.replaceKey("success-message.import.success", replacementMap));
	}

	@Subcommand("modify")
	@Description("Modifies the shop")
	@CommandPermission("slabbo.modify")
	public class SlabboModifyCommand extends BaseCommand {
		@HelpCommand
		public void onCommand(CommandSender sender, CommandHelp help) {
			//sender.sendMessage("=====[ Slabbo Modification ]=====");
			help.showHelp();
		}

		@Subcommand("buyprice")
		@Description("Sets the buying price for the shop")
		@CommandPermission("slabbo.modify.buyprice")
		public void onModifyBuyPrice(Player player, int newBuyingPrice) {
			if (newBuyingPrice < -1) {
				player.sendMessage(ChatColor.RED+"Please provide a positive buy price, or -1 for not buying.");
				return;
			}

			Shop lookingAtShop = getLookingAtShop(player);
			if (lookingAtShop == null) {
				player.sendMessage(ChatColor.RED+"That's not a shop.");
				return;
			}

			boolean isShopOwner = lookingAtShop.ownerId.equals(player.getUniqueId());
			boolean canModifyOthers = player.hasPermission("slabbo.modify.buyprice.others");

			if (!isShopOwner) {
				if (!canModifyOthers) {
					player.sendMessage(ChatColor.RED+"That's not your shop.");
					return;
				}
			}

			lookingAtShop.buyPrice = newBuyingPrice;

			player.sendMessage(ChatColor.GREEN+"Buy price set to "+newBuyingPrice);

			Slabbo.shopUtil.shops.put(lookingAtShop.getLocationString(), lookingAtShop);

			DataUtil.saveShops();
		}

		@Subcommand("sellprice")
		@Description("Sets the selling price for the shop")
		@CommandPermission("slabbo.modify.sellprice")
		public void onModifySellPrice(Player player, int newSellingPrice) {
			if (newSellingPrice < -1) {
				player.sendMessage(ChatColor.RED+"Please provide a positive sell price.");
				return;
			}

			Shop lookingAtShop = getLookingAtShop(player);
			if (lookingAtShop == null) {
				player.sendMessage(ChatColor.RED+"That's not a shop.");
				return;
			}

			boolean isShopOwner = lookingAtShop.ownerId.equals(player.getUniqueId());
			boolean canModifyOthers = player.hasPermission("slabbo.modify.sellprice.others");

			if (!isShopOwner) {
				if (!canModifyOthers) {
					player.sendMessage(ChatColor.RED+"That's not your shop.");
					return;
				}
			}

			lookingAtShop.sellPrice = newSellingPrice;

			player.sendMessage(ChatColor.GREEN+"Sell price set to "+newSellingPrice);

			Slabbo.shopUtil.shops.put(lookingAtShop.getLocationString(), lookingAtShop);

			DataUtil.saveShops();
		}

		@Subcommand("quantity")
		@Description("Sets the quantity for the shop")
		@CommandPermission("slabbo.modify.quantity")
		public void onModifyQuantity(Player player, int newQuantity) {
			if (newQuantity < 0) {
				player.sendMessage(ChatColor.RED+"Please provide a positive quantity.");
				return;
			}

			Shop lookingAtShop = getLookingAtShop(player);
			if (lookingAtShop == null) {
				player.sendMessage(ChatColor.RED+"That's not a shop.");
				return;
			}

			boolean isShopOwner = lookingAtShop.ownerId.equals(player.getUniqueId());
			boolean canModifyOthers = player.hasPermission("slabbo.modify.quantity.others");

			if (!isShopOwner) {
				if (!canModifyOthers) {
					player.sendMessage(ChatColor.RED+"That's not your shop.");
					return;
				}
			}

			lookingAtShop.quantity = newQuantity;

			player.sendMessage(ChatColor.GREEN+"Quantity set to "+newQuantity);

			Slabbo.shopUtil.shops.put(lookingAtShop.getLocationString(), lookingAtShop);

			DataUtil.saveShops();
		}
	}
}
