import java.util.ArrayList;
import java.util.List;

	
	public class Object{
		//store all dot in the print object
		List<Dot> dotlist = new ArrayList<Dot>();
		//object's get out direction
		int getoutDirect;
		int objectID;
		int infinite;
		//store all dots near the object, for iterative object calc
		List<Dot> neardotlist=new ArrayList<Dot>();
	}
	