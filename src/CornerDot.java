/*
 * direction:
   0 unsure
   1 downward  y++
   2 upward y--
   3 right    x++
   4 left     x--
   5 up       z++
   6 down     z--
   7 in print object's contour , cannot get out
   8 mark as in print object. because if mark as 7, it will effect later dots' marks.

*/

//each dot has 12 corner dot
//the corner dot is the dot at the corner of the center dot in same dimension (not space corner)
//this design is needed for not near dot to get out

//use when it is sure one dot is on edge
public class CornerDot {
		Dot cornerdot;
		int directx;
		int directy;
		int directz;
		//0 means no changes,1 means plus, -1 means minus from center dot
		CornerDot(Dot tempdot,int a,int b,int c){
			cornerdot=tempdot;
			directx=a;
			directy=b;
			directz=c;
		}
		
}
