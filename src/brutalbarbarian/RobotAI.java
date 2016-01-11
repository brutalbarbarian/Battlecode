package brutalbarbarian;

import battlecode.common.*;
import brutalbarbarian.utils.Pair;

import java.util.Random;

/**
 * Created by Lu on 11/01/2016.
 */
public abstract class RobotAI {
    protected static final Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    protected static final RobotType[] robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET};

    RobotType type;
    protected int attackRange, sensorRange;
    Team team;
    Random rand;

    protected RobotController rc;

    public RobotAI (RobotController rc) {
        this.rc = rc;
        this.type = rc.getType();
        attackRange = type.attackRadiusSquared;
        sensorRange = type.sensorRadiusSquared;
        team = rc.getTeam();

        rand = new Random(rc.getID());
    }

    protected abstract void doInitialise();

    protected abstract void doTurn() throws GameActionException;

    public void startLoop() {
        try {
            while (true) {
                try {
                    doTurn();
                } catch (GameActionException e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                Clock.yield();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void initialise() {
        try {
            doInitialise();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public Pair<Direction, Integer> getClosestValidDirection(Direction dir) {
        if (rc.canMove(dir)) {
            return new Pair<>(dir, 0);
        }

        Direction dirLeft = dir;
        Direction dirRight = dir;
        for (int i = 1; i < 8; i++) {
            Direction testDir;
            if (i % 2 == 1) {
                testDir = (dirLeft = dirLeft.rotateLeft());
            } else {
                testDir = (dirRight = dirRight.rotateRight());
            }
            if (rc.canMove(testDir)) {
                return new Pair<>(testDir, (i + 1) / 2);
            }
        }
        return new Pair<>(Direction.NONE, -1);    // No solutions found
    }
}
