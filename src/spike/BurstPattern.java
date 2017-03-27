package spike;

import java.util.ArrayList;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import util.StatUtil;

/**
 * @author Siva Venkadesh
 *
 * 3/2017
 */
public class BurstPattern {
	
	private ArrayList<ArrayList<Double>> ISIsDuringBurst;
	private ArrayList<Double> IBIs;
	
	
	public BurstPattern(double[] ISIs, double diffFactorPre, double diffFactorPost) {		
		if(ISIs!=null){
			ISIsDuringBurst = new ArrayList<>();
			IBIs = new ArrayList<>();
		
			ArrayList<Double> burstISI = new ArrayList<>();
			boolean isIBI = false;
			for(int i=0;i<ISIs.length;i++){					
					
				double fac1 = 0;
				double fac2 = 0;
				if(i>0)
					fac1 = ISIs[i] / ISIs[i-1];
				if(i<ISIs.length-1)
					fac2 = ISIs[i] / ISIs[i+1];
				
				if((fac1 > diffFactorPre || i==0) 
						&& (fac2 > diffFactorPost || i==ISIs.length-1)) 						
					isIBI = true;
										
				
				if(!isIBI){
					burstISI.add(ISIs[i]);
				}
				if(isIBI){
					ISIsDuringBurst.add(burstISI);
					burstISI = new ArrayList<>();
					IBIs.add(ISIs[i]);	
					isIBI= false;
				}					
			}			
			ISIsDuringBurst.add(burstISI);
		}		
	}
	
	public boolean isBurst(){
		if(getNBursts()>1) 
			return true;
		/*int realBurstCnt = 0;
		for(ArrayList<Double> burst: ISIsDuringBurst){
			if(burst.size()>0)
				realBurstCnt++;
		}
		if(realBurstCnt>1)
			return true;*/
		return false;
	}

		
	private double[] getFirstNISIs(int burstIdx, int n) {			
		double[] firstNIsis = null;
		if(burstIdx<ISIsDuringBurst.size()){
			ArrayList<Double> ISIs = ISIsDuringBurst.get(burstIdx);
			if(!ISIs.isEmpty()) {
				int max_length = (ISIs.size() < n)? ISIs.size() : n;
				firstNIsis = new double[max_length];
				for(int i=0; i<firstNIsis.length; i++) {
					firstNIsis[i] = ISIs.get(i);
				}
			}
		}
		
		return firstNIsis;
	}
	
	public double[][] getFirstNISIsAndTheirLatenciesToSecondSpike(int burstIdx, int n) {
		double[] ISILatency = null;
		double[] firstNISIs = getFirstNISIs(burstIdx, n);
		if(firstNISIs!=null) {				
			ISILatency = new double[firstNISIs.length];
			double LatencysoFar = 0;
			for(int i=0; i<ISILatency.length; i++) {
				LatencysoFar += firstNISIs[i];
				ISILatency[i] = LatencysoFar;								
			}
		}
		return new double[][]{ISILatency, firstNISIs};
	}
	
	
	public double calculateSfa(int burstIdx, int n) {
		return getSimpleRegression(burstIdx,n).getSlope();
	}		
	public SimpleRegression getSimpleRegression(int burstIdx, int n) {
		double[][] xy = getFirstNISIsAndTheirLatenciesForRegression(burstIdx,n);		
		SimpleRegression sr = new SimpleRegression();
		sr.addData(xy);
		return sr;
	}
	public double[][] getFirstNISIsAndTheirLatenciesForRegression(int burstIdx, int n) {
		double[][] xy = null;
		double[] firstNISIs = getFirstNISIs(burstIdx, n);
		if(firstNISIs!=null) {				
			xy = new double[firstNISIs.length][2];
			double LatencysoFar = 0;
			for(int i=0; i<xy.length; i++) {
				LatencysoFar += firstNISIs[i];
				xy[i][0] = LatencysoFar;	
				xy[i][1] = firstNISIs[i];
			}
		}
		return xy;
	}
	
	public ArrayList<ArrayList<Double>> getISIsDuringBurst(){
		return ISIsDuringBurst;
	}
	public ArrayList<Double> getIBIs(){
		return IBIs;
	}
	public int getNSpikes(int burstIdx){
		if(burstIdx<ISIsDuringBurst.size()){
			//if(!ISIsDuringBurst.get(burstIdx).isEmpty())
			{
				return ISIsDuringBurst.get(burstIdx).size()+1;
			}
		}				
		else
			return -1;
	}
	public double getIBI(int burstIdx){
		if(burstIdx<IBIs.size())
			return IBIs.get(burstIdx);
		else
			return -1;
	}
	public double getBW(int burstIdx){
		double bw=-1;
		if(burstIdx<ISIsDuringBurst.size()&&
			!ISIsDuringBurst.get(burstIdx).isEmpty()){
			ArrayList<Double> isisDuringBurst = ISIsDuringBurst.get(burstIdx);		
			for(double isi:isisDuringBurst){
				bw+=isi;
			}
		}		
		return bw;
	}
	public int getNBursts(){
		return this.ISIsDuringBurst.size();
	}
	public double getPbiTimeMin(double timeMin, double fsl, int burstIdx){
		double accumDurs = timeMin + fsl;
		for(int i=0;i<=burstIdx; i++){
			if(i>0){
				accumDurs += this.IBIs.get(i-1); 
			}
			accumDurs += getBW(i);
		}
		return accumDurs;
	}
	public double getPbiTimeMax(double timeMin, double fsl, int burstIdx, double timeMax){
		if(burstIdx >= this.IBIs.size()){
			return timeMax;
		}
		return getPbiTimeMin(timeMin, fsl, burstIdx) + this.IBIs.get(burstIdx);
	}
	public void displayBursts(){
		System.out.println(ISIsDuringBurst);
		System.out.println(IBIs);
	}
	
	public double getIntraBurstAvgISI(int n){
		if(n>=ISIsDuringBurst.size()){
			throw new IllegalStateException();
		}
		ArrayList<Double> burst = this.ISIsDuringBurst.get(n);
		if(burst.size()==0) 
			return 0;
		return StatUtil.calculateMean(burst);
	}
	
	public double getAvgIntraBurstAvgISI(){
		int n = this.ISIsDuringBurst.size();
		int realN = 0;
		double IntraburstAvg = 0;
		for(int i=0;i<n;i++){
			if(this.ISIsDuringBurst.get(i).size() > 0){
				IntraburstAvg += StatUtil.calculateMean(this.ISIsDuringBurst.get(i));
				realN++;
			}			
		}
		return (IntraburstAvg/realN*1.0d);
	}
	
}
