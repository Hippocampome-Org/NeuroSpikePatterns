package spike;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import util.StatUtil;


/**
 * @author Siva Venkadesh
 *
 * 3/2017
 */
public class SpikePatternAdapting extends SpikePattern{

	private int sfaISIs;	
	
	public SpikePatternAdapting() {
		super();
		// TODO Auto-generated constructor stub
	}
	public SpikePatternAdapting(ModelSpikePatternData spikePatternData, double current, double timeMin, double durationOfCurrent, double vR) {
		super(spikePatternData, current, timeMin, durationOfCurrent, vR);
		if(this.ISIs!=null) {
			this.ISI0 = getFirstISI();
			this.frequency0 = getFirstFrequency();
		}
	}
	
	public SpikePatternAdapting(double[] spikeTimes, double currentInjected, double timeMin, double durationOfCurrentInjected){
		super(spikeTimes, currentInjected, timeMin, durationOfCurrentInjected);
	}
	private double ISI0;
	private double frequency0;
	
	private double getFirstISI(){
		return this.getISIs()[0];
	}
	
	private double getFirstFrequency() {
		double firstISI = getFirstISI() * 0.001;
		return 1.0d/firstISI;
	}
	
	public double getISI(int idx){
		return this.getISIs()[idx];
	}

	public double[] getISIsStartingFromIdx(int startIdx){		
		double[] isis = null;
		if(startIdx < ISIs.length){
			if(this.ISIs!=null){
				isis = new double[ISIs.length-startIdx];
				for(int i=0; i<ISIs.length; i++) {				
					if(i>=startIdx){
						isis[i-startIdx] = ISIs[i];	
					}												
				}
			}					
		}
		return isis;
	}
	public double[] getNisisStartingFromIdx(int startIdx, int n){		
		double[] isis = null;
		if(startIdx < ISIs.length){
			int N = ((startIdx+n) <= ISIs.length)? n: (ISIs.length-startIdx);
			if(this.ISIs!=null){
				isis = new double[N];
				for(int i=startIdx; i<startIdx+N; i++) {						
					isis[i-startIdx] = ISIs[i];	
				}
			}					
		}
		return isis;
	}
	public double[] getFirstNISIs(int n) {			
		double[] firstNIsis = null;
		if(ISIs!=null) {
			int max_length = (ISIs.length < n)? ISIs.length : n;
			firstNIsis = new double[max_length];
			for(int i=0; i<firstNIsis.length; i++) {
				firstNIsis[i] = ISIs[i];
			}
		}
		return firstNIsis;
	}
	/*
	 * n not needed for now!
	 */
	public double[] getFirstNISIs2(int previous_n, int n) {			
		double[] firstNIsis = null;
		if(ISIs!=null && ISIs.length>previous_n) {				
			firstNIsis = new double[ISIs.length-previous_n];
			for(int i=previous_n; i<ISIs.length; i++) {
				firstNIsis[i-previous_n] = ISIs[i];
			}
		}		
		return firstNIsis;
	}
	
	

	public double[][] getFirstNISIsAndTheirLatenciesToSecondSpike(int n) {
		double[] ISILatency = null;
		double[] firstNISIs = getFirstNISIs(n);
		if(firstNISIs!=null) {				
			ISILatency = new double[firstNISIs.length];
			double LatencysoFar = this.getFSL();
			for(int i=0; i<ISILatency.length; i++) {
				LatencysoFar += firstNISIs[i];
				ISILatency[i] = LatencysoFar;								
			}
		}
		return new double[][]{ISILatency, firstNISIs};
	}
	public double[] getISILatenciesToSecondSpike(){
		double[] latencies = null;
		if(this.ISIs!=null){
			latencies = new double[ISIs.length];
			double latSoFar = this.getFSL();
			for(int i=0; i<latencies.length; i++) {
				latSoFar += ISIs[i];
				latencies[i] = latSoFar;								
			}
		}		
		return latencies;
	}
	public double[] getISILatenciesToSecondSpike(int startISIidx){
		double[] latencies = null;
		if(this.ISIs!=null){
			latencies = new double[ISIs.length-startISIidx];
			double latSoFar = this.getFSL();
			for(int i=0; i<ISIs.length; i++) {
				latSoFar += ISIs[i];
				if(i>=startISIidx){
					latencies[i-startISIidx] = latSoFar;	
				}												
			}
		}		
		return latencies;
	}
	
		public double[][] getFirstNISIsAndTheirLatenciesForRegression(int n) {
			double[][] xy = null;
			double[] firstNISIs = getFirstNISIs(n);
			if(firstNISIs!=null) {				
				xy = new double[firstNISIs.length][2];
				double LatencysoFar = this.getFSL();
				for(int i=0; i<xy.length; i++) {
					LatencysoFar += firstNISIs[i];
					xy[i][0] = LatencysoFar;	
					xy[i][1] = firstNISIs[i];
				}
			}
			return xy;
		}
		
		public double[][] getFirstNISIsAndTheirLatenciesForRegression2(int previous_nsfaISIs, int n) {
			double[][] xy = null;
			double[] firstNISIs = getFirstNISIs2(previous_nsfaISIs, n);	
			
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
	
	public double[] getFirstNIsiLatencySlopes(int n) {
		double[] slopes = null;		
		double[] ISILatency = getFirstNISIsAndTheirLatenciesToSecondSpike(n+1)[0];
		
		if(ISILatency!=null) {				
			slopes = new double[ISILatency.length-1];
			for(int i=0; i<slopes.length; i++){
				slopes[i] = StatUtil.calculateSlope(ISILatency[i] , ISIs[i], ISILatency[i+1], ISIs[i+1]);
			}
		}
		return slopes;
	}
	/*
	 * slope of the linear regression fit
	 */
	public double calculateSfa() {
		return getSimpleRegression().getSlope();
		//getSimpleRegression().
	}
	public double calculateSfa(int n) {
		//double[][] xy = getFirstNISIsAndTheirLatenciesToSecondSpike(n);		
		//return StatUtil.calculateSlopeOfRegression(xy[0], xy[1]);
		return getSimpleRegression(n).getSlope();
	}
	
	public double calculateSfa2(int pre_n, int n) {
		//double[][] xy = getFirstNISIsAndTheirLatenciesToSecondSpike(n);		
		//return StatUtil.calculateSlopeOfRegression(xy[0], xy[1]);
		return getSimpleRegression2(pre_n, n).getSlope();
	}
	
	public double calculateSfaYintrcpt(int n) {
		//double[][] xy = getFirstNISIsAndTheirLatenciesToSecondSpike(n);		
		//return StatUtil.calculateSlopeOfRegression(xy[0], xy[1]);
		return getSimpleRegression(n).getIntercept();
	}
	
	public double calculateSfaYintrcpt2(int pre_n, int n) {
		//double[][] xy = getFirstNISIsAndTheirLatenciesToSecondSpike(n);		
		//return StatUtil.calculateSlopeOfRegression(xy[0], xy[1]);
		return getSimpleRegression2(pre_n, n).getIntercept();
	}
	public SimpleRegression getSimpleRegression() {
		return getSimpleRegression(sfaISIs);
	}
	public SimpleRegression getSimpleRegression(int n) {
		double[][] xy = getFirstNISIsAndTheirLatenciesForRegression(n);
		
		SimpleRegression sr = new SimpleRegression();
		sr.addData(xy);
		return sr;
	}
	public SimpleRegression getSimpleRegression2(int pre_n, int n) {
		double[][] xy = getFirstNISIsAndTheirLatenciesForRegression2(pre_n, n);
		
		SimpleRegression sr = new SimpleRegression();
		sr.addData(xy);
		return sr;
	}
	public double getLinearFitCoeffOfDeter(){
		return getSimpleRegression().getRSquare();
	}
	
	
	
	public double getISI0(){return ISIs[0];}
	public double getISILast() {return ISIs[ISIs.length-1];}
	public double getFrequency0() {return frequency0;}
	
		
	public static void main(String[] args) {
		
	}
	public int getSfaIsis() {
		return sfaISIs;
	}
	public void setSfaSpikes(int sfaSpikes) {
		this.sfaISIs = sfaSpikes;
	}
	public ModelSpikePatternData getModelSpikePatternData(){
		return spikePatternData;
		
	}
}
