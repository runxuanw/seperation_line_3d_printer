import java.util.*;





	public class Dot{
		int indexx;
		int indexy;
		int indexz;
		float x;
		float y;
		float z;
		float minUnit;
		List<NearDot> nearbylist = new ArrayList<NearDot>();
		List<CornerDot> cornerlist = new ArrayList<CornerDot>();
		//the direction for the dot to get out, either directly or iteratively
		int getoutDirect;
		//whether the dot is on edge of an object, if onedge==2, then it is on the edge of an infinite object
		int onEdge;
		//store for the best case in hill climb
		int maxgetoutDirect;
		int maxonEdge;
		//the id of the object that the dot is in
		int inObjectID;
		//0 means no,1 means yes
		int intempObject;
		//same rule, flag, for not repeat search in bfs, which would be infinite loop
		int inedgeList;
		//same, flag
		int checkedrealList;
		//same, flag, for bfs in filling
		boolean dotchecked;
		//for iterative get out, the object depth limit
		int objectdepth;
		//for iterative get out, the available space limit
		int availdepth;
		
		boolean printed;
		//whether is a connection dot in printing seperation line
		boolean conn;
		float precision;
		int seg_id;
		int num_conn;
		
		//for bfs in process_segment
		boolean searched;
		
		public Dot(float x_,float y_,float z_,float miniUnit,float _precision,int indexx_,int indexy_,int indexz_){
			x= (float) ((Math.round(x_*1000.000))/1000.000);
			y=(float) ((Math.round(y_*1000.000))/1000.000);
			z=(float) ((Math.round(z_*1000.000))/1000.000);
			getoutDirect=0;
			minUnit=miniUnit;
			precision=_precision;
			intempObject=0;
			onEdge=0;
			inObjectID=-1;
			inedgeList=0;
			indexx=indexx_;
			indexy=indexy_;
			indexz=indexz_;
			objectdepth=-1;
			availdepth=-1;
			checkedrealList=0;
			dotchecked=false;
			printed=false;
			conn=false;
			seg_id=-1;
			num_conn=-1;
			searched=false;
			//System.out.println(x+" "+y+" "+z);
		}
		
		public boolean isNearObject(int direct){
			for(int itr=0;itr<nearbylist.size();itr++){
				if(nearbylist.get(itr).direct==direct){
					if(nearbylist.get(itr).neardot.inObjectID!=-1){
						return true;
					}
					else{
						return false;
					}
				}
			}
			//should never hit here
			System.out.println("error: inObjectID failed to find the direction!");
			return false;
		}
		
		//to see whether it is near an object in that direction
		public int nearObjectID(int direct){
			return getneardot(direct).inObjectID;
		}
		
		public boolean isOnEdge(int direct){
			for(int itr=0;itr<nearbylist.size();itr++){
				if(nearbylist.get(itr).direct==direct){
					if(nearbylist.get(itr).neardot.onEdge==1){
						return true;
					}
					else{
						return false;
					}
				}
			}
			System.out.println("error: get out near dot's on edge status!");
			return false;
		}
		
		public void setPrintObject(){
			if(getoutDirect==0){
				getoutDirect=7;
			}
			else{
				if(getoutDirect!=7){
					System.out.println("error: the direction of the dot has been set other than 7!");
					}
			//	else{
			//		System.out.println("already set!");
			//	}
			}
		}
		
		public int getDepth(int direct){
			for(int itr=0;itr<nearbylist.size();itr++){
				if(nearbylist.get(itr).direct==direct){
					return nearbylist.get(itr).maxdepth;
				}
			}
			//should never hit here
			System.out.println("error: depth not found in direction "+direct);
			return -999;
		}
		
		public void setDepth(int newdepth,int direct){
			for(int itr=0;itr<nearbylist.size();itr++){
				if(nearbylist.get(itr).direct==direct){
					nearbylist.get(itr).maxdepth=newdepth;
				}
			}

		}
		
		public boolean hasNearDot(Dot dot) {
			for(NearDot d : nearbylist)
				if(d.neardot == dot) return true;
			for(CornerDot d : cornerlist)
				if(d.cornerdot == dot) return true;
			return false;
		}
		
		public Dot getneardot(int direct){
			for(int itr=0;itr<nearbylist.size();itr++){
				if(nearbylist.get(itr).direct==direct){
					return nearbylist.get(itr).neardot;
				}
			}
			//should never hit here
			System.out.println("error: "+indexx+" "+indexy+" dot not found in direction "+direct);
			Dot nulldot=new Dot(-999,-999,-999,0,0,-1,-1,-1);
			return nulldot;
		}
		
		public void initCornerDot(Dot[][][] metrix,int boundx,int boundy,int boundz){
			if(indexy+1<boundy){
				if(indexz+1<boundz){
					addCornerDot(metrix[indexx][indexy+1][indexz+1]);}
				if(indexz-1>=0){
					addCornerDot(metrix[indexx][indexy+1][indexz-1]);}
			}
			if(indexy-1>=0){
				if(indexz+1<boundz){
					addCornerDot(metrix[indexx][indexy-1][indexz+1]);}
				if(indexz-1>=0){
					addCornerDot(metrix[indexx][indexy-1][indexz-1]);}
			}
			if(indexx+1<boundx){
				if(indexz+1<boundz){
					addCornerDot(metrix[indexx+1][indexy][indexz+1]);}
				if(indexz-1>=0){
					addCornerDot(metrix[indexx+1][indexy][indexz-1]);}
				if(indexy+1<boundy){
					addCornerDot(metrix[indexx+1][indexy+1][indexz]);}
				if(indexy-1>=0){
					addCornerDot(metrix[indexx+1][indexy-1][indexz]);}
			}
			if(indexx-1>=0){
				if(indexz+1<boundz){
					addCornerDot(metrix[indexx-1][indexy][indexz+1]);}
				if(indexz-1>=0){
					addCornerDot(metrix[indexx-1][indexy][indexz-1]);}
				if(indexy+1<boundy){
					addCornerDot(metrix[indexx-1][indexy+1][indexz]);}
				if(indexy-1>=0){
					addCornerDot(metrix[indexx-1][indexy-1][indexz]);}
			}
		}
		
		public void addCornerDot(Dot tempdot){
			//0 means no changes,1 means plus, -1 means minus from center dot
			int a=0;
			int b=0;
			int c=0;
			if(tempdot.x>x){
				a=1;
			}
			if(tempdot.x<x){
				a=-1;
			}
			if(tempdot.y>y){
				b=1;
			}
			if(tempdot.y<y){
				b=-1;
			}
			if(tempdot.z>z){
				c=1;
			}
			if(tempdot.z<z){
				c=-1;
			}
			CornerDot newcorner=new CornerDot(tempdot,a,b,c);
			cornerlist.add(newcorner);
		}
		
		public void addNearDot(Dot tempdot,boolean nearEdge,int direction){
			if(nearEdge==false){
			//need to check whether it is already in the list

				NearDot newnearbydot=new NearDot(tempdot,direction);
				nearbylist.add(newnearbydot);
			}
			else{
				//this tempdot will be a null dot
				NearDot newnearbydot=new NearDot(tempdot,direction);
				newnearbydot.maxdepth=-1;
				nearbylist.add(newnearbydot);
			}
			
		}
		

	}