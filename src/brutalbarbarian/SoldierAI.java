package brutalbarbarian;

import battlecode.common.*;
import brutalbarbarian.utils.Pair;

/**
 * Created by Lu on 11/01/2016.
 */
public class SoldierAI extends RobotAI {
    static final int DECAY_TIME = 5;

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
            if (message != null && message[0] == CMD_DIRECTION) {
                orderedDirection = Direction.values()[message[1]];
                orderDecay = DECAY_TIME;
                rc.setIndicatorString(0, "Recieved Signal:" + orderedDirection);
            } else if (message != null && message[0] == CMD_HELP) {
                MapLocation location = decodeLocation(message[1]);
                orderedDirection = rc.getLocation().directionTo(location);
                orderDecay = DECAY_TIME;
                rc.setIndicatorString(0, "Recieved Help Call: " + location + ", towards " + orderedDirection);
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
            } else if (robot.team != Team.NEUTRAL) {
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
                if (rc.senseRubble(rc.getLocation().add(moveDirection)) >= GameConstants.RUBBLE_SLOW_THRESH) {
                    rc.setIndicatorString(1, "Attempting to clear rubble at: " + moveDirection);
                    rc.clearRubble(moveDirection);
                    return;
                }

                moveDirection = getClosestValidDirection(moveDirection, (dir)->{return rc.canMove(dir);});
                if (orderedDirection != Direction.NONE) {
                    if (closestLeader != null
                            && closestLeader.location.add(orderedDirection).equals(
                            rc.getLocation().add(moveDirection))) {
                        return; // Don't walk in front of the leader. That's rude.
                    } else if (distanceBetween(orderedDirection, moveDirection) >= 3) {
                        return; // Do nothing. If ordered, stop attempting to backtrack.
                    } else if (closestEnemy != null && rc.getLocation().distanceSquaredTo(closestEnemy.location) <=
                            attackRange) {
                        // if we're ordered to move in that general direction... stop cause we're too close to the enemy
                        Direction dirToEnemy = rc.getLocation().directionTo(closestEnemy.location);
                        if (distanceBetween(dirToEnemy, orderedDirection) < 2) {
                            // attempt to move in the opposite direction
                            moveDirection = getClosestValidDirection(dirToEnemy.opposite(), dir -> {return rc.canMove(dir);});
                            if (distanceBetween(moveDirection, dirToEnemy) < 2) {
                                return; // just stop. don't try to move closer to the enemy.
                            }
//                            return;
                        }
                    }
                }
                if (rc.canMove(moveDirection)) {
                    rc.setIndicatorString(1, "Attempting to move: " + moveDirection);
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
