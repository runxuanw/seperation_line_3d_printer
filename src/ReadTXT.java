import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class ReadTXT {

	File file;
	//file must be the dir of 3d_line(not src)
	ReadTXT(String filename){
		file = new File(filename);
	}
	
	
	float findInitHeight(){
		//find first layer height, return in inch
		try {
			Scanner sc = new Scanner(file);
			String line=sc.nextLine();

			while(sc.hasNextLine()){
			if(line.contains("Layer:")){
				return (float) (sc.nextFloat()/25.4);
			}
			line=sc.nextLine();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//error
		return -1;
	}
	
	//number of layer in file is actually contours, must count layers again
	int findtotalLayer(){
		int totalLayer=0;
		float layerheight=0;
		try {
			Scanner sc = new Scanner(file);

			String line=sc.nextLine();
		    //find total number of layer
			while(sc.hasNextLine()){
				
			if(line.contains("Layer:")){
				float tempheight=sc.nextFloat();
				if(layerheight!=tempheight){
				totalLayer++;
				layerheight=tempheight;
				}
			}
				line=sc.nextLine();
			
			}
			if(totalLayer==-1){
				System.out.println("fatal error: total layer not found!");
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return totalLayer;
		
	} 
	
	//return in inch
	float findXLength(){
		float xmin=0;
		float xmax=0;

		try{
			Scanner sc = new Scanner(file);
			String line=sc.nextLine();
			while(sc.hasNextLine()){
		if(line.contains("XMax XMin YMax YMin")){
			xmax=sc.nextFloat();
			xmin=sc.nextFloat();
		}
		line=sc.nextLine();
			}
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("x-length "+(xmax-xmin));
		return (xmax-xmin);
	}
	
	float findYLength(){
		float ymin=0;
		float ymax=0;
		try{
			Scanner sc = new Scanner(file);
			String line=sc.nextLine();
			while(sc.hasNextLine()){
			if(line.contains("XMax XMin YMax YMin")){
				sc.nextFloat();
				sc.nextFloat();
				ymax=sc.nextFloat();
				ymin=sc.nextFloat();
			}
		line=sc.nextLine();
			}
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("y-length "+(ymax-ymin));
		return (ymax-ymin);
	}	
	
	
	float findLayerHeight(){
		float layerHeight=-1;
		float lowheight=-1;
		float highheight=-1;
		try {
			Scanner sc = new Scanner(file);
			//partially hard coded, jump the header
			String line=sc.nextLine();

			//locate a layer's height by finding the difference of two layers
			while(sc.hasNextLine()){
			if(line.contains("Layer:")){
				if(lowheight==-1){
				lowheight=sc.nextFloat();
				}
				else{
					highheight=sc.nextFloat();
					if(highheight>lowheight){
					layerHeight=highheight-lowheight;
					//convert layer height from mm to inch
					layerHeight=Float.parseFloat(String.format("%.4f", layerHeight/25.4));
					System.out.println("layer height:"+layerHeight);
					break;
					}
				}
			}
			line=sc.nextLine();
			}
			if(layerHeight==-1){
				System.out.println("fatal error: total layer height not found!");
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return layerHeight;
		
	}
	
	
}
