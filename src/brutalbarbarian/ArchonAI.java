package brutalbarbarian;

import battlecode.common.*;
import brutalbarbarian.utils.Pair;

/**
 * Created by Lu on 11/01/2016.
 */
public class ArchonAI extends RobotAI {
    Direction prefDirection;

    public ArchonAI(RobotController rc) {
        super(rc);
    }

    @Override
    public void doInitialise() {
        int fate = rand.nextInt(1000);

        prefDirection = directions[fate % 8];
    }

    @Override
    protected void doTurn() throws GameActionException {
        int fate = rand.nextInt(1000);

        if (rc.isCoreReady()) {
            RobotInfo[] robotsWithinRange = rc.senseNearbyRobots();//sensorRange);

            int x = 0, y = 0;
            int enemyCount = 0;
            int friendlyCount = 0;
//            RobotInfo closestEnemy = null;
//            int closestEnemyDistance = Integer.MAX_VALUE;
            for (RobotInfo robot : robotsWithinRange) {
                if (robot.team == team) {
                    friendlyCount++;
                } else if (robot.type != RobotType.ZOMBIEDEN) {
                    int distance = robot.location.distanceSquaredTo(rc.getLocation());
//                    if (distance < closestEnemyDistance) {
//                        closestEnemy = robot;
//                        closestEnemyDistance = distance;
//                    }
                    x += robot.location.x;
                    y += robot.location.y;
                    enemyCount++;
                }
            }

            Pair<Direction, Integer> moveDirection;
            // If there are any enemies in range. Move in opposite direction of any enemy
            if (enemyCount > friendlyCount) {
                // Too cowardly right now..

                // Move in different direction?
                MapLocation loc = new MapLocation(x/enemyCount, y/enemyCount);
                Direction proposedDirection;
                if (loc.equals(rc.getLocation())) {
                    // Average of all enemies have surrounded us. Move in a random direction???
                    proposedDirection = directions[fate%8];
                } else {
                    // Move in the opposite direction of the average of all enemies
                    proposedDirection = loc.directionTo(rc.getLocation());
                }

                // Randomize the direction slightly
                if (fate % 3 == 1) {
                    proposedDirection = proposedDirection.rotateLeft();
                } else if (fate % 3 == 2) {
                    proposedDirection = proposedDirection.rotateRight();
                }
                moveDirection = getClosestValidDirection(proposedDirection);

                // Well. We probably don't want to keep going in whatever our previous direction, since there's
                // enemy's nearby.
                if (moveDirection.a != Direction.NONE) {
                    prefDirection = moveDirection.a;
                }
            } else {
                // Probably should check for nearby resources. Oh well.
                moveDirection = getClosestValidDirection(prefDirection);
                if (moveDirection.b >= 2) {
                    prefDirection = moveDirection.a;
                }

                // we'll use the pref direction to also try build a soldier there
                if (rc.hasBuildRequirements(RobotType.SOLDIER) && rc.canBuild(moveDirection.a, RobotType.SOLDIER)) {
                    rc.build(moveDirection.a, RobotType.SOLDIER);
                    return;
                }
            }

            // Try figure find a closest valid location to move.
            if (moveDirection.a != Direction.NONE) {
                rc.move(moveDirection.a);
            } else {
                // Do what??
            }
        }
    }
}
