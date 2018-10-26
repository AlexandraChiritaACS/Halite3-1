import hlt.*;
import hlt.Log;

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
        Log.log((Integer.toString(Constants.MAX_HALITE)));
        Log.log("Successfully created bot! My Player ID is " + game.myId + ". Bot rng seed is " + rngSeed + ".");
        final Player me = game.me;
        final GameMap gameMap = game.gameMap;
        gameMap.shipYard = me.shipyard.position;
        boolean endgame = false;
        for (; ; ) {
            int shipCount = 0;
            game.updateFrame();

            final ArrayList<Command> commandQueue = new ArrayList<>();
            // Spawns first ship.
            int gameTurn = game.turnNumber;
            int gameTimeLeft = Constants.MAX_TURNS - gameTurn;
            if (!endgame && gameTimeLeft == 10) {
                endgame = true;
            }
            for (Ship ship : me.ships.values()) {
                shipCount++;
                // Variables created for readability.
                Integer id = ship.id.id;
                Integer shipX = ship.position.x;
                Integer shipY = ship.position.y;
                Integer shipYardX = me.shipyard.position.x;
                Integer shipYardY = me.shipyard.position.y;
                Shipyard shipyard = me.shipyard;
                Position shipYardPosition = shipyard.position;
                if ((gameMap.calculateDistance(ship.position, shipYardPosition) + 5) >= gameTimeLeft) {
                    shipsStatus.replace(id, "returning");
                }
                if (!shipsStatus.containsKey(id)) {
                    shipsStatus.put(id, "exploring");
                    Log.log("new ship");
                }
                if (shipsStatus.get(id).equals("exploring")) {
                    if (gameMap.at(ship.position).halite < Constants.MAX_HALITE / 10) {
                        Position bestPos = greatestHaliteAroundShip(ship, gameMap);
                        commandQueue.add(ship.move(gameMap.naiveNavigate(ship, bestPos, false)));
                    } else {
                        commandQueue.add(ship.stayStill());
                    }
                }
                if (shipsStatus.get(id).equals("returning")) {
                    Log.log("returning");
                    if (ship.position.equals(shipYardPosition)) {
                        shipsStatus.replace(id, "exploring");
                        // Position bestPos = greatestHaliteAroundShip(ship, gameMap);
                        commandQueue.add(ship.move(gameMap.naiveNavigate(ship, greatestHaliteAroundShip(ship, gameMap), endgame)));
                        Log.log("home");
                    } else {
                        commandQueue.add(ship.move(gameMap.naiveNavigate(ship, me.shipyard.position, endgame)));
                    }
                } else if (ship.halite >= (Constants.MAX_HALITE / 2)) {
                    shipsStatus.replace(id, "returning");
                }
            }
            if (game.turnNumber <= 200 && me.halite >= Constants.SHIP_COST && !gameMap.at(me.shipyard).isOccupied() && shipCount < 30) {
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
        int haliteAmount = 0;
        for (Position mainPos : surroundingPositions) {
            if (gameMap.at(mainPos).halite > haliteAmount) {
                haliteAmount = gameMap.at(mainPos).halite;
                bestPos = mainPos;
            }
        }
        return bestPos;
    }
}