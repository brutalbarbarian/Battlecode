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
        orderedDirection = Direction.NONE;
        orderDecay = 0;
    }

    Direction orderedDirection;
    int orderDecay;

    @Override
    protected void doTurn() throws GameActionException {
        int fate = rand.nextInt(1000);

        RobotInfo[] robotsWithinRange = rc.senseNearbyRobots();//sensorRange);

        orderDecay --;
        if (orderedDirection != Direction.NONE && orderDecay == 0) {
            orderedDirection = Direction.NONE;
        }
        Signal[] signals = rc.emptySignalQueue();
        if (signals.length > 0) {
            int closestSignalLength = Integer.MAX_VALUE;
            Signal closestSignal = null;
            for (Signal signal : signals) {
                if (signal.getTeam().isPlayer()) {
                    int distance = signal.getLocation().distanceSquaredTo(rc.getLocation());
                    if (distance < closestSignalLength) {
                        closestSignalLength = distance;
                        closestSignal = signal;
                    }
                }
            }


            int[] message = closestSignal.getMessage();
            if (closestSignal.getTeam().isPlayer() && message != null && message[0] == CMD_DIRECTION) {
                orderedDirection = Direction.values()[message[1]];
                orderDecay = 6; // 8 turns before we ignore this signal.
                rc.setIndicatorString(0, "Recieved Signal:" + orderedDirection);
            }
        }

        RobotInfo closestZombie = null, closestLeader = null, closestEnemy = null;
        int closestZombieDistance = Integer.MAX_VALUE;
        int closestLeaderDistance = Integer.MAX_VALUE;
        int closestEnemyDistance = Integer.MAX_VALUE;
        int friendlyCount = 0;
        int x = 0, y = 0;

        for (RobotInfo robot : robotsWithinRange) {
            int distance = robot.location.distanceSquaredTo(rc.getLocation());
            if(robot.team == team) {
                if (robot.type == RobotType.ARCHON) {
                    if (distance < closestLeaderDistance) {
                        closestLeader = robot;
                        closestLeaderDistance = distance;
                    }
                }
                friendlyCount++;
                x += robot.location.x;
                y += robot.location.y;
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
            Direction moveDirection;
            if (orderedDirection != Direction.NONE) {
                if (rc.senseRubble(rc.getLocation().add(orderedDirection)) >= GameConstants.RUBBLE_SLOW_THRESH) {
                    rc.clearRubble(orderedDirection);
                    return;
                }

                moveDirection = orderedDirection;
            } else if (closestLeader != null) {
                // Follow the leader
                moveDirection = closestLeader.location.directionTo(rc.getLocation()).opposite();
            } else {
                moveDirection = directions[fate%8];
                if (friendlyCount > 0) {
                    MapLocation loc = new MapLocation(x/friendlyCount, y/friendlyCount);
                    if (!loc.equals(rc.getLocation())) {
                        moveDirection = rc.getLocation().directionTo(loc);
                    }
                }
            }
            if (moveDirection != Direction.NONE) {
                moveDirection = getClosestValidDirection(moveDirection);
                if (orderedDirection != Direction.NONE && distanceBetween(orderedDirection, moveDirection) >= 3) {
                    return; // Do nothing. If ordered, stop attempting to backtrack.
                }

                if (rc.senseRubble(rc.getLocation().add(moveDirection)) >= GameConstants.RUBBLE_SLOW_THRESH) {
                    rc.clearRubble(moveDirection);
                } else if (rc.canMove(moveDirection)) {
                    rc.move(moveDirection);
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
