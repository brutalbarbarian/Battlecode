package brutalbarbarian;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {

    static abstract class RobotUnit {
        static final Direction[] directions = {
                Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
                Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

        protected final Random random;

        protected RobotUnit(RobotController rc) {
            random = new Random(rc.getID());
        }

        public void run() {
            firstRun();

            while(true) {
                try {
                    doRun();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                } finally {
                    Clock.yield();
                }
            }
        }

        protected abstract void firstRun();
        protected abstract void doRun();

    }



    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        RobotAI ai = null;
        switch(rc.getType()) {
            case ARCHON:
                ai = new ArchonAI(rc);
                break;
//            case SCOUT:
//                break;
            case SOLDIER:
                ai = new SoldierAI(rc);
                break;
//            case GUARD:
//                break;
//            case VIPER:
//                break;
//            case TURRET:
//                break;
//            case TTM:
//                break;
            default:
                System.out.println("Unknown Robot Type of " + rc.getType());
        }

        if (ai != null) {
            ai.initialise();
            ai.startLoop();
        }
    }
}
