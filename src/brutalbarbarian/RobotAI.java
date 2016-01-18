package brutalbarbarian;

import battlecode.common.*;

import java.util.HashMap;
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

    final RobotType type;
    final int attackRange, sensorRange;
    final Team team;
    final Random rand;

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
                    rc.setIndicatorString(3, e.getMessage());
//                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
                Clock.yield();
            }
        } catch (Exception e) {
//            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void initialise() {
        try {
            initialiseCaches();
            doInitialise();
        } catch (Exception e) {
//            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    interface RobotWeightCheck {
        float getRobotWeight(RobotInfo robot);
    }
    interface DirectionCheck{
        boolean canUseDirection(Direction dir);
    }

    // Utility functions.
    private void initialiseCaches() {
        robotCache = new HashMap<>();
    }

    private void clearCaches() {
        robotCache.clear();
    }

    private RobotInfo[] getRobotCache(int rangeSquared) {
        RobotInfo[] robots = robotCache.getOrDefault(robotCache, null);
        if (robots == null) {
            robots = rc.senseNearbyRobots(rangeSquared);
            robotCache.put(rangeSquared, robots);
        }
        return robots;
    }

    private HashMap<Integer, RobotInfo[]> robotCache;

    public RobotInfo getHighestWeightRobotWithinRange(int rangeSquared, RobotWeightCheck check) {
        RobotInfo[] robots = getRobotCache(rangeSquared);
        RobotInfo bestCantidate = null;
        float bestWeight = 0;
        for (RobotInfo robot : robots) {
            float weight = check.getRobotWeight(robot);
            if (weight > bestWeight) {
                bestWeight = weight;
                bestCantidate = robot;
            }
        }
        return bestCantidate;
    }

    public MapLocation getCentreOfMass(int rangeSquared, RobotWeightCheck check) {
        RobotInfo[] robots = getRobotCache(rangeSquared);
        float xWeight = 0, yWeight = 0, weightSum = 0;
        for (RobotInfo robot : robots) {
            float weight = check.getRobotWeight(robot);
            if (weight > 0) {
                xWeight += weight * robot.location.x;
                yWeight += weight * robot.location.y;
                weightSum += weight;
            }
        }
        if (weightSum > 0) {
            return null;
        } else {
            return new MapLocation(Math.round(xWeight/weightSum), Math.round(yWeight/weightSum));
        }
    }

    public float getSumOfWeights(int rangeSquared, RobotWeightCheck check) {
        RobotInfo[] robots = getRobotCache(rangeSquared);
        float weightSum = 0;
        for (RobotInfo robot : robots) {
            float weight = check.getRobotWeight(robot);
            if (weight > 0) {
                weightSum += weight;
            }
        }
        return weightSum;
    }

    public Direction getDirectionToCentreOfMass(int rangeSquared, RobotWeightCheck check) {
        MapLocation loc = getCentreOfMass(rangeSquared, check);
        if (loc == null) {
            return Direction.NONE;
        } else {
            return rc.getLocation().directionTo(loc);
        }
    }

    public Direction getClosestValidDirection(Direction dir, DirectionCheck locationCheck) {
        if (locationCheck.canUseDirection(dir)) {
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
            if (locationCheck.canUseDirection(testDir)) {
                return testDir;
            }
        }
        return Direction.NONE;    // No solutions found
    }

    public int distanceBetween(Direction dir1, Direction dir2) {
        int dist = Math.abs(dir2.ordinal() - dir1.ordinal());
        if (dist < 4) {
            return dist;
        } else {
            return 8 - dist;
        }
    }
}
