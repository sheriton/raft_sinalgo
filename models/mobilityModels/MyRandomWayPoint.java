package projects.raft.models.mobilityModels;

import java.util.Random;

import sinalgo.configuration.Configuration;
import sinalgo.configuration.CorruptConfigurationEntryException;
import sinalgo.io.mapIO.Map;
import sinalgo.models.MobilityModel;
import sinalgo.nodes.Node;
import sinalgo.nodes.Position;
import sinalgo.runtime.Main;
import sinalgo.tools.Tools;
import sinalgo.tools.statistics.Distribution;

public class MyRandomWayPoint extends MobilityModel {

	// we assume that these distributions are the same for all nodes
		protected static Distribution speedDistribution;
		protected static Distribution waitingTimeDistribution;
		protected static Distribution moveDistanceDistribution;

		private static boolean initialized = false; // a flag set to true after initialization of the static vars of this class has been done.
		protected static Random random = Distribution.getRandom(); // a random generator of the framework 
		
		protected Position nextDestination = new Position(); // The point where this node is moving to
		protected Position moveVector = new Position(); // The vector that is added in each step to the current position of this node
		protected Position currentPosition = null; // the current position, to detect if the node has been moved by other means than this mobility model between successive calls to getNextPos()
		protected int remaining_hops = 0; // the remaining hops until a new path has to be determined
		protected int remaining_waitingTime = 0;
		
		/* (non-Javadoc)
		 * @see mobilityModels.MobilityModelInterface#getNextPos(nodes.Node)
		 */
		public Position getNextPos(Node n) {
			// restart a new move to a new destination if the node was moved by another means than this mobility model
			if(currentPosition != null) 
			{
				if(!currentPosition.equals(n.getPosition())) {
					remaining_waitingTime = 0;
					remaining_hops = 0;
				}
			} 
			else 
			{
				currentPosition = new Position(0, 0, 0);
			}
			
			Position nextPosition = new Position();
			
			// execute the waiting loop
			if(remaining_waitingTime > 0) 
			{
				remaining_waitingTime --;
				return n.getPosition();
			}

			if(remaining_hops == 0) 
			{
				// determine the speed at which this node moves
				double speed = Math.abs(speedDistribution.nextSample()); // units per round

				// determine the next point where this node moves to
				nextDestination = getNextWayPoint(n.getPosition());
				
				// determine the number of rounds needed to reach the target
				double dist = nextDestination.distanceTo(n.getPosition());
				double rounds = dist / speed;
				remaining_hops = (int) Math.ceil(rounds);
				// determine the moveVector which is added in each round to the position of this node
				double dx = nextDestination.xCoord - n.getPosition().xCoord;
				double dy = nextDestination.yCoord - n.getPosition().yCoord;
				double dz = nextDestination.zCoord - n.getPosition().zCoord;
				moveVector.xCoord = dx / rounds;
				moveVector.yCoord = dy / rounds;
				moveVector.zCoord = dz / rounds;
			}

			
			if(remaining_hops <= 1) // don't add the moveVector, as this may move over the destination.
			{ 
				nextPosition.xCoord = nextDestination.xCoord;
				nextPosition.yCoord = nextDestination.yCoord;
				nextPosition.zCoord = nextDestination.zCoord;
				// set the next waiting time that executes after this mobility phase
				remaining_waitingTime = (int) Math.ceil(waitingTimeDistribution.nextSample());
				remaining_hops = 0;
			} 
			else 
			{
				double newx = n.getPosition().xCoord + moveVector.xCoord; 
				double newy = n.getPosition().yCoord + moveVector.yCoord; 
				double newz = n.getPosition().zCoord + moveVector.zCoord; 
				nextPosition.xCoord = newx;
				nextPosition.yCoord = newy;
				nextPosition.zCoord = newz;
				remaining_hops --;
			}
			
			if(Configuration.useMap) 
			{
				boolean inLake = false;
				Map map = Tools.getBackgroundMap();
				inLake = !map.isWhite(n.getPosition());  //we are already standing in the lake
				
				if(inLake){
					Main.fatalError("A node is standing in a lake. Cannot find a step outside.");
				}
				
				/* vai cair num 'lago'  */
				if (!map.isWhite(nextPosition)) 
				{
					currentPosition = n.getPosition();
					remaining_hops = 0;
					nextPosition = getNextPos(n);
				}
			}
			
			currentPosition.assign(nextPosition);
			return nextPosition;
		}
		
		/**
		 * Determines the next waypoint where this node moves after having waited.
		 * The position is expected to be within the deployment area.
		 * @return the next waypoint where this node moves after having waited. 
		 */
		protected Position getNextWayPoint(Position currentPosition) {
			double distance = moveDistanceDistribution.nextSample(); 		// next distance to move
			double angle = (1d - random.nextDouble()) * 2 * Math.PI; 	// next angle to move (in radians)

			/* calculates dx and dy, which are the components to be added to the actual coordinates to find the next position at distance d from the current position */
			double dx = distance * Math.cos(angle);
			double dy = distance * Math.sin(angle);
			
			/* calculates the new coordinates after calculating */
			double nextX = currentPosition.xCoord + dx;
			double nextY = currentPosition.yCoord + dy;		
			double nextZ = 0;
			
			if (Main.getRuntime().getTransformator().getNumberOfDimensions() == 3) {
				/* not implemented */
				/* we need to decompose the distance in R^3, then we can define nextZ coordinate */
			}
			
			return new Position(nextX, nextY, nextZ);
		}
		
		/**
		 * Creates a new random way point object, and reads the speed distribution and 
		 * waiting time distribution configuration from the XML config file.
		 * @throws CorruptConfigurationEntryException When a needed configuration entry is missing.
		 */
		public MyRandomWayPoint() throws CorruptConfigurationEntryException {
			if(!initialized) {
				speedDistribution = Distribution.getDistributionFromConfigFile("MyRandomWayPoint/Speed");
				waitingTimeDistribution = Distribution.getDistributionFromConfigFile("MyRandomWayPoint/WaitingTime");
				moveDistanceDistribution = Distribution.getDistributionFromConfigFile("MyRandomWayPoint/MoveDistance");
				initialized = true;
			}
		}

}
