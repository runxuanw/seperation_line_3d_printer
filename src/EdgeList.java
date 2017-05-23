import java.util.ArrayList;
import java.util.List;


public class EdgeList {
		int nearobjectID;
		int direct;
		List<Dot> edgeDotList=new ArrayList<Dot>();
		
		EdgeList(int id,int direction){
			nearobjectID=id;
			direct=direction;
		}
}
