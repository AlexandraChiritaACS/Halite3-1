import hlt.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class MyBot {
    public static void main(final String[] args) {
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            rngSeed = System.nanoTime();
        }
        final Random rng = new Random(rngSeed);

        Game game = new Game();

        HashMap<Integer, String> shipsStatus = new HashMap<>();

        game.ready("Vandalist");

        Log.log("Successfully created bot! My Player ID is " + game.myId + ". Bot rng seed is " + rngSeed + ".");

        for (; ; ) {
            game.updateFrame();
            final Player me = game.me;
            final GameMap gameMap = game.gameMap;
            final ArrayList<Command> commandQueue = new ArrayList<>();
            // Spawns first ship.

            for (Ship ship : me.ships.values()) {
                // Variables created for readability.
                Integer id = ship.id.id;
                Integer shipX = ship.position.x;
                Integer shipY = ship.position.y;
                Integer shipYardX = me.shipyard.position.x;
                Integer shipYardY = me.shipyard.position.y;

                if (!shipsStatus.containsKey(id)) {
                    shipsStatus.put(id, "exploring");
                }
                if (shipsStatus.get(id)=="exploring") {
                    if (gameMap.at(ship.position).halite < Constants.MAX_HALITE / 10) {
                        Position bestPos = greatestHaliteAroundShip(ship, gameMap);
                        commandQueue.add(ship.move(gameMap.naiveNavigate(ship, bestPos)));
                    } else {
                        commandQueue.add(ship.stayStill());
                    }
                }
                if (shipsStatus.get(id)== "returning") {

                    if (shipX == shipYardX && shipY == shipYardY) {
                        shipsStatus.replace(id, "exploring");
                       // Position bestPos = greatestHaliteAroundShip(ship, gameMap);
                        commandQueue.add(ship.move(gameMap.naiveNavigate(ship,freeSpotfromPort(ship,gameMap))));
                        Log.log("home");
                    } else {
                        commandQueue.add(ship.move(gameMap.naiveNavigate(ship, me.shipyard.position)));
                    }
                } else if (ship.halite >= (Constants.MAX_HALITE / 2)) {
                    shipsStatus.replace(id, "returning");
                }
            }
            if (game.turnNumber <= 200 && me.halite >= Constants.SHIP_COST && !gameMap.at(me.shipyard).isOccupied()) {
                commandQueue.add(me.shipyard.spawn());
            }

            // Logging ship missions (Temporary Debugging)
            for (String s : shipsStatus.values()) {
                Log.log(s);
            }
            game.endTurn(commandQueue);
        }
    }


    /**
     * Returns the position of the cell with the greatest amount of Halite around a ship.
     *
     * @param ship    any ship entity.
     * @param gameMap the current gameMap.
     * @return position of cell with highest Halite.
     */
    public static Position greatestHaliteAroundShip(Ship ship, GameMap gameMap) {
        ArrayList<Position> surroundingPositions = ship.position.getSurroundingCardinals();
        Position bestPos = new Position(0, 0);
        Integer haliteAmount = 0;
        for (Position mainPos : surroundingPositions) {
            if (gameMap.at(mainPos).halite > haliteAmount && gameMap.at(mainPos).isEmpty()) {
                haliteAmount = gameMap.at(mainPos).halite;
                bestPos = mainPos;
            }
        }
        return bestPos;
    }

    public static Position freeSpotfromPort(Ship ship, GameMap gameMap) {
        ArrayList<Position> surroundingPositions = ship.position.getSurroundingCardinals();
        for (Position mainPos : surroundingPositions){
            if ( gameMap.at(mainPos).isEmpty())
                return mainPos;
        }
    }
}