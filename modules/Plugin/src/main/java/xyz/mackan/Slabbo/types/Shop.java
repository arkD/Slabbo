package xyz.mackan.Slabbo.types;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import xyz.mackan.Slabbo.Slabbo;
import xyz.mackan.Slabbo.abstractions.ISlabboSound;
import xyz.mackan.Slabbo.abstractions.SlabboAPI;
import xyz.mackan.Slabbo.utils.ShopUtil;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@SerializableAs("Shop")
public class Shop implements Cloneable, ConfigurationSerializable {
	private static final long serialVersionUID = -1358999872552913870L;

	public int buyPrice;
	public int sellPrice;
	public int quantity;

	public Location location;

	public ItemStack item;

	public int stock = 0;

	public UUID ownerId;

	public boolean admin;

	public String note = "";

	//public UUID droppedItemId;

	public String linkedChestLocation;

	public Shop (int buyPrice, int sellPrice, int quantity, Location location, ItemStack item, int stock, UUID ownerId, boolean admin, String linkedChestLocation) {
		this.buyPrice = buyPrice;
		this.sellPrice = sellPrice;
		this.quantity = quantity;
		this.location = location;
		this.item = item;
		this.stock = stock;
		this.ownerId = ownerId;
		this.admin = admin;
		this.linkedChestLocation = linkedChestLocation;
	}

	public Shop (int buyPrice, int sellPrice, int quantity, Location location, ItemStack item, int stock) {
		this.buyPrice = buyPrice;
		this.sellPrice = sellPrice;
		this.quantity = quantity;
		this.location = location;
		this.item = item;
		this.stock = stock;
	}

	public Shop (int buyPrice, int sellPrice, int quantity, Location location, ItemStack item) {
		this.buyPrice = buyPrice;
		this.sellPrice = sellPrice;
		this.quantity = quantity;
		this.location = location;
		this.item = item;
	}

	public Map<String, Object> serialize () {
		LinkedHashMap result = new LinkedHashMap();

		result.put("buyPrice", buyPrice);
		result.put("sellPrice", sellPrice);
		result.put("quantity", quantity);
		result.put("location", location);
		result.put("item", item);
		result.put("stock", stock);
		result.put("admin", admin);
		result.put("ownerId", ownerId.toString());

		result.put("linkedChestLocation", linkedChestLocation);

		result.put("note", note);

		return result;
	}

	public static Shop deserialize (Map<String, Object> args) {
		int buyPrice = (Integer) args.get("buyPrice");
		int sellPrice = (Integer) args.get("sellPrice");
		int quantity = (Integer) args.get("quantity");
		int stock = (Integer) args.get("stock");

		Location location = (Location) args.get("location");

		ItemStack item = (ItemStack) args.get("item");

		String loadedOwnerId = (String) args.get("ownerId");

		UUID ownerId = UUID.fromString(loadedOwnerId);

		boolean admin = (boolean) args.get("admin");

		String linkedChestLocation = (String) args.get("linkedChestLocation");

		String note = (String) args.getOrDefault("note", "Let's trade!");


		Shop newShop = new Shop(buyPrice, sellPrice, quantity, location, item, stock, ownerId, admin, linkedChestLocation);

		newShop.note = note;

		return newShop;
	}

	public String getLocationString () {
		return ShopUtil.locationToString(this.location);
	}

	public String getInfoString () {
		SlabboAPI api = Bukkit.getServicesManager().getRegistration(SlabboAPI.class).getProvider();

		return String.format(
			"[%s] | Item: %s | Owner: %s",
			getLocationString(),
			api.getItemName(item),
			Bukkit.getOfflinePlayer(ownerId).getName()
		);
	}
}