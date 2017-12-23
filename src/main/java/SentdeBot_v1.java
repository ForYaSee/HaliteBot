import hlt.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
 * Original bot in python: https://www.youtube.com/watch?v=vC3lQ3ZJE2Y
 */

public class SentdeBot_v1 {

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Sentdebot-v1");

        List<Move> commandQueue = new ArrayList<>();

        while (true) {
            commandQueue.clear();
            networking.updateMap(gameMap);

            List<Ship> myShips = new ArrayList<>(gameMap.getMyPlayer().getShips().values());

            for (final Ship ship : myShips) {
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    continue;
                }

                List<Entity> entitiesByDistance = new ArrayList<>(gameMap.nearbyEntitiesByDistance(ship).values());
                Collections.sort(entitiesByDistance, new Comparator<Entity>() {
                    @Override
                    public int compare(Entity a, Entity b) {
                        return Double.compare(a.getDistanceTo(ship), b.getDistanceTo(ship));
                    }
                });

                List<Planet> closestEmptyPlanets = new ArrayList<>();
                for (Entity entity : entitiesByDistance) {
                    for (Planet pl : new ArrayList<>(gameMap.getAllPlanets().values())) {
                        if(pl.isOwned() || pl.isFull())
                            continue;
                        if (pl.getId() == entity.getId())
                            closestEmptyPlanets.add(pl);
                    }
                }

                List<Ship> closestEnemies = new ArrayList<>();
                for (Entity entity : entitiesByDistance) {
                    for (Ship sh : new ArrayList<>(gameMap.getAllShips())) {
                        if (!new ArrayList<>(gameMap.getMyPlayer().getShips().values()).contains(sh) && sh.getId() == entity.getId())
                            closestEnemies.add(sh);
                    }
                }

                if (closestEmptyPlanets.size() > 0) {
                    Planet targetPlanet = closestEmptyPlanets.get(0);
                    if (ship.canDock(targetPlanet)) {
                        commandQueue.add(new DockMove(ship, targetPlanet));
                    } else {
                        ThrustMove move = Navigation.navigateShipToDock(gameMap, ship, targetPlanet, Constants.MAX_SPEED);
                        if (move != null) {
                            commandQueue.add(move);
                        }
                    }
                } else if (closestEnemies.size() > 0) {
                    Ship targetShip = closestEnemies.get(0);
                    ThrustMove move = Navigation.navigateShipTowardsTarget(gameMap, ship, ship.getClosestPoint(targetShip), Constants.MAX_SPEED, false, Constants.MAX_NAVIGATION_CORRECTIONS, 1);
                    if (move != null) {
                        commandQueue.add(move);
                    }
                }
            }
            Networking.sendMoves(commandQueue);
        }
    }
}
