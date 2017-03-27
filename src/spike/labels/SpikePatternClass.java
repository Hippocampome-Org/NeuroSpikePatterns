package spike.labels;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import classifier.ClassificationParameterID;

/**
 * @author Siva Venkadesh
 *
 * 3/2017
 */
public class SpikePatternClass {
	public static final Set<SpikePatternComponent>TRANSIENTS = new HashSet<>(); 
	public static final Set<SpikePatternComponent> SPIKING = new HashSet<>();
	public static final Set<SpikePatternComponent> BURSTING = new HashSet<>();
	public static final Set<SpikePatternComponent> STUTTERING = new HashSet<>();
	static{
		TRANSIENTS.add(SpikePatternComponent.D);
		TRANSIENTS.add(SpikePatternComponent.ASP);
		TRANSIENTS.add(SpikePatternComponent.FASP);
		TRANSIENTS.add(SpikePatternComponent.TSTUT);
		TRANSIENTS.add(SpikePatternComponent.TSWB);
		TRANSIENTS.add(SpikePatternComponent.X);
		
		SPIKING.add(SpikePatternComponent.ASP);
		SPIKING.add(SpikePatternComponent.NASP);
		SPIKING.add(SpikePatternComponent.FASP);
		
		STUTTERING.add(SpikePatternComponent.TSTUT);
		BURSTING.add(SpikePatternComponent.TSWB);
		STUTTERING.add(SpikePatternComponent.PSTUT);
		BURSTING.add(SpikePatternComponent.PSWB);
		
	}
	
	private SpikePatternComponent[] components;	
	private static final int MAX_COMP = 5; //3 is the current max experimental length , pswb addition
	private int length;
	
	
	public Map<ClassificationParameterID, Double> classificationParameters;
	
	public SpikePatternClass(){
		components = new SpikePatternComponent[MAX_COMP];
		length = 0;
		classificationParameters =  new HashMap<>();
		initClassificationParameters();
		
	}	
	
	private void initClassificationParameters(){
		ClassificationParameterID[] parmIDs = ClassificationParameterID.values();
		for(ClassificationParameterID parmID: parmIDs){
			classificationParameters.put(parmID, Double.NaN);
		}		
	}
	public void addClassificationParameter(ClassificationParameterID parmID, Double value){
		classificationParameters.put(parmID, value);
	}
	public SpikePatternClass(String patternClass, String compDelimitor){
		this();
		StringTokenizer st = new StringTokenizer(patternClass, compDelimitor);
		while(st.hasMoreTokens()){
			components[length++] = SpikePatternComponent.valueOf(st.nextToken());
		}
	}
	public void addComponent(SpikePatternComponent component){
		components[length++]=component;
	}
	public void removeLastAddedComponent(){
		components[length--]=null;		
	}
	public SpikePatternComponent[] getSpikePatternLabel(){
		return this.components;
	}
	public int getLength() {
		return length;
	}
	public boolean contains(SpikePatternComponent component){		
		for(int i=0;i<length;i++){
			if(components[i]==null){
				System.out.println(length+", "+i);
				System.out.println(components[0]+"."+components[1]+"."+components[2]);
			}
			if(components[i].equals(component))
				return true;
		}
		return false;
	}
	public boolean steadyStateReached(){
		if(TRANSIENTS.contains(this.components[length-1])){
			return false;
		}
		return true;
	}
	public boolean containsSWB(){
		for(int i=0;i<length;i++){
			if(BURSTING.contains(this.components[i])){
				return true;
			}			
		}
		return false;		
	}
	public boolean containsSTUT(){
		for(int i=0;i<length;i++){
			if(STUTTERING.contains(this.components[i])){
				return true;
			}			
		}
		return false;		
	}
	public boolean containsSP(){
		for(int i=0;i<length;i++){
			if(SPIKING.contains(this.components[i])){
				return true;
			}			
		}
		return false;		
	}
	public int getnPieceWiseParms() {
		int nPieceWiseParms=0;
		for(int i=0;i<length;i++){
			if(components[i].equals(SpikePatternComponent.ASP)){
				nPieceWiseParms += 2;
				continue;
			}
			if(components[i].equals(SpikePatternComponent.NASP)){
				nPieceWiseParms += 1;
				continue;
			}
			if(components[i].equals(SpikePatternComponent.X)){
				nPieceWiseParms += 1;
			}
		}
	return nPieceWiseParms;
	}
	// ignore X for excel sheet!
	public void display(){
		for(int i=0;i<length;i++){
			if(!components[i].equals(SpikePatternComponent.X)){
				if(!components[i].equals(SpikePatternComponent.EMPTY)){
					System.out.print(components[i]);
					if(TRANSIENTS.contains(components[i])){
						System.out.print(".");
					}
				}else{
					System.out.print("*No_data");
				}
			}			
		}			
	}
	// ignore X for excel sheet!
	public void display(ClassificationParameterID[] parmIDs){
		for(int i=0;i<length;i++){
			if(!components[i].equals(SpikePatternComponent.X)){
				if(!components[i].equals(SpikePatternComponent.EMPTY)){
					System.out.print(components[i]);
					if(TRANSIENTS.contains(components[i])){
						System.out.print(".");
					}
				}else{
					System.out.print("*No_data");
				}
			}			
		}
		System.out.print("\t");
		
		for(ClassificationParameterID parmID:parmIDs){
			if(classificationParameters.get(parmID).isNaN()){
				System.out.print("no data\t");
			}else{
				System.out.print(classificationParameters.get(parmID)+"\t");
			}
		}
		
	}
	
	
	public boolean equals(SpikePatternClass targetClass){
		if(targetClass.length != this.length)
			return false;
		
		for(int i=0;i<length;i++){
			if(!components[i].equals(targetClass.components[i]))
				return false;
		}
		return true;		
	}
	
	public void replaceWithPSTUT(){
		if(this.contains(SpikePatternComponent.D)){
			length = 0;
			addComponent(SpikePatternComponent.D);
			addComponent(SpikePatternComponent.PSTUT);
		}else{
			length = 0;			
			addComponent(SpikePatternComponent.PSTUT);
		}
	}
	public void replaceWithPSWB(){
		if(this.contains(SpikePatternComponent.D)){
			length = 0;
			addComponent(SpikePatternComponent.D);
			addComponent(SpikePatternComponent.PSWB);
		}else{
			length = 0;			
			addComponent(SpikePatternComponent.PSWB);
		}
	}
	
	public String toString(){
		String str = "";
		for(int i=0;i<length;i++){
			str+= components[i]+".";
		}
		return str;
	}
}
