import CustomCode.MostHaliteOnArea;
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
        Log.log("Successfully created bot! My Player ID is " + game.myId + ". Bot rng seed is " + rngSeed + ".");
        final Player me = game.me;
        final GameMap gameMap = game.gameMap;
        int mapSize = gameMap.height;
        int players = game.players.size();
        Position dropLocation = null;
        boolean onLocation = false;
        int buyShip;
        double returnSpeed = 1.35;
        if (mapSize > 55) {
            buyShip = 300;
        } else {
            buyShip = 250;
        }

        gameMap.shipYard = me.shipyard.position;
        boolean endgame = false;
        MostHaliteOnArea mostHaliteOnArea = new MostHaliteOnArea();
        mostHaliteOnArea.setGameMap(gameMap);

        for (; ; ) {
            int shipCount = 0;
            game.updateFrame();
            if (!me.dropoffs.isEmpty()) {
                for (Dropoff dropoff : me.dropoffs.values()) {
                    dropLocation = dropoff.position;
                    gameMap.dropOff = dropLocation;
                }
            }


            final ArrayList<Command> commandQueue = new ArrayList<>();
            // Spawns first ship.
            int gameTurn = game.turnNumber;
            int gameTimeLeft = Constants.MAX_TURNS - gameTurn;
            if (!endgame && gameTimeLeft == 15) {
                endgame = true;
            }
            if (gameTurn == 25) {
                returnSpeed = 1.3;
            } else if (gameTurn == 50) {
                returnSpeed = 1.2;
            } else if (gameTurn == 100) {
                returnSpeed = 1.1;
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
                if (dropLocation != null && gameMap.calculateDistance(ship.position, shipYardPosition) > gameMap.calculateDistance(ship.position, dropLocation)) {
                    if ((gameMap.calculateDistance(ship.position, dropLocation) + 10) >= gameTimeLeft) {
                        shipsStatus.replace(id, "returning");
                    }
                } else {
                    if ((gameMap.calculateDistance(ship.position, shipYardPosition) + 10) >= gameTimeLeft) {
                        shipsStatus.replace(id, "returning");
                    }
                }

                if (!shipsStatus.containsKey(id)) {
                    if (dropLocation != null && id % 2 == 0) {
                        shipsStatus.put(id, "toDropoff");
                    } else {
                        shipsStatus.put(id, "exploring");
                        Log.log("new ship");
                    }
                }
                if (shipCount == 12 && me.dropoffs.isEmpty() && mapSize >= 48 && players == 2 && !shipsStatus.containsValue("dropoff")) {
                    shipsStatus.replace(id, "dropoff");
                    mostHaliteOnArea.setMostHaliteInArea();
                }
                if (shipsStatus.get(id).equals("dropoff")) {
                    if (ship.position.equals(mostHaliteOnArea.getMostHaliteInArea())) {
                        onLocation = true;
                        if (me.halite > Constants.DROPOFF_COST) {
                            commandQueue.add(ship.makeDropoff());
                            onLocation = false;
                        }
                    } else {
                        commandQueue.add(ship.move(gameMap.naiveNavigate(ship, (mostHaliteOnArea.getMostHaliteInArea()), false)));
                    }
                }
                if (shipsStatus.get(id).equals("toDropoff")) {
                    if (aroundDropof(ship, dropLocation)) {
                        shipsStatus.replace(id, "exploring");
                    } else if (gameMap.at(ship.position).halite < Constants.MAX_HALITE / 10) {
                        commandQueue.add(ship.move(gameMap.naiveNavigate(ship, dropLocation, false)));
                    } else {
                        commandQueue.add(ship.stayStill());
                    }
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
                    if (ship.position.equals(shipYardPosition) || ship.position.equals(dropLocation) && !endgame) {
                        shipsStatus.replace(id, "exploring");
                        // Position bestPos = greatestHaliteAroundShip(ship, gameMap);
                        commandQueue.add(ship.move(gameMap.naiveNavigate(ship, greatestHaliteAroundShip(ship, gameMap), endgame)));
                        Log.log("home");
                    } else {
                        if (dropLocation == null) {
                            commandQueue.add(ship.move(gameMap.naiveNavigate(ship, me.shipyard.position, endgame)));
                        } else {
                            Position home = me.shipyard.position;
                            if (gameMap.calculateDistance(ship.position, me.shipyard.position) > gameMap.calculateDistance(ship.position, dropLocation)) {
                                home = dropLocation;
                            }
                            commandQueue.add(ship.move(gameMap.naiveNavigate(ship, home, endgame)));
                        }
                    }

                } else if (ship.halite >= (Constants.MAX_HALITE / returnSpeed) && !shipsStatus.get(id).equals("dropoff")) {
                    shipsStatus.replace(id, "returning");
                }
            }
            if (game.turnNumber <= buyShip && me.halite >= Constants.SHIP_COST && !gameMap.at(me.shipyard).isOccupied() && shipCount < 30 && gameMap.clearShipyard(me.shipyard) && !onLocation) {
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
            if (gameMap.at(mainPos).halite >= haliteAmount && gameMap.at(mainPos).isEmpty()) {
                haliteAmount = gameMap.at(mainPos).halite;
                bestPos = mainPos;
            }
        }
        if (gameMap.at(bestPos).halite <= gameMap.at(ship).halite && gameMap.at(ship).halite != 0) {
            bestPos = ship.position;
        }
        return bestPos;
    }

    public static boolean aroundDropof(Ship ship, Position dropoffPosition) {
        ArrayList<Position> aroundDrop = dropoffPosition.getSurroundingCardinals();
        for (Position around : aroundDrop) {
            if (around.equals(ship.position)) {
                return true;
            }
        }
        return false;
    }
}