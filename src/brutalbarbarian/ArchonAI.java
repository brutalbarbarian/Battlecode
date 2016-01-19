package brutalbarbarian;

import battlecode.common.*;
import battlecode.world.signal.IndicatorStringSignal;
import brutalbarbarian.utils.Pair;

/**
 * Created by Lu on 11/01/2016.
 */
public class ArchonAI extends RobotAI {
    Direction prefDirection;
    int movesignaldelay;
    int stuckTurns;

    public ArchonAI(RobotController rc) {
        super(rc);
    }

    @Override
    public void doInitialise() {
        int fate = rand.nextInt(1000);

        prefDirection = directions[fate % 8];
        movesignaldelay = 4;
        stuckTurns = 0;
    }

    @Override
    protected void doTurn() throws GameActionException {
        movesignaldelay--;
        int fate = rand.nextInt(1000);

        RobotInfo[] robotsWithinRange = rc.senseNearbyRobots();//sensorRange);
        Signal[] signals = rc.emptySignalQueue();

        int x = 0, y = 0;
        int closestEnemyDistance = Integer.MAX_VALUE;
        int friendlyCount = 0, enemyCount = 0;
        RobotInfo closestHurtFriendly = null, closestEnemy = null;
        int closestHurtFriendlyDistance = Integer.MAX_VALUE;
        for (RobotInfo robot : robotsWithinRange) {
            int distance = robot.location.distanceSquaredTo(rc.getLocation());
            if (robot.team == team) {
                if (robot.health < robot.maxHealth && robot.type != RobotType.ARCHON) {
                    if (distance < closestHurtFriendlyDistance) {
                        closestHurtFriendly = robot;
                        closestHurtFriendlyDistance = distance;
                    }
                }
                friendlyCount++;
            } else if (robot.team != Team.NEUTRAL) {//(robot.type != RobotType.ZOMBIEDEN) {
                if (distance < closestEnemyDistance || (closestEnemy != null && closestEnemy.type == RobotType.ZOMBIEDEN)) {
                    closestEnemy = robot;
                    closestEnemyDistance = distance;
                }
                x += robot.location.x;
                y += robot.location.y;
                enemyCount++;
            }
        }



        if (rc.isWeaponReady() && closestHurtFriendly != null) {
            rc.repair(closestHurtFriendly.location);
//            return;
        }

        if (rc.isCoreReady()) {
            Direction moveDirection;
            boolean headingForResources = false;
            boolean foundEnemy = false;
            // If there are any enemies in range. Move in opposite direction of any enemy
            if (enemyCount > friendlyCount) {
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
                moveDirection = getClosestValidDirection(proposedDirection, (dir)->{return rc.canMove(dir);});

                // Well. We probably don't want to keep going in whatever our previous direction, since there's
                // enemy's nearby.
                if (moveDirection != Direction.NONE) {
                    prefDirection = moveDirection;
                }

                rc.setIndicatorString(0, "Running away in direction: " + moveDirection);
            } else {
                Direction proposedDirection = prefDirection;
                boolean heardHelp = false;
                for (Signal sig : signals) {
                    if (sig.getTeam().isPlayer()) {
                        int[] message = sig.getMessage();
                        if (message != null && message[0] == CMD_HELP) {
                            // recieved help call. go towards help
                            MapLocation targetLocation = decodeLocation(message[1]);
                            if (!targetLocation.equals(rc.getLocation())) {
                                rc.setIndicatorString(0, "Heard help call for " + targetLocation);

                                prefDirection = rc.getLocation().directionTo(targetLocation);
                                proposedDirection = prefDirection;
                                heardHelp = true;
                                break;
                            }
                        }
                    }
                }

                if (!heardHelp) {
                    if (rc.hasBuildRequirements(RobotType.SOLDIER)) {
                        // we'll use the pref direction to also try build a soldier there
                        Direction buildDirection = getClosestValidDirection(prefDirection.opposite(), (dir) -> {
                            return rc.canBuild(dir, RobotType.SOLDIER);
                        });
                        if (rc.canBuild(buildDirection, RobotType.SOLDIER)) {
                            rc.build(buildDirection, RobotType.SOLDIER);
                            return;
                        }
                    }
                    if (enemyCount > 0) {
                        MapLocation loc = new MapLocation(x / enemyCount, y / enemyCount);
                        if (!loc.equals(rc.getLocation())) {
                            proposedDirection = rc.getLocation().directionTo(loc);

                            rc.setIndicatorString(0, "Enemy Found at: " + proposedDirection);

                            if (proposedDirection != Direction.NONE) {
                                prefDirection = proposedDirection;
                                foundEnemy = true;
                            }

                            // Let's tell everyone there's an enemy nearby.
                            if (movesignaldelay <= 0 && proposedDirection != Direction.NONE) {
                                rc.broadcastMessageSignal(CMD_DIRECTION, proposedDirection.ordinal(), sensorRange * 2);
                                movesignaldelay = 4;
                                return;
                            } else {
                                // if target unit actually has a meaningful attack, and we're in their range...
                                // we probably want to move back
                                if (closestEnemy.attackPower > 0 && closestEnemy.type.attackRadiusSquared <= closestEnemyDistance) {
                                    // How about we try move back a step mhmm?
                                    proposedDirection = proposedDirection.opposite();
                                } else if (enemyCount > friendlyCount / 2 || enemyCount > 4) {
                                    // if there are at least half as many enemies in vision as friendlies...
                                    // or there are at least 4 enemies
                                    // SHOUT
                                    rc.setIndicatorString(0, "Crying for Help for location: " + loc);
                                    proposedDirection = Direction.NONE;
                                    rc.broadcastMessageSignal(CMD_HELP, encodeLocation(loc), sensorRange * 20);
                                    return;
                                }
                            }
                        }
                    } else {
                        // Let's check for nearby resources
                        MapLocation[] partLocations = rc.sensePartLocations(sensorRange);
                        MapLocation closestPartLocation = null;
                        int closestPartLocationDistance = Integer.MAX_VALUE;
                        for (MapLocation location : partLocations) {
                            int distance = location.distanceSquaredTo(rc.getLocation());
                            if (distance < closestPartLocationDistance) {
                                closestPartLocationDistance = distance;
                                closestPartLocation = location;
                            }
                        }
                        if (closestPartLocation != null) {
                            proposedDirection = rc.getLocation().directionTo(closestPartLocation);
                            if (proposedDirection != Direction.NONE) {
                                rc.setIndicatorString(0, "Resource Found at: " + proposedDirection);

                                prefDirection = proposedDirection;  // We see resource. Let's go there!
                                headingForResources = true;
                            }
                        }
                    }
                }

                if (rc.senseRubble(rc.getLocation().add(proposedDirection)) >= GameConstants.RUBBLE_SLOW_THRESH) {
                    rc.setIndicatorString(1, "Clear Rubble: " + proposedDirection);

                    rc.clearRubble(proposedDirection);
                    moveDirection = Direction.NONE;
                } else {
                    moveDirection = getClosestValidDirection(proposedDirection, (dir)->{return rc.canMove(dir);});

                    if (moveDirection == Direction.NONE) {
                        // Welp. We're stuck.
                        stuckTurns++;
                        if (stuckTurns >= 4) {
                            stuckTurns = 0;
                            prefDirection = prefDirection.opposite();

                            rc.setIndicatorString(1, "Stuck. Changing Direction to: " + prefDirection);
                        }
                    } else if (moveDirection != Direction.NONE && distanceBetween(moveDirection, prefDirection) >= 2) {
                        // If we're walking towards the edge of the map, then sure, let's change direction.
                        // Otherwise, don't.


                        if (headingForResources || foundEnemy) {
                            moveDirection = Direction.NONE; // We really want that resource...
                            // do nothing
                        } else if (fate % 6 < 1) {    // 1 in 6
                            prefDirection = moveDirection;
                            // Always send a new signal out whenever we change pref direction
                            rc.broadcastMessageSignal(CMD_DIRECTION, prefDirection.ordinal(), sensorRange * 2);
                            movesignaldelay = 4;
                            return;
                        } else if (fate % 6 < 2) {  // 1 in 6
                            moveDirection = Direction.NONE; // Don't attempt to move there... but we still want to broadcast pref
                        }
                    }
                }

                if (movesignaldelay <= 0) {
                    rc.setIndicatorString(1, "Command Move: " + prefDirection);

                    rc.broadcastMessageSignal(CMD_DIRECTION, prefDirection.ordinal(), sensorRange * 2);
                    movesignaldelay = 4;
                    return;
                }
            }

            // Try figure find a closest valid location to move.
            if (moveDirection != Direction.NONE) {
                stuckTurns = 0; // reset stuck turns, since we've happily moved somewhere
                rc.move(moveDirection);
            } else {
                // Do what??
            }
        }
    }
}
