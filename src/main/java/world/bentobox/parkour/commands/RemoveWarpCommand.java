package world.bentobox.parkour.commands;

import java.util.List;
import java.util.Objects;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.parkour.Parkour;
import world.bentobox.parkour.ParkourManager;

public class RemoveWarpCommand extends CompositeCommand {

    public RemoveWarpCommand(CompositeCommand parent) {
        super(parent, "removewarp");
    }

    @Override
    public void setup() {
        this.setPermission("parkour.removewarp");
        setOnlyPlayer(true);
        setDescription("parkour.commands.parkour.removewarp.description");
        setConfigurableRankCommand();
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            this.showHelp(this, user);
            return false;
        }
        Island island = getIslands().getIsland(getWorld(), user);
        // Check rank to use command
        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK, user.getTranslation(getPlugin().getRanksManager().getRank(rank)));
            return false;
        }

        ParkourManager pm = ((Parkour)getAddon()).getPm();
        if (pm.getWarpSpot(island).isEmpty()) {
            user.sendMessage("parkour.errors.no-warp");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        ParkourManager pm = ((Parkour)getAddon()).getPm();
        Island island = getIslands().getIsland(getWorld(), user);
        user.sendMessage("parkour.warp.removed");
        pm.setWarpSpot(island, null);
        return true;
    }

}
