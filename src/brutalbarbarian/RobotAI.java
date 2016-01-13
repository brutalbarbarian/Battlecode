package brutalbarbarian;

import battlecode.common.*;
import brutalbarbarian.utils.Pair;

import java.util.Random;

/**
 * Created by Lu on 11/01/2016.
 */
public abstract class RobotAI {
    protected static final int CMD_TARGET = 0;
    protected static final int CMD_DIRECTION = 1;
    protected static final int CMD_RETREAT = 2;

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

    public Direction getClosestValidDirection(Direction dir) {
        if (rc.canMove(dir)) {
            return dir;
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
                return testDir;
            }
        }
        return Direction.NONE;    // No solutions found
    }

    public int distanceBetween(Direction dir1, Direction dir2) {
        rc.setIndicatorString(0, dir1 + ":" + dir1.ordinal() + "," + dir2 + ":" + dir2.ordinal());
        int dist = Math.abs(dir2.ordinal() - dir1.ordinal());
        if (dist < 4) {
            return dist;
        } else {
            return 8 - dist;
        }
    }
}
