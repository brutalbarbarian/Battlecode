package brutalbarbarian;

import battlecode.common.*;
import brutalbarbarian.utils.Pair;

/**
 * Created by Lu on 11/01/2016.
 */
public class SoldierAI extends RobotAI {
    public SoldierAI(RobotController rc) {
        super(rc);
    }

    @Override
    public void doInitialise() {

    }

    @Override
    protected void doTurn() throws GameActionException {
        int fate = rand.nextInt(1000);

        RobotInfo[] robotsWithinRange = rc.senseNearbyRobots();//sensorRange);

        RobotInfo closestZombie = null, closestLeader = null, closestEnemy = null;
        int closestZombieDistance = Integer.MAX_VALUE;
        int closestLeaderDistance = Integer.MAX_VALUE;
        int closestEnemyDistance = Integer.MAX_VALUE;

        for (RobotInfo robot : robotsWithinRange) {
            int distance = robot.location.distanceSquaredTo(rc.getLocation());
            if(robot.team == team) {
                if (robot.type == RobotType.ARCHON) {
                    if (distance < closestLeaderDistance) {
                        closestLeader = robot;
                        closestLeaderDistance = distance;
                    }
                }
            } else if (robot.team == Team.ZOMBIE && robot.type != RobotType.ZOMBIEDEN) {
                if (distance < closestZombieDistance) {
                    closestZombie = robot;
                    closestZombieDistance = distance;
                }
            } else {
                if (distance < closestEnemyDistance) {
                    closestEnemy = robot;
                    closestEnemyDistance = distance;
                }
            }
        }
        if (closestZombie != null) {
            closestEnemy = closestZombie;
        }

        if (rc.isWeaponReady() && closestEnemy != null && rc.canAttackLocation(closestEnemy.location)) {
            rc.attackLocation(closestEnemy.location);
        } else if (rc.isCoreReady()) {
            if (closestLeader != null) {
                // Follow the leader
                Direction dir = closestLeader.location.directionTo(rc.getLocation()).opposite();
                Pair<Direction, Integer> moveDirection = getClosestValidDirection(dir);
                if (moveDirection.a != Direction.NONE) {
                    rc.move(moveDirection.a);
                }
            } else {
                // Do nothing?
                Direction dir = directions[fate%8];
                Pair<Direction, Integer> moveDirection = getClosestValidDirection(dir);
                if (moveDirection.a != Direction.NONE) {
                    rc.move(moveDirection.a);
                }
            }
        } else {
            // ???
        }

        // if there's a nearby zombie in range, shoot it.
        // Otherwise if there's a nearby enemy, shoot it
        // Otherwise try find the nearest archon. Follow it.
        // Otherwise, pick a direction and move???
    }
}
