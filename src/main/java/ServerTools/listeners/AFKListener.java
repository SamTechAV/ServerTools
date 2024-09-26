package ServerTools.listeners;

import ServerTools.ServerTools;
import ServerTools.utils.AFKManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class AFKListener implements Listener {

    private final ServerTools plugin;
    private final AFKManager afkManager;

    public AFKListener(ServerTools plugin) {
        this.plugin = plugin;
        this.afkManager = plugin.getAFKManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().distanceSquared(event.getTo()) == 0) return;
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (afkManager.isAFK(player)) {
            event.setCancelled(true); // Prevent hunger depletion
        }
    }
}