package world.bentobox.parkour.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.island.IslandEnterEvent;
import world.bentobox.bentobox.api.events.island.IslandExitEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.parkour.Parkour;

/**
 * @author tastybento
 *
 */
public class VisitorListener extends AbstractListener {

    private Map<UUID, Location> checkpoints = new HashMap<>();
    private Map<UUID, Long> timers = new HashMap<>();

    /**
     * @param addon
     */
    public VisitorListener(Parkour addon) {
        super(addon);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onVisitorArrive(IslandEnterEvent e) {
        // Check if visitor
        User user = User.getInstance(e.getPlayerUUID());
        if (user == null || !user.isOnline() || !addon.inWorld(e.getLocation())) {
            return;
        }
        Island island = e.getIsland();
        Optional<MetaDataValue> metaStart = island.getMetaData(START);
        Optional<MetaDataValue> metaEnd = island.getMetaData(END);
        if (metaStart.isEmpty()) {
            user.sendRawMessage("There is no start set up yet.");
        } else if (metaEnd.isEmpty()) {
            user.sendRawMessage("There is no end set up yet.");
        } else if (!timers.containsKey(e.getPlayerUUID())){
            user.sendRawMessage("To start, step on the gold pressure plate.");
            user.setGameMode(GameMode.SURVIVAL);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onVisitorLeave(IslandExitEvent e) {
        User user = User.getInstance(e.getPlayerUUID());
        if (checkpoints.containsKey(e.getPlayerUUID()) && user != null && user.isOnline()) {
            user.sendRawMessage("You parkour session ended.");
        }
        clear(e.getPlayerUUID());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent e) {
        clear(e.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent e) {
        clear(e.getPlayer().getUniqueId());
    }

    private void clear(UUID uuid) {
        checkpoints.remove(uuid);
        timers.remove(uuid);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onVisitorFall(EntityDamageEvent e) {
        BentoBox.getInstance().logDebug(e.getEventName() + " " + timers.containsKey(e.getEntity().getUniqueId()));
        // Check if visitor
        if (!e.getCause().equals(DamageCause.VOID) || !timers.containsKey(e.getEntity().getUniqueId())) {
            return;
        }
        if (e.getEntity() instanceof Player player) {
            player.playEffect(EntityEffect.ENTITY_POOF);
            player.setVelocity(new Vector(0,0,0));
            player.setFallDistance(0);
            player.teleport(checkpoints.get(player.getUniqueId()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onVisitorCommand(PlayerCommandPreprocessEvent e) {
        if (!timers.containsKey(e.getPlayer().getUniqueId())) {
            return;
        }
        User user = User.getInstance(e.getPlayer());
        user.notify("protection.protected", TextVariables.DESCRIPTION, user.getTranslation("protection.command-is-banned"));
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onStartEndSet(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.PHYSICAL)) return;
        if (e.getClickedBlock() == null || !e.getClickedBlock().getType().equals(Material.LIGHT_WEIGHTED_PRESSURE_PLATE) || !addon.inWorld(e.getPlayer().getLocation())) {
            return;
        }

        Location l = e.getClickedBlock().getLocation();
        User user = Objects.requireNonNull(User.getInstance(e.getPlayer()));
        if (addon.getIslands().getProtectedIslandAt(l).isPresent()) {
            Island island = addon.getIslands().getProtectedIslandAt(l).get();
            Optional<MetaDataValue> metaStart = island.getMetaData(START);
            Optional<MetaDataValue> metaEnd = island.getMetaData(END);
            if (metaStart.filter(mdv -> isLocEquals(l, mdv.asString())).isPresent()) {
                if (!timers.containsKey(e.getPlayer().getUniqueId())) {
                    user.sendRawMessage("Parkour Start!");
                    e.getPlayer().playSound(l, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1F, 1F);
                    timers.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
                    checkpoints.put(user.getUniqueId(), Util.getLocationString(metaStart.get().asString()));
                }
            } else if (metaEnd.filter(mdv -> isLocEquals(l, mdv.asString())).isPresent()) {
                if (timers.containsKey(e.getPlayer().getUniqueId())) {
                    double duration = (System.currentTimeMillis() - timers.get(e.getPlayer().getUniqueId())) / 1000D;
                    user.sendRawMessage("Parkour End!");
                    e.getPlayer().playSound(l, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1F, 1F);
                    user.sendRawMessage("You took " + df2.format(duration) + " seconds");
                    clear(e.getPlayer().getUniqueId());
                } else {
                    user.sendRawMessage("You need to start before ending!");
                }


            }
        }
    }
}