package CustomCode;

import hlt.GameMap;
import hlt.Log;
import hlt.Position;


public class MostHaliteOnArea {
    public GameMap gameMap;
    public Position mostHaliteInArea;
    public int mHeight;
    public int mWidth;

    public Position getMostHaliteInArea() {
        return mostHaliteInArea;
    }

    public void setGameMap(GameMap gameMap) {
        this.gameMap = gameMap;
        this.mHeight = gameMap.height;
        this.mWidth = gameMap.width;

    }

    public void setMostHaliteInArea() {
        Position home = gameMap.shipYard;
        Log.log(Integer.toString(home.x));
        int blockSize;
        int maxBlockSize = 32;
        int minBlockSize = 8;
        int startX = 0;
        if (home.x > 32) {
            startX = 32;
            minBlockSize = 32;
            maxBlockSize = 64;
        }
        int newblockHalite = 0;


        int x;
        int maxHaliteBlok = 0;
        Position bestPosition = null;

        for (blockSize = minBlockSize; blockSize <= maxBlockSize; blockSize += 8) {
            for (int y = 0; y < mHeight; y++) {
                for (x = startX; x < maxBlockSize; x++) {
                    int halite = gameMap.at(new Position(x, y)).halite;
                    newblockHalite += halite;
                }
                if (y != 0 && (y + 1) % 8 == 0) {
                    if (maxHaliteBlok < newblockHalite) {
                        Position localBest = new Position(x - 4, y - 4);
                        if (gameMap.calculateDistance(home, localBest) > 8) {
                            maxHaliteBlok = newblockHalite;
                            bestPosition = localBest;
                        }
                    }
                    newblockHalite = 0;
                }
            }
            startX += 8;
        }
        Log.log(bestPosition.x + "," + bestPosition.y);
        this.mostHaliteInArea = bestPosition;
    }
}