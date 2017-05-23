//maxdepth:
// -1 infinite
// -2 unsure
// x(>=0) x depth to be hit a object or get out

	
	public class NearDot{
		//this class only used in dot
		//3d array can find near by dot as well, but use a direct link should be faster
		//however, when the calc is n^2+n, and this improves n to c, it does not help much, whatever...
		int direct;
		Dot neardot;
		int maxdepth;//max get out depth in this direction after this neardot
		
		
		public NearDot(Dot tempneardot,int nearbyDirect){
			direct=nearbyDirect;
			neardot=tempneardot;
			maxdepth=0;
		}
	}