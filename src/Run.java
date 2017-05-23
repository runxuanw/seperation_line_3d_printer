import java.util.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.State;

//物体顶/底边缘未封死，需把最上层物体算作边缘填充粉 

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

//need to sort the edgelist and start with the largest in analyse
public class Run {
	// for print edge
	//segment is a collection of contiguous dots for seperation line in one layer
	//there can be more than one segment in one layer
	static List<Dot> Segment = new ArrayList<Dot>();
	//the final sorted sub-segment to be printed
	static List<Dot> printSegment = new ArrayList<Dot>();
	//same as precision, redundent
	static float precisionValue=0;
	//for estimate printing time, current data for simulation
	static float lifttime=1;//in second
	static float movespeed=3;//in inch/sec
	//the auto adjust seems has minor mis-calc, add maual adjustx and adjusty to fix it, for printing purpose
	//not a good way to fix though
	static float manualadjustx=(float) 0.012;
	static float manualadjusty=(float) 0.012;
	
	static float boundx, boundy;
	static int countx, county, countz;

	public static void main(String args[]) {

		String filename = "";

		Scanner scan = new Scanner(System.in);
		if(scan.hasNextLine()) filename = scan.nextLine();

		ReadTXT readfile = new ReadTXT(filename);

		// setting, only infinite get out, for simple object
		boolean simplecalc = true;
		//calc only the largest block, only once, simple calc must be true
		boolean calconce = false;
		boolean rapidChange = false;
		// specific about boundary
		// need to get data from file
		// out of memory if to 140^3
		// runtime n^3 (spactial calc); may use cuboid instead of cube to reduce the affect of precision from n^3 to n

		// for adjust the points if there exists a negative cooredinate
		float adjustionx = 0;
		float adjustiony = 0;

		//print precision set for x and y, in inch
		//mm to inch
		//0.3 to 0.01181
		//0.2 to 0.00787
		//0.15 to 0.00590
		float precision=(float) 0.01181;
		
		//get layer height (inch)
		float layerHeight = readfile.findLayerHeight();
		//minUnit is precision in z
		float minUnit = layerHeight;
		float initHeight=readfile.findInitHeight();
		precisionValue=precision;
		
		
		// currently optimal to the object's x,y length
		boundx = (float) (readfile.findXLength()*1.02);
		boundy = (float) (readfile.findYLength()*1.02);
		float centerx = boundx / 2;
		float centery = boundy / 2;
		
		// add mini amount to precision, to get off the glitch of float
		// calculation
		float precisionapplycontour = (float) (precision * 0.9999);

		countx = (int) (boundx / precision)+10;
		county = (int) (boundy / precision)+10;
		countz = readfile.findtotalLayer();
		System.out.println("total layer: " + countz);
		System.out.println("count: " + countx + " " + county + " " + countz + "\n");

		// generate Metrix
		long startconstructTime = System.nanoTime();
		Dot[][][] dotMatrix = new Dot[countx][county][countz];

		float z = 0;// the 0 will be start edge value
		float y = 0;
		float x = 0;
		for (int k = 0; k < countz; k++) {
			z += minUnit;
			y = 0;
			for (int j = 0; j < county; j++) {
				y += precision;
				x = 0;
				for (int i = 0; i < countx; i++) {
					x += precision;
					Dot tempdot = new Dot(x, y, z, minUnit,precision, i, j, k);
					dotMatrix[i][j][k] = tempdot;
				}
			}
		}
		System.out.println("Finish constructing");

		// connect each dot to its neighbor
		Dot nulldot = new Dot(-999, -999, -999, minUnit,precision, -1, -1, -1);
		for (int k = 0; k < countz; k++) {
			for (int j = 0; j < county; j++) {
				for (int i = 0; i < countx; i++) {
					if (i - 1 >= 0)
						dotMatrix[i][j][k]
								.addNearDot(dotMatrix[i - 1][j][k], false,4);
					if (j - 1 >= 0)
						dotMatrix[i][j][k]
								.addNearDot(dotMatrix[i][j - 1][k], false,2);
					if (k - 1 >= 0)
						dotMatrix[i][j][k]
								.addNearDot(dotMatrix[i][j][k - 1], false,6);
					if (i + 1 < countx)
						dotMatrix[i][j][k]
								.addNearDot(dotMatrix[i + 1][j][k], false,3);
					if (j + 1 < county)
						dotMatrix[i][j][k]
								.addNearDot(dotMatrix[i][j + 1][k], false,1);
					if (k + 1 < countz)
						dotMatrix[i][j][k]
								.addNearDot(dotMatrix[i][j][k + 1], false,5);
					if (i - 1 < 0)
						dotMatrix[i][j][k].addNearDot(nulldot, true, 4);
					if (j - 1 < 0)
						dotMatrix[i][j][k].addNearDot(nulldot, true, 2);
					if (k - 1 < 0)
						dotMatrix[i][j][k].addNearDot(nulldot, true, 6);
					if (i + 1 >= countx)
						dotMatrix[i][j][k].addNearDot(nulldot, true, 3);
					if (j + 1 >= county)
						dotMatrix[i][j][k].addNearDot(nulldot, true, 1);
					if (k + 1 >= countz)
						dotMatrix[i][j][k].addNearDot(nulldot, true, 5);
				}
			}
		}
		
		//check the connection is correct
		for (int k = 0; k < countz; k++) {
			for (int j = 0; j < county; j++) {
				for (int i = 0; i < countx; i++) {
					if(dotMatrix[i][j][k].nearbylist.size()!=6){
					System.out.println("There is a connection error, the dot does not have 6 near dots: "+i+" "+j+" "+k+" "+dotMatrix[i][j][k].nearbylist.size());
					}
				}
			}
		}
				
		System.out.println("Finish connecting");

		// get point in each layer and convert them into the Metrix
		// the gap of each point needs to be filled
		// not a good place to read contour
		// if the max min has negative coordinates, add the absolute value of
		// min number to each dot to calibrate the txt!!!
		File file = new File(filename);
		float xmax = -9999;
		float xmin = -9999;
		float ymax = -9999;
		float ymin = -9999;
		boolean calccenter = false;
		try {
			Scanner sc = new Scanner(file);
			String line = sc.nextLine();
			int currentlayer = 0;
			// first, locate the layer
			while (sc.hasNextLine()) {

				// get xmin and ymin, and set adjustion of all points if
				// necessary
				if (line.contains("XMax XMin YMax YMin")) {
					xmax=sc.nextFloat();
					xmin=sc.nextFloat();
					ymax=sc.nextFloat();
					ymin=sc.nextFloat();
				}
				if (ymax != -9999 && ymin != -9999 && xmax != -9999
						&& xmin != -9999 && calccenter == false ) {
					adjustionx = centerx - (xmax + xmin) / 2;
					adjustiony = centery - (ymax + ymin) / 2;
					System.out.println("x adjust: "+adjustionx);
					System.out.println("y adjust: "+adjustiony);
					calccenter = true;
				}
				
				if (line.contains("Layer:")) {
					// start from txt layer 1->matrix layer 0, get (layerheight-initheight)/perlayerheight
					float currentlayerHeight=sc.nextFloat();
					currentlayer = (int) Math.round(((currentlayerHeight/25.4)-initHeight)/layerHeight);
					// System.out.println("new currentlayer:"+currentlayer);
					int contourdots=sc.nextInt();
				// then locate the contour and get points into metrix
					float angel = -1;
					float pointx = -1;
					float pointy = -1;
					float lastPointx = -1;
					float lastPointy = -1;
					float printangel=-1;
					float lastprintangel=-1;
					float initx=-1;
					float inity=-1;
					boolean start=true;

					pointx = sc.nextFloat() + adjustionx;
					pointy = sc.nextFloat() + adjustiony;
					initx=pointx;
					inity=pointy;
					//jump the print angel
					printangel=sc.nextFloat();
					angel = -999;
				//	System.out.println();
					while (((pointx != initx || pointy != inity) && start == false)||start==true) {
							start=false;
							lastPointx = pointx;
							lastPointy = pointy;
							lastprintangel=printangel;
							pointx = sc.nextFloat() + adjustionx;
							pointy = sc.nextFloat() + adjustiony;
							//jump the print angel							
							printangel=sc.nextFloat();
							angel = getrealangel(pointx-lastPointx,pointy-lastPointy);
						

						// fill metrix according to the coordinates
						//use recalculated angel, partial lines not loaded because the same angel stop loop method!!!!!!!!!!!!!!!!
						float fillPointx = lastPointx;
						float fillPointy = lastPointy;
						int xIndex = 0;
						int yIndex = 0;
						//填充方法为填充射线路径，因为计算可能有误差，所以加入另一终点的的预算index作为限制
						int limitxIndex=Math.round(pointx/ precisionapplycontour);
						int limityIndex=Math.round(pointy/ precisionapplycontour);
						int counterx = (int) (Math.abs(((pointx - lastPointx) / Math.cos(Math
										.toRadians(angel)))
										/ precisionapplycontour));
						int countery = (int) (Math.abs(((pointy - lastPointy) / Math.sin(Math
										.toRadians(angel)))
										/ precisionapplycontour));
						int realcounter = counterx;
						if (countery > counterx) {
							realcounter = countery;
						}
						// System.out.println(counterx+" "+countery+" "+(lastPointx-adjustionx)+" "+(lastPointy-adjustiony)+" "+Math.toRadians(angel)+" "+angel);
						while (realcounter >= 0) {
							xIndex = Math.round(fillPointx
									/ precisionapplycontour);
							yIndex = Math.round(fillPointy
									/ precisionapplycontour);
							
							dotMatrix[xIndex][yIndex][currentlayer]
									.setPrintObject();
							fillPointx += precisionapplycontour
									* Math.cos(Math.toRadians(angel));
							fillPointy += precisionapplycontour
									* Math.sin(Math.toRadians(angel));
							
							realcounter--;
						}
					}
				}
				line = sc.nextLine();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		printlayer(dotMatrix, 0, countz - 1, county, countx);
		System.out.println("Finish loading contour into Metrix");

		// may not needed this contour editing any more
		// must make sure the contour is reasonable for filling process
		// 1. if a dot has two or more connected dots in 2d but without blank
		// space between each of them, then the dot is illegal
		// 2. if a dot has less than two connected dot in 2d, then it is also
		// illegal
		for (int k = 0; k < countz; k++) {
			// this means only check the dots with 8 near dots in 2d
			for (int j = 1; j < county - 1; j++) {
				for (int i = 1; i < countx - 1; i++) {
					// a state machine for rule 1 and 2
					if (dotMatrix[i][j][k].getoutDirect == 7) {
						// legal state 0 is init, 1 means has the first 7, 2
						// mean has the blank after the first 7, 3 means has
						// another
						// dot after the black which means this dot is legal
						int legalstate = 0;
						// the startdot is used for tracing the first and last
						// near dot
						int startdot = -1;
						for (int count = 1; count <= 8; count++) {
							if (legalstate == 0
									&& getIsBoundary(count, dotMatrix, i, j, k)) {
								legalstate = 1;
								startdot = count;
							} else if (legalstate == 1
									&& !getIsBoundary(count, dotMatrix, i, j, k)) {
								legalstate = 2;
							} else if (legalstate == 2 && startdot != 1
									&& getIsBoundary(count, dotMatrix, i, j, k)) {
								// this is legal
								legalstate = 3;
							} else if (legalstate == 2 && startdot == 1
									&& getIsBoundary(count, dotMatrix, i, j, k)) {
								// this needs another blank
								legalstate = 4;
							} else if (legalstate == 4
									&& startdot == 1
									&& !getIsBoundary(count, dotMatrix, i, j, k)) {
								legalstate = 3;
							}
						}
						if (legalstate != 3) {
							// delete the contour dot
							dotMatrix[i][j][k].getoutDirect = 0;
						}
					}
				}
			}
		}

		System.out.println("Finish contour editing");

		// use bfs to verify the outter metrix filling, use it to get ride of
		// error 8
		// only 2d is needed, 3d bfs will never be used here
		// still need to upgrade to multiple block filling verify (currently
		// only the outter area)
		for (int k = 0; k < countz; k++) {
			int currentgetoutDir = 8;
			for (int j = 0; j < county; j++) {
				for (int i = 0; i < countx; i++) {
					if (dotMatrix[i][j][k].getoutDirect != 7
							&& dotMatrix[i][j][k].indexx != -1) {
						if (dotMatrix[i][j][k].dotchecked == false) {

							// use a bfs for this unchecked region
							List<Dot> sameblock = new ArrayList<Dot>();
							sameblock.add(dotMatrix[i][j][k]);
							while (!sameblock.isEmpty()) {
								Dot tempdot = sameblock.get(0);

								// mark the first one
								if (tempdot.dotchecked == false) {
									if (currentgetoutDir == 0) {
										tempdot.getoutDirect = 8;
										tempdot.dotchecked = true;
									} else if (currentgetoutDir == 8) {
										tempdot.getoutDirect = 0;
										tempdot.dotchecked = true;
									} else {
										System.out
												.println("bfs filling error! wrong getoutDir");
									}
								}

								// check its near dots, bfs starts
								for (int dircount = 1; dircount < 5; dircount++) {
									Dot tempneardot = tempdot
											.getneardot(dircount);
									// if the near dot is not a null dot or a 7
									if (tempneardot.indexx != -1
											&& tempneardot.getoutDirect != 7
											&& tempneardot.dotchecked == false) {
										if (currentgetoutDir == 0) {
											tempneardot.getoutDirect = 8;
										} else if (currentgetoutDir == 8) {
											tempneardot.getoutDirect = 0;
										} else {
											System.out
													.println("bfs filling error! wrong getoutDir");
										}
										sameblock.add(tempneardot);
										tempneardot.dotchecked = true;
									}
								}
								sameblock.remove(tempdot);
							}
							currentgetoutDir = dotMatrix[i][j][k].getoutDirect;
						} else {
							// it would be either 0 or 8
							currentgetoutDir = dotMatrix[i][j][k].getoutDirect;
						}
					}// if dir!=7
				}
			}

		}

		System.out.println("Finish bfs filling");

		// print layer 1
		// printlayer(dotMetrix,0,county,countx);

		// printlayer(dotMetrix,0,county,countx);
		// printConnectNum(dotMetrix,0,county,countx);
		long endconstructTime = System.nanoTime();
		System.out.println("All Construct Took "
				+ (endconstructTime - startconstructTime) / 1000000 + " ms");

		long startTime = System.nanoTime();
		// start to calculate each dot and put them into object
		// first iterate each unsure direction dots, change the direction
		// of one dot if it is sure in other dot's iterated calculation

		// will create a list of object here for store each dot, instead of just
		// mark their get out
		// direction
		int oncecounter=0;
		List<Object> objectlist = new ArrayList<Object>();
		for (int k = 0; k < countz; k++) {
			for (int j = 0; j < county; j++) {
				for (int i = 0; i < countx; i++) {
					if(calconce!=true||oncecounter==0){
						oncecounter++;
					if (dotMatrix[i][j][k].getoutDirect == 0) {
						// calculate each direction to get out the dot
						// and find the largest object which the dot will be get
						// out
						// this greedy alogrithm does not guarantee to yield the
						// shortest seperation line

						Object largestObject = new Object();
						// operate in 2d first, change later
						for (int dircount = 1; dircount < 7; dircount++) {

							// init the object
							Object tempObject = new Object();
							tempObject.getoutDirect = dircount;
							tempObject.dotlist.add(dotMatrix[i][j][k]);
							dotMatrix[i][j][k].intempObject = 1;
							List<Dot> BFSdotlist = new ArrayList<Dot>();
							// although the size will always be 6...
							// for(int
							// counter=0;counter<dotMetrix[i][j][k].nearbylist.size();counter++){
							// only add the dot in object for expanding calc
							BFSdotlist.add(dotMatrix[i][j][k]);
							// }

							// use bfs to expand the calculation for adding new
							// dots in object
							while (!BFSdotlist.isEmpty()) {
								// get the first dot in the list and check its
								// neardot can be qualified to be put in object
								Dot tempdot = BFSdotlist.get(0);
								int directDepth = tempdot.getDepth(dircount);

								// first make sure its direct will not be unsure
								// for the first dot,
								// because dot in the object should always know
								// its get out direction
								if (directDepth == 0) {
									directDepth = findDepth(tempdot, dircount);
								}

								// if it is infinite depth for that direction,
								// get all near by dots with same
								// depth in same direction into the object
								// because the algorithm need to find the direct
								// getout objects first then
								// calc the rest to get out depend on these
								// object, so the directDepth>0 case
								// will be develop in another loop
								if (directDepth == -1) {
									for (int counter = 0; counter < tempdot.nearbylist
											.size(); counter++) {
										// currently only 2d if not commented
										// if(tempdot.nearbylist.get(counter).direct!=5&&tempdot.nearbylist.get(counter).direct!=6){
										Dot tempneardot = tempdot.nearbylist
												.get(counter).neardot;
										// check whether it is already in the
										// object or other object and whether is
										// a nulldot

										if (tempneardot.intempObject != 1
												&& (tempneardot.z != -999)
												&& (tempneardot.getoutDirect == 0)) {
											int tempneardotdepth = tempneardot
													.getDepth(dircount);

											// if it is not infinite get out
											// from that direction, and not in
											// any other object
											// then it must be on the edge of
											// the infinite object
											// since its near dot is in object
											// and itself will not be in the
											// object
											// should use a flag to indicate
											// depth unsure
											if (tempneardotdepth > 0) {
												tempObject.neardotlist
														.add(tempneardot);
											}

											// if its depth is also infinite,
											// then add the dot in the object
											if (tempneardotdepth == directDepth) {
												tempObject.dotlist
														.add(tempneardot);
												BFSdotlist.add(tempneardot);
												tempneardot.intempObject = 1;
											}
											// should use a flag to indicate
											// depth unsure
											// if the depth is unsure, then use
											// a recursive calc to find out its
											// depth
											else if (tempneardotdepth == 0) {
												// go alone with that direction
												// and find if there is a block
												// or a infinite get out dot
												// in that direction, must be
												// one of these two cases
												// since it is the infinite
												// object, we do not care there
												// is other object on the way
												// can use dynamic programming
												// to improve run time
												tempneardotdepth = findDepth(
														tempneardot, dircount);

												// if it is not infinite get out
												// from that direction, and not
												// in any other object
												// then it must be on the edge
												// of the infinite object
												// since its near dot is in
												// object and itself will not be
												// in the object
												// should use a flag to indicate
												// depth unsure,the 0 here is
												// not unsure as before!!!
												if (tempneardotdepth >= 0) {
													tempObject.neardotlist
															.add(tempneardot);
												}

												if (tempneardotdepth == directDepth) {
													tempObject.dotlist
															.add(tempneardot);
													BFSdotlist.add(tempneardot);
													tempneardot.intempObject = 1;
												}
											}

										}
										// }//except up and down in 3d
									}// check all its neighbors
								}

								// remove the dot from bfs list
								BFSdotlist.remove(tempdot);
							}

							// clear all the intempObject status for all dots in
							// tempobject
							for (int counter = 0; counter < tempObject.dotlist
									.size(); counter++) {
								tempObject.dotlist.get(counter).intempObject = 0;
							}
							// make sure it must be an infinite get out object
							boolean infiniteObject = (tempObject.dotlist.get(0)
									.getDepth(dircount) == -1);
							if (largestObject.dotlist.size() == 0
									&& infiniteObject) {
								largestObject = tempObject;
							} else if (largestObject.dotlist.size() < tempObject.dotlist
									.size() && infiniteObject) {
								largestObject = tempObject;
							}

						}
						// calc done, store the info
						if (largestObject.dotlist.size() > 0) {
							// settle the object id and info
							largestObject.objectID = objectlist.size();
							largestObject.infinite = 1;
							// put the object in the object list
							objectlist.add(largestObject);
							// the object is sure now, set the getoutDirect and
							// in which object for each point
							for (int counter = 0; counter < largestObject.dotlist
									.size(); counter++) {
								largestObject.dotlist.get(counter).getoutDirect = largestObject.getoutDirect;
								largestObject.dotlist.get(counter).inObjectID = largestObject.objectID;
							}
							// mark all the edge dots
							// the onedge dots are not necessary to be out of
							// later largestObject, but it must not be in
							// current largest
							// object
							for (int counter = 0; counter < largestObject.neardotlist
									.size(); counter++) {
								largestObject.neardotlist.get(counter).onEdge = 2;
							}
						}
					}// end the calc for one unsure point and generated a object
					}
				}
			}
		}

		System.out.println("Finish 3d infinite direct get out");

		// printlayer(dotMetrix,0,countz-1,county,countx);
		// printedge(dotMetrix,0,countz-1,county,countx);

		if (simplecalc == true) {
			System.out
					.println("simple calc is on, only infinite get out enabled");
		}
		if (simplecalc == false) {

			// start the iterative get out
			// use hill climbing, so select the shortest edge count as final
			// result
			// need a store object list (or edge list) for the shortest edge
			// counts result
			// currently just random climb, not with a good seed

			int edgecount = -1;
			// init is done
			// may turn off the hill climb by set its counter to 1
			for (int climbtime = 0; climbtime < 1; climbtime++) {

				// init all the conditions for a new hill climb, refresh all the
				// finite object
				clearfiniteobject(objectlist);

				for (int counter = 0; counter < objectlist.size(); counter++) {
				//	System.out.println("object calc: "+counter+"/total "+objectlist.size());
					for (int counterNear = 0; counterNear < objectlist
							.get(counter).neardotlist.size(); counterNear++) {
						// if the edge dot is still not in an object, it must be
						// get out depended on the existed object
						Dot tempdot = objectlist.get(counter).neardotlist
								.get(counterNear);
						if (tempdot.getoutDirect == 0&&tempdot.indexx!=-1) {
							
							Object largestobject = new Object();
							// if the near dot on the direction
							for (int dircount = 1; dircount < 7; dircount++) {
								if (tempdot.isNearObject(dircount)) {
									// means this direction has a object, it is
									// possible for the dot to get out
									// use bfs to get a full list of near by
									// edges
									List<Dot> BFSEdgeDot = new ArrayList<Dot>();
									EdgeList edgelist = new EdgeList(
											tempdot.nearObjectID(dircount),
											dircount);
									Object tempobject = new Object();
									// init with first dot
									BFSEdgeDot.add(tempdot);
									edgelist.edgeDotList.add(tempdot);
									tempdot.inedgeList = 1;
									while (!BFSEdgeDot.isEmpty()) {
										Dot tempedgedot = BFSEdgeDot.get(0);

										// for each edge dot, its corner dot
										// needs to be checked
										if (tempedgedot.cornerlist.size() == 0) {
											tempedgedot.initCornerDot(dotMatrix, countx, county, countz);
										}
										
										// for each corner dot

										for (int countercorner = 0; countercorner < tempedgedot.cornerlist
												.size(); countercorner++) {
											Dot tempedgedotnear = tempedgedot.cornerlist
													.get(countercorner).cornerdot;
											// currently 2d if dirextz==0
											if (tempedgedotnear.onEdge >= 1
													&& tempedgedotnear.inObjectID == -1
													&& tempedgedotnear.inedgeList == 0&&tempedgedotnear.indexx!=-1) {
												if (tempedgedotnear
														.nearObjectID(dircount) == edgelist.nearobjectID) {
													edgelist.edgeDotList
															.add(tempedgedotnear);
													BFSEdgeDot
															.add(tempedgedotnear);
													tempedgedotnear.inedgeList = 1;
												}
											}
										}

										// for each near dot
										for (int dircountsub = 1; dircountsub < 7; dircountsub++) {
											// if the near directountsub dot is
											// on edge
											// must get from 8 direction instead
											// of 4
											Dot tempedgedotnear = tempedgedot
													.getneardot(dircountsub);
											if (tempedgedotnear.onEdge >= 1
													&& tempedgedotnear.inObjectID == -1
													&& tempedgedotnear.inedgeList == 0&&tempedgedotnear.indexx!=-1) {
												// need to have a mark to
												// indicate have searched
												// also its dircount(direction)
												// has the same object onsame
												// direction
												// here can be improved by
												// marking the nearby dot when
												// forming a object
												if (tempedgedotnear
														.nearObjectID(dircount) == edgelist.nearobjectID) {
													edgelist.edgeDotList
															.add(tempedgedotnear);
													BFSEdgeDot
															.add(tempedgedotnear);
													tempedgedotnear.inedgeList = 1;
												}
											}

										}
										// remove after done calc
										BFSEdgeDot.remove(tempedgedot);
									}

									// the edgelist should be complete by now
									// now get all the depth of inObject and
									// available space for all dots in edgelist
									for (int counterlist = 0; counterlist < edgelist.edgeDotList
											.size(); counterlist++) {
										edgelist.edgeDotList.get(counterlist).objectdepth = getObjectDepth(
												edgelist.edgeDotList
														.get(counterlist),
												edgelist.direct,
												edgelist.nearobjectID);
										edgelist.edgeDotList.get(counterlist).availdepth = findfiniteDepth(
												edgelist.edgeDotList
														.get(counterlist),
												oppositedir(edgelist.direct));
									}

									// the core calc for get maximum sub-object,
									// the generate a new temp object
									// hill climbing for finding the largest
									// possible object
									int maxvolumn = 0;
									// current min of object depth, which is max
									// depth to get out object
									int limitdepth = edgelist.edgeDotList
											.get(0).objectdepth;
									// create a new list dot for store the temp
									// maximum sub-object's edge of get out
									// direction
									List<Dot> realEdgeList = new ArrayList<Dot>();
									// use bfs to expand the first dot
									List<Dot> bfsrealEdge = new ArrayList<Dot>();
									// use for clear the checked flag and have
									// another itr with diff direct calc
									List<Dot> checkeddots = new ArrayList<Dot>();

									bfsrealEdge
											.add(edgelist.edgeDotList.get(0));
									// use inedgeList flag, to make sure the
									// expand dot are at edge
									// it is np complete! so... better improve
									// it
									// first calc whether current dot from bfs
									// can be put in reallist, if can, expand
									// its near dots
									// and put onedge dots on the bfs

									// infinite loop!
									while (!bfsrealEdge.isEmpty()) {
										Dot temprealdot = bfsrealEdge.get(0);

										int tempvolumn = 0;
										boolean expand = false;
										// no adjust to the limit, no need to
										// recalc
										if (limitdepth <= temprealdot.objectdepth) {
											maxvolumn += Math.min(
													temprealdot.objectdepth,
													temprealdot.availdepth);
											realEdgeList.add(temprealdot);
											expand = true;
										}
										// recalc the new volumn and see whether
										// it is bigger
										if (limitdepth > temprealdot.objectdepth) {
											for (int countersub = 0; countersub < realEdgeList
													.size(); countersub++) {
												tempvolumn += Math
														.min(realEdgeList
																.get(countersub).availdepth,
																temprealdot.objectdepth);
											}
											if (tempvolumn >= maxvolumn) {
												maxvolumn = tempvolumn;
												realEdgeList.add(temprealdot);
												limitdepth = temprealdot.objectdepth;
												expand = true;
											}
											// there is a chance, still expand
											// climbtime = 0 will be first
											// maximum stop
											// the variable here is critical
											else if (climbtime != 0
													&& (maxvolumn - tempvolumn) < 20
													&& (Math.random() * 100 > 90)) {
												maxvolumn = tempvolumn;
												realEdgeList.add(temprealdot);
												limitdepth = temprealdot.objectdepth;
												expand = true;
											}
										}

										if (expand) {
											// currently only 2d if <5
											for (int dircountsub = 1; dircountsub < 7; dircountsub++) {
												// add near dots on same edge
												// list
												Dot temprealneardot = temprealdot
														.getneardot(dircountsub);
												if (temprealneardot.inedgeList == 1
														&& temprealneardot.checkedrealList == 0) {
													bfsrealEdge
															.add(temprealneardot);
													temprealneardot.checkedrealList = 1;
													checkeddots
															.add(temprealneardot);
												}
											}

											for (int countersub = 0; countersub < temprealdot.cornerlist
													.size(); countersub++) {
												CornerDot cornerdot = temprealdot.cornerlist
														.get(countersub);
												if (cornerdot.cornerdot.inedgeList == 1
														&& cornerdot.cornerdot.checkedrealList == 0) {
													bfsrealEdge
															.add(cornerdot.cornerdot);
													cornerdot.cornerdot.checkedrealList = 1;
													checkeddots
															.add(cornerdot.cornerdot);
												}
											}

										}
										// remove the dot from bfs when done
										bfsrealEdge.remove(temprealdot);

									}

									// clear the flags for another itr
									while (!checkeddots.isEmpty()) {
										checkeddots.get(0).checkedrealList = 0;
										checkeddots.remove(0);
									}

									// done calc,make a new temp object
									// do not give an id until it is the sure
									// largest object

									for (int countersub = 0; countersub < realEdgeList
											.size(); countersub++) {
										int getdepth = Math
												.min(realEdgeList
														.get(countersub).availdepth,
														limitdepth);
										Dot startadddot = realEdgeList
												.get(countersub);
										// add all dots on this depth path to
										// the object
										for (int countersub2 = 0; countersub2 <= getdepth; countersub2++) {
											tempobject.dotlist.add(startadddot);
											startadddot = startadddot
													.getneardot(oppositedir(edgelist.direct));
										}
										tempobject.getoutDirect = edgelist.direct;

									}

									if (largestobject.dotlist.size() < tempobject.dotlist
											.size()
											|| largestobject.dotlist.size() == 0) {
										largestobject = tempobject;
									}

									// clear all flag for a new edgelist
									for (int countersub = 0; countersub < edgelist.edgeDotList
											.size(); countersub++) {
										edgelist.edgeDotList.get(countersub).inedgeList = 0;
									}

								}

							}

							// finalize largestobject

							for (int countersub = 0; countersub < largestobject.dotlist
									.size(); countersub++) {
								largestobject.dotlist.get(countersub).getoutDirect = largestobject.getoutDirect;
								largestobject.dotlist.get(countersub).inObjectID = largestobject.objectID;
							}
							largestobject.objectID = objectlist.size();
							largestobject.infinite = 0;
							// down: this segment of code hasn't been debugged
							// yet
							// must mark the near dots!
							// the calc may be more concise later
							// itr all the dot in the largestobject
							for (int dotcount = 0; dotcount < largestobject.dotlist
									.size(); dotcount++) {
								Dot itrtempdot = largestobject.dotlist
										.get(dotcount);
								for (int countersub3 = 1; countersub3 < 7; countersub3++) {
									Dot itrtempneardot = itrtempdot
											.getneardot(countersub3);
									if (itrtempneardot.getoutDirect == 0
											&& itrtempneardot.onEdge == 0) {
										largestobject.neardotlist
												.add(itrtempneardot);
										itrtempneardot.onEdge = 1;
									}
								}
							}
							// up

							objectlist.add(largestobject);

						}
					}
				}

				int tempedgecount = calcedgecount(dotMatrix, 0, countz - 1,
						county, countx);

				// save finiteobjects if better
				if (edgecount == -1) {
					System.out.println("init hill climb:" + tempedgecount);
					for (int objectcounter = 0; objectcounter < objectlist
							.size(); objectcounter++) {
						if (objectlist.get(objectcounter).infinite == 0) {
							for (int counter = 0; counter < objectlist
									.get(objectcounter).dotlist.size(); counter++) {
								objectlist.get(objectcounter).dotlist
										.get(counter).maxonEdge = objectlist
										.get(objectcounter).dotlist
										.get(counter).onEdge;
								objectlist.get(objectcounter).dotlist
										.get(counter).maxgetoutDirect = objectlist
										.get(objectcounter).dotlist
										.get(counter).getoutDirect;
							}
						}
					}
					edgecount = tempedgecount;
				} else if (edgecount > tempedgecount) {
					System.out.println("hill climb from " + edgecount + " to "
							+ tempedgecount);

					for (int objectcounter = 0; objectcounter < objectlist
							.size(); objectcounter++) {
						if (objectlist.get(objectcounter).infinite == 0) {
							for (int counter = 0; counter < objectlist
									.get(objectcounter).dotlist.size(); counter++) {
								objectlist.get(objectcounter).dotlist
										.get(counter).maxonEdge = objectlist
										.get(objectcounter).dotlist
										.get(counter).onEdge;
								objectlist.get(objectcounter).dotlist
										.get(counter).maxgetoutDirect = objectlist
										.get(objectcounter).dotlist
										.get(counter).getoutDirect;
							}
						}
					}
					edgecount = tempedgecount;
				}
				// else{
				// System.out.println("no hill climb, temp edge count "+tempedgecount);
				// }

			}// finish hill climbing

			// final result for finite object
			for (int objectcounter = 0; objectcounter < objectlist.size(); objectcounter++) {
				if (objectlist.get(objectcounter).infinite == 0) {
					for (int counter = 0; counter < objectlist
							.get(objectcounter).dotlist.size(); counter++) {
						objectlist.get(objectcounter).dotlist.get(counter).onEdge = objectlist
								.get(objectcounter).dotlist.get(counter).maxonEdge;
						objectlist.get(objectcounter).dotlist.get(counter).getoutDirect = objectlist
								.get(objectcounter).dotlist.get(counter).maxgetoutDirect;
					}
				}
			}

			System.out.println("Finish 3d finite direct get out");
		}

		long endTime = System.nanoTime();
		System.out.println("Infinite and Finite Calc Took "
				+ (endTime - startTime) / 1000000 + " ms");
		
		
		//there could be some gap between layer if there is a rapid change in shape
		//check whether there is a gap (a non-nulldot and non-edge dot and non-7 dot and non-8 dot
		//above or below the 8 dot), if there is, make the near dot as an edge
		if(rapidChange) {
		for (int layer = 0; layer < countz; layer++) {
			for (int j = 0; j < county; j++) {
				for (int i = 0; i < countx; i++) {
					if (dotMatrix[i][j][layer].getoutDirect == 8) {
						if(layer-1>=0){
							Dot tempdot=dotMatrix[i][j][layer-1];
							if(tempdot.getoutDirect!=8&&tempdot.getoutDirect!=7
									&&tempdot.indexx!=-1&&tempdot.onEdge==0){
								tempdot.onEdge=1;
							}
						}
						if(layer+1<countz){
							Dot tempdot=dotMatrix[i][j][layer+1];
							if(tempdot.getoutDirect!=8&&tempdot.getoutDirect!=7
									&&tempdot.indexx!=-1&&tempdot.onEdge==0){
								tempdot.onEdge=1;
							}
						}
						}
					}
				}
			}
		System.out.println("Finish rapid changing edge");
		}
		
		
		

		// print out for each layer to txt
		printedge(dotMatrix, 0, countz - 1, county, countx);
		printlayer(dotMatrix, 0, countz - 1, county, countx);
		printall(dotMatrix, countz - 1, county, countx, filename,layerHeight,initHeight,adjustionx,adjustiony);

		System.out.println("Finish Print Seperate Line!");

	}// end of main

	public static void clearfiniteobject(List<Object> objectlist) {
		for (int objectrefresh = 0; objectrefresh < objectlist.size(); objectrefresh++) {
			// if it is an finite object, clear everything and every flag of
			// dots in it
			if (objectlist.get(objectrefresh).infinite == 0) {
				for (int dotrefresh = 0; dotrefresh < objectlist
						.get(objectrefresh).dotlist.size(); dotrefresh++) {
					// check whether it is an edge of infinite object, if it is
					// then the onedge property remains the same
					if (objectlist.get(objectrefresh).dotlist.get(dotrefresh).onEdge != 2) {
						objectlist.get(objectrefresh).dotlist.get(dotrefresh).onEdge = 0;
					}
					objectlist.get(objectrefresh).dotlist.get(dotrefresh).getoutDirect = 0;
					objectlist.get(objectrefresh).dotlist.get(dotrefresh).intempObject = 0;
					objectlist.get(objectrefresh).dotlist.get(dotrefresh).inObjectID = -1;
					objectlist.get(objectrefresh).dotlist.get(dotrefresh).inedgeList = 0;
					objectlist.get(objectrefresh).dotlist.get(dotrefresh).objectdepth = -1;
					objectlist.get(objectrefresh).dotlist.get(dotrefresh).availdepth = -1;
					objectlist.get(objectrefresh).dotlist.get(dotrefresh).checkedrealList = 0;
					objectlist.get(objectrefresh).dotlist.get(dotrefresh).cornerlist
							.clear();

				}
				objectlist.remove(objectrefresh);
				objectrefresh--;
			}
		}
	}

	public static int calcedgecount(Dot[][][] metrix, int layerlow,
			int layerhigh, int county, int countx) {
		int edgecount = 0;
		for (int layer = layerlow; layer <= layerhigh; layer++) {
			for (int j = 0; j < county; j++) {
				for (int i = 0; i < countx; i++) {
					if (metrix[i][j][layer].onEdge >= 1) {
						edgecount++;
					}
				}
			}// one layer end
		}
		return edgecount;
	}

	// enter the metrix, the lowest layer to print and the highest layer to
	// print and the size of the map
	// it will print all layers between lowest layer and highest layer
	public static void printlayer(Dot[][][] metrix, int layerlow,
			int layerhigh, int county, int countx) {
		// try to print the layer x to y in eclipse console!
		// for fun and for debug!!!

		PrintWriter writer;
		try {
			writer = new PrintWriter("output_console.txt", "UTF-8");
			for (int layer = layerlow; layer <= layerhigh; layer++) {
				writer.print("layer: " + layer + "\n");
				for (int j = 0; j < county; j++) {
					for (int i = 0; i < countx; i++) {
						// System.out.print(metrix[i][j][layer].getoutDirect);
						writer.print(metrix[i][j][layer].getoutDirect);
					}
					// System.out.print("\n");
					writer.print("\n");
				}// one layer end
				writer.print("\n");
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void printedge(Dot[][][] metrix, int layerlow,
			int layerhigh, int county, int countx) {
		PrintWriter writer;
		try {
			writer = new PrintWriter("output_edge.txt", "UTF-8");
			for (int layer = layerlow; layer <= layerhigh; layer++) {
				writer.print("layer: " + layer + "\n");
				for (int j = 0; j < county; j++) {
					for (int i = 0; i < countx; i++) {
						// System.out.print(metrix[i][j][layer].getoutDirect);
						if(metrix[i][j][layer].onEdge>=1){
							writer.print("e");
						}
						else{
						writer.print(metrix[i][j][layer].getoutDirect);
						}
					}
					// System.out.print("\n");
					writer.print("\n");
				}// one layer end
				writer.print("\n");
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	public static boolean hasFutureEdge(PrintWriter _writer, Dot _dot,
			int layer, Dot[][][] matrix, int boundx, int boundy, int boundz) {
		boolean end = true;
		for (int i = 0; i < _dot.nearbylist.size(); i++) {
			if (_dot.nearbylist.get(i).neardot.indexz == layer) {
				if (_dot.nearbylist.get(i).neardot.onEdge >= 1
						&& _dot.nearbylist.get(i).neardot.printed == false) {
					end = false;
				}
			}
		}
		if (_dot.cornerlist.size() == 0) {
			_dot.initCornerDot(matrix, boundx, boundy, boundz);
		}
		for (int i = 0; i < _dot.cornerlist.size(); i++) {
			if (_dot.cornerlist.get(i).cornerdot.indexz == layer) {
				if (_dot.cornerlist.get(i).cornerdot.onEdge >= 1
						&& _dot.cornerlist.get(i).cornerdot.printed == false) {
					end = false;
				}
			}
		}

		return !end;
	}
	

	public static float getrealangel(float x, float y){
		double angle=0;
		float PI=(float) 3.14159265;
		float shypo=(float) Math.sqrt(x*x+y*y);//length of hypotenuse
		if(x==0&y==0)
		{
			return 0;
		}
		else if(x>=0&&y>=0){
			angle=Math.asin(y/shypo)/PI*180;
			}
		else if(x<0&&y>=0){
			angle=180-Math.asin(y/shypo)/PI*180;
		}
		else if(x<0&&y<0){
			angle=180-Math.asin(y/shypo)/PI*180;
		}
		else{
			angle=360+Math.asin(y/shypo)/PI*180;
		}
		return (float) angle;
	}
	

	public static float getprintangle(float x, float y){
		double angle=0;
		float PI=(float) 3.14159265;
		float shypo=(float) Math.sqrt(x*x+y*y);//length of hypotenuse
		if(x==0&y==0)
		{
			return 0;
		}
		else if(x>=0&&y>=0){
			angle=90.0+Math.asin(y/shypo)/PI*180;
			}
		else if(x<0&&y>=0){
			angle=270.0-Math.asin(y/shypo)/PI*180;
		}
		else if(x<0&&y<0){
			angle=270-Math.asin(y/shypo)/PI*180;
		}
		else{
			angle=90+Math.asin(y/shypo)/PI*180;
		}
		return (float) angle;
	}
	
	
	public static boolean isNearDot(Dot formerdot,int dotindex){
		boolean start=false;
		boolean near=false;
		boolean end=false;
		boolean line=false;
		float dx;
		float dy;
		float laterdx=0;
		float laterdy=0;
		Dot currentdot=Segment.get(dotindex);
		if(formerdot==null){
			start=true;
		}
		else if(formerdot!=null&&(Math.abs(formerdot.x-currentdot.x)>precisionValue+0.1||
				Math.abs(formerdot.y-currentdot.y)>precisionValue+0.1)){
			start=true;
		}
		//is an end dot
		if(dotindex+1>=Segment.size()){
			end=true;
		}
		else if(Math.abs(Segment.get(dotindex+1).x-currentdot.x)>precisionValue+0.1||
				Math.abs(Segment.get(dotindex+1).y-currentdot.y)>precisionValue+0.1){
			end=true;
		}
		//is in a line
		if(!start&&!end){
			dx=currentdot.x - formerdot.x;
			dy=currentdot.y - formerdot.y;
			laterdx=Segment.get(dotindex+1).x-currentdot.x;
			laterdy=Segment.get(dotindex+1).y-currentdot.y;
			if(Math.abs(dx-laterdx)<0.1&&Math.abs(dy-laterdy)<0.1){
				line=true;
			}
		}
		return (!start&&!end&&!line);
		
	}
	

	public static void print_subsegment(PrintWriter _writer,float _adjustx,float _adjusty,float _height){
		float laterdy=0;
		float laterdx=0;
		//system_out_seg(printSegment);
		_writer.println("seperation layer:");
		_writer.println(_height);
		_writer.println(printSegment.size());
		//System.out.print("point number:"+printSegment.size()+"\n");
		for(int i=0;i<printSegment.size();i++){
			
			//System.out.println(Math.round((printSegment.get(i).x-_adjustx)*1000.000)/1000.000+" "+Math.round((printSegment.get(i).y-_adjusty)*1000.000)/1000.000);
			if(i+1<printSegment.size()){
				laterdx=printSegment.get(i+1).x-printSegment.get(i).x;
				laterdy=printSegment.get(i+1).y-printSegment.get(i).y;
				_writer.println(Math.round((printSegment.get(i).x-_adjustx-manualadjustx)*1000.000)/1000.000+" "+Math.round((printSegment.get(i).y-_adjusty-manualadjusty)*1000.000)/1000.000+" "+Math.round(getprintangle(laterdx,laterdy)*1000.000)/1000.000);
			}
			else{
				_writer.println(Math.round((printSegment.get(i).x-_adjustx-manualadjustx)*1000.000)/1000.000+" "+Math.round((printSegment.get(i).y-_adjusty-manualadjusty)*1000.000)/1000.000+" "+Math.round(getprintangle(laterdx,laterdy)*1000.000)/1000.000);
			}
		}
	}
	
	
	
	//estimate how long time to print a set of line
	public static int est_Print(){
		//need to add lift time between each different line and add time to travel to the next line
		return 0;
	}
	
	//estimate how long to print a line, ignore time to turn angle, basically just time to go a length
	public static int est_linePrint(){
		return 0;
	}
	
	public static void system_out_seg(List<Dot> _seg){
		System.out.println("seg:");
		for(int i=0;i<_seg.size();i++){
			System.out.println(_seg.get(i).indexx+" "+_seg.get(i).indexy);
		}
		System.out.println("end seg");
	}
	
	//concise and print the line seg
	public static void conciseSeg(ArrayList<Dot> _seg,float _height,PrintWriter _writer,float _adjustx,float _adjusty){
		Dot formerdot = null;
		Dot currentdot =null;
		float dx;
		float dy;
		float laterdx=0;
		float laterdy=0;

		//n点插值，>=2, only support 2
		final int nearinterval=2;
		int intervalcount=nearinterval;
		
		//may consider the number of near edge as a indicator as a line or an area
		//also need to consider the how to distinguish a line nearly straight but not really staright
		
		for (int dotcount = 0; dotcount < _seg.size(); dotcount++) {
			currentdot=_seg.get(dotcount);
			boolean start=false;
			boolean end=false;
			boolean line=false;
			
			//determine what kind of dot it is
			//is a start dot
			if(formerdot==null){
				start=true;
			}
			//is an end dot
			if(dotcount+1>=_seg.size()){
				end=true;
			}
			//is in a line
			if(!start&&!end){
				dx=currentdot.x - formerdot.x;
				dy=currentdot.y - formerdot.y;
				laterdx=_seg.get(dotcount+1).x-currentdot.x;
				laterdy=_seg.get(dotcount+1).y-currentdot.y;
				if(Math.abs(dx-laterdx)<0.01&&Math.abs(dy-laterdy)<0.01){
					line=true;
				}
			}
			
			//print rules: start and end dots must be printed, a dot in a staright line is ignored, 
			//a dot is contiguous to other dots will be combined with near dot to form a new dot to be printed
			if(start&&!end){
				intervalcount=nearinterval-1;
				laterdx=_seg.get(dotcount+1).x-currentdot.x;
				laterdy=_seg.get(dotcount+1).y-currentdot.y;
				printSegment.add(currentdot);
				}
			else if(end&&!start){
				//print at each end of contour
				printSegment.add(currentdot);
				print_subsegment(_writer,_adjustx,_adjusty,_height);
				printSegment.clear();
			}
			else if(line){
				intervalcount=0;
				//print nothing
			}
			else{
				//print when count down to 0
				if(intervalcount==0){
					intervalcount=nearinterval;
					currentdot.x=(float) (Math.round((currentdot.x+_seg.get(dotcount+1).x)*100.0)/200.0);
					currentdot.y=(float) (Math.round((currentdot.y+_seg.get(dotcount+1).y)*100.0)/200.0);
					printSegment.add(currentdot);
					}
				else{
					intervalcount--;
				}
				
				}
			
			formerdot=_seg.get(dotcount);
	}
	}
	
	public static int countEdgeConnection(Dot _dot, int _layer){
		int counter=0;
		for (int i = 0; i < _dot.nearbylist.size(); i++) {
			if (_dot.nearbylist.get(i).neardot.indexz == _layer) {
				if (_dot.nearbylist.get(i).neardot.onEdge >= 1) {
					counter++;
				}
			}
		}
		for (int i = 0; i < _dot.cornerlist.size(); i++) {
			if (_dot.cornerlist.get(i).cornerdot.indexz == _layer) {
				if (_dot.cornerlist.get(i).cornerdot.onEdge >= 1) {
					counter++;
				}
			}
		}
		return counter;
	}
	
	
	public static ArrayList<Dot> getEdgePathStartByDot(Dot _dot, Dot[][][] matrix, HashSet<Dot> visited) {
		ArrayList<Dot> res = new ArrayList<Dot>();
		res.add(_dot);
		int maxSize = 0;
		ArrayList<Dot> candidate = null;
		for (int i = 0; i < _dot.nearbylist.size(); i++) {
			Dot nearDot = _dot.nearbylist.get(i).neardot;
			if (nearDot.indexz == _dot.indexz) {
				if (nearDot.onEdge >= 1	&& nearDot.printed == false && !visited.contains(nearDot)) {
					visited.add(nearDot);
					ArrayList<Dot> tempList = getEdgePathStartByDot(nearDot, matrix, visited);
					if(tempList.size() > maxSize) {
						candidate = tempList;
						maxSize = tempList.size();
					}
					visited.remove(nearDot);
				}
			}
		}
		if (_dot.cornerlist.size() == 0) {
			_dot.initCornerDot(matrix, countx, county, countz-1);
		}
		for (int i = 0; i < _dot.cornerlist.size(); i++) {
			Dot nearDot = _dot.cornerlist.get(i).cornerdot;
			if (nearDot.indexz == _dot.indexz) {
				if (nearDot.onEdge >= 1	&& nearDot.printed == false && !visited.contains(nearDot)) {
					visited.add(nearDot);
					ArrayList<Dot> tempList = getEdgePathStartByDot(nearDot, matrix, visited);
					if(tempList.size() > maxSize) {
						candidate = tempList;
						maxSize = tempList.size();
					}
					visited.remove(nearDot);
				}
			}
		}
		if(candidate != null) res.addAll(candidate);
		return res;
	}
	

	public static void printall(Dot[][][] matrix, int countz, int county,
			int countx, String _filename,float _layerHeight,float _initHeight,float _adjustx,float _adjusty) {
		File file = new File(_filename);
		//get original data again and parse them to new file
		try {
			PrintWriter writer;
			//sSystem.out.println(_filename.split(".txt")[0]);
			writer = new PrintWriter(_filename.split(".txt")[0]+"_output.txt", "UTF-8");
			Scanner sc = new Scanner(file);
			String line = sc.nextLine();
			
			int currentlayer = 0;
			float currentlayerHeight= (float) (Math.round(_initHeight*25.400*1000.000)/1000.000);
			// first, locate the layer
			while (sc.hasNextLine()) {
				//check if one layer ends, if it does, print the seperation line for that layer
				if(line.contains("Layer:")){
					String bufferwriter="";
					bufferwriter=line+"\r\n";
					float newlayerHeight=sc.nextFloat();
					bufferwriter+=newlayerHeight;
					//enter different height, print seperation line for last layer
					if(newlayerHeight!=currentlayerHeight){
						//System.out.println("layer:" + layer);
						for (int j = 0; j < county; j++) {
							for (int i = 0; i < countx; i++) {
								if (matrix[i][j][currentlayer].onEdge >= 1
										&& matrix[i][j][currentlayer].printed == false) {

									// if not solo point, print
									if (hasFutureEdge(writer, matrix[i][j][currentlayer],
											currentlayer, matrix, countx, county, countz)) {

										HashSet<Dot> visited = new HashSet<Dot>();
										visited.add(matrix[i][j][currentlayer]);
										ArrayList<Dot> path = getEdgePathStartByDot(matrix[i][j][currentlayer], matrix, visited);
										for(Dot dot : path)
											dot.printed = true;
										if(path.get(0).hasNearDot(path.get(path.size()-1))) path.add(matrix[i][j][currentlayer]);
										// the segment list is finished, processed and printed
										conciseSeg(path,currentlayerHeight,writer,_adjustx,_adjusty);
										
									}
								}
							}
						}// one layer end
						currentlayerHeight=newlayerHeight;
						currentlayer = (int) Math.round(((currentlayerHeight/25.40)-_initHeight)/_layerHeight);
						
					}
					writer.print(bufferwriter);
				}
				//copy and parse orginal data
				else{
					writer.println(line);
				}
				line = sc.nextLine();
				
			}
			//print the last line of origin file
			writer.println(line);
			
			//print the seperation line for last layer
			for (int j = 0; j < county; j++) {
				for (int i = 0; i < countx; i++) {
					if (matrix[i][j][currentlayer].onEdge >= 1
							&& matrix[i][j][currentlayer].printed == false) {

						// if not solo point, print
						if (hasFutureEdge(writer, matrix[i][j][currentlayer],
								currentlayer, matrix, countx, county, countz)) {

							HashSet<Dot> visited = new HashSet<Dot>();
							visited.add(matrix[i][j][currentlayer]);
							ArrayList<Dot> path = getEdgePathStartByDot(matrix[i][j][currentlayer], matrix, visited);
							for(Dot dot : path)
								dot.printed = true;
							if(path.get(0).hasNearDot(path.get(path.size()-1))) path.add(matrix[i][j][currentlayer]);
							// the segment list is finished, processed and printed
							conciseSeg(path,currentlayerHeight,writer,_adjustx,_adjusty);
							
						}
						}
					}
				}
		
			
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static boolean getIsBoundary(int num, Dot[][][] dotMetrix, int i,
			int j, int k) {
		if (num == 1) {
			return (dotMetrix[i][j - 1][k].getoutDirect == 7);
		}
		if (num == 2) {
			return (dotMetrix[i + 1][j - 1][k].getoutDirect == 7);
		}
		if (num == 3) {
			return (dotMetrix[i + 1][j][k].getoutDirect == 7);
		}
		if (num == 4) {
			return (dotMetrix[i + 1][j + 1][k].getoutDirect == 7);
		}
		if (num == 5) {
			return (dotMetrix[i][j + 1][k].getoutDirect == 7);
		}
		if (num == 6) {
			return (dotMetrix[i - 1][j + 1][k].getoutDirect == 7);
		}
		if (num == 7) {
			return (dotMetrix[i - 1][j][k].getoutDirect == 7);
		}
		if (num == 8) {
			return (dotMetrix[i - 1][j - 1][k].getoutDirect == 7);
		}
		System.out.println("error!");
		return false;
	}

	public static void printConnectNum(Dot[][][] metrix, int layer, int county,
			int countx) {
		// try to print the layer x in eclipse console!
		// for fun and for debug!!!
		for (int j = 0; j < county; j++) {
			for (int i = 0; i < countx; i++) {

				System.out.print(metrix[i][j][layer].nearbylist.size());

			}
			System.out.print("\n");
		}
	}

	// if return <0, then infinite, else it is the value of finite depth
	// depth should -1 if >0
	// currently, it will change all the depth on its path, so the run time will
	// be divided by n !!!
	public static int calcDepth(Dot startdot, int direct) {
		int depth = 1;
		// two ways of returning, hitting the block or infinite direct dot
		// this is only for infinite get out
		if (startdot.getDepth(direct) == -1) {
			return -99999;
		}
		if (startdot.getneardot(direct).getoutDirect != 0) {
			return 1;
		}
		depth += calcDepth(startdot.getneardot(direct), direct);

		// this is the all depth changing code
		if (depth < 0) {
			startdot.setDepth(-1, direct);
		}
		if (depth >= 0) {
			startdot.setDepth(depth, direct);
		}
		// end change depth path code
		return depth;
	}

	public static int findDepth(Dot startdot, int direct) {
		int depth = calcDepth(startdot, direct);
		if (depth > 0) {
			depth = depth - 1;
		}
		if (depth < 0) {
			depth = -1;
		}
		return depth;
	}

	// redundent, could be more concise
	public static int calcfiniteDepth(Dot startdot, int direct) {
		int depth = 1;
		// two ways of returning, hitting the block or infinite direct dot
		// this is only for infinite get out
		if (startdot.getDepth(direct) == -1) {
			return -99999;
		}
		// difference from infinite
		if (startdot.getneardot(direct).getoutDirect != 0) {
			return 1;
		}
		depth += calcDepth(startdot.getneardot(direct), direct);
		// this is the all depth changing code
		if (depth < 0) {
			startdot.setDepth(-1, direct);
		}
		if (depth >= 0) {
			startdot.setDepth(depth, direct);
		}
		// end change depth path code
		return depth;
	}

	public static int findfiniteDepth(Dot startdot, int direct) {
		int depth = calcfiniteDepth(startdot, direct);
		if (depth > 0) {
			depth = depth - 1;
		}
		if (depth < 0) {
			depth = -1;
		}
		return depth;
	}

	public static int oppositedir(int direct) {
		// hard code to get opposite direct
		int dir = 0;
		if (direct == 1) {
			dir = 2;
		}
		if (direct == 2) {
			dir = 1;
		}
		if (direct == 3) {
			dir = 4;
		}
		if (direct == 4) {
			dir = 3;
		}
		if (direct == 5) {
			dir = 6;
		}
		if (direct == 6) {
			dir = 5;
		}
		if (dir == 0) {
			System.out.println("error: opposite direction not found!");
		}
		return dir;
	}

	public static int getObjectDepth(Dot startdot, int direct, int objectid) {
		int depth = 0;
		Dot tempdot = startdot.getneardot(direct);
		;
		while (tempdot.inObjectID == objectid) {
			tempdot = tempdot.getneardot(direct);
			depth++;
		}
		return depth;
	}

}
