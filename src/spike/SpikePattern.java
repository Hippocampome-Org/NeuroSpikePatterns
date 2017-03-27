package spike;

import java.util.ArrayList;

//import ec.app.izhikevich.model.Izhikevich9pModel;

import util.GeneralUtils;
import util.StatUtil;


/**
 * @author Siva Venkadesh
 *
 * 3/2017
 */
public class SpikePattern {
	public static final double V_BELOW_ALLOWED_OFFSET_FROM_VMIN_DURING_SPIKE = 50;
	public static final double 	ALLOWED_OFFSET_FROM_VREST = 50;
	//core attributes
	private int noOfSpikes;
	protected double averageFrequency;
	protected double averageISI;  // temporary	
	private double minISI;
	private double maxISI;	
	protected double[] spikeTimes = null;	
	protected double[] ISIs = null;
	
	//EP
	protected double currentInjected;
	protected double timeMin;
	protected double durationOfCurrentInjection;
	
	protected double vR;	
	protected ModelSpikePatternData spikePatternData;
	
	private BurstPattern burstPattern;
	private double swa;
	
	public SpikePattern(){
		
	}
	public SpikePattern(ModelSpikePatternData spikePatternData, double current, double timeMin, double durationOfCurrent, double vR) {		
		this(spikePatternData.getSpikeTimes(), current, timeMin, durationOfCurrent);
		this.vR = vR;
		this.setSpikePatternData(spikePatternData);
	}
	
	/*
	 * Initializer for Experimental spike through spike Times
	 */	
	public SpikePattern(double[] spikeTimes, double currentInjected, double timeMin, double durationOfCurrentInjected) {
		this.spikeTimes = spikeTimes;
		this.noOfSpikes = (spikeTimes==null)?0:spikeTimes.length;
		this.currentInjected = currentInjected;
		this.timeMin = timeMin;
		this.durationOfCurrentInjection = durationOfCurrentInjected;
		this.ISIs = calculateISIs();
		this.setMinISI(findMinISI());
		this.setMaxISI(findMaxISI());
		this.setAverageFrequency(calculateAverageFiringFrequency());	
		this.setAverageISI(calculateAverageISI());
	}
	
	
	public SpikePattern(double currentInjected, double timeMin, double durationOfCurrentInjected) {
		this.currentInjected = currentInjected;
		this.timeMin = timeMin;
		this.durationOfCurrentInjection = durationOfCurrentInjected;
	}
	//Members	
	private double[] calculateISIs() {
		if(spikeTimes.length <= 1) {
			if(spikeTimes.length < 1) return null;
			else return null;
		}
		double[] isi = new double[spikeTimes.length-1];
		for(int i=0;i<isi.length;i++){
			isi[i] = spikeTimes[i+1] - spikeTimes[i];
 		}
		return isi;
	}
	
	private double findMinISI() {
		double min_ISI = this.durationOfCurrentInjection;
		if(ISIs!=null) {
			for(double isi: ISIs){
				if(isi < min_ISI) { min_ISI = isi; }			
			}
		}
		return min_ISI;
	}
	
	private double findMaxISI() {
		double max_ISI = 0;
		if(ISIs!=null) {
			for(double isi: ISIs){
				if(isi > max_ISI) { max_ISI = isi; }			
			}
		}
		return max_ISI;
	}
	
	private double calculateAverageISI() {
		if(spikeTimes.length <= 1) {
			if(spikeTimes.length < 1) return 0;
			else return durationOfCurrentInjection;
		}
	//	double[] ISIs = this.getISIs();		
		//average isi's
		double total_isi = 0;
		for(int i=0;i<ISIs.length;i++) {
			total_isi += ISIs[i];
		}
		double avg_isi = total_isi/ISIs.length;
		return avg_isi;
	}
	
	private double calculateAverageFiringFrequency() {	
		if(spikeTimes.length <= 1) {
			if(spikeTimes.length < 1) return 0;
			else return 1.0d/(durationOfCurrentInjection*0.001);
		}
	//	double[] ISIs = this.getISIs();		
		//average isi's
		double total_freq = 0;
		for(int i=0;i<ISIs.length;i++) {
			total_freq += (1.0d/ISIs[i])*1000.0d;
		}
		double avg_freq = total_freq/(ISIs.length*1.0d);
		
		return avg_freq;
	}
	private double calculateAverageFiringFrequency(int first_N_ISIs) {	
		if(spikeTimes.length <= 1) {
			if(spikeTimes.length < 1) return 0;
			else return 1.0d/(durationOfCurrentInjection*0.001);
		}
		int n = (first_N_ISIs > ISIs.length-1)? (ISIs.length-1): first_N_ISIs;
	//	double[] ISIs = this.getISIs();		
		//average isi's
		double total_freq = 0;
		for(int i=0;i<=n;i++) {
			total_freq += (1.0d/ISIs[i])*1000.0d;
		}
		double avg_freq = total_freq/(n*1.0d);
		
		return avg_freq;
	}
	public double getFiringFrequencyBasedOnISIs(){		
		return calculateAverageFiringFrequency();
	}
	public double getFiringFrequencyBasedOnISIs(int first_N_ISIs){		
		return calculateAverageFiringFrequency(first_N_ISIs);
	}
	public double getFiringFrequencyBasedOnSpikesCount(){		
		if(spikeTimes.length < 1) 
			return 0;
		else 
			return (spikeTimes.length*1.0d)/(durationOfCurrentInjection*0.001);		
	}
	public double getFSL() {
		double delay = durationOfCurrentInjection;
		if(spikeTimes!=null &&spikeTimes.length>0) {
			delay = spikeTimes[0] - timeMin;
		}
		return delay;
	}
	
	public double getPSS() {
		double timeMax = timeMin + durationOfCurrentInjection;
		double delay = 0;
		if(spikeTimes!=null&&spikeTimes.length>0) {
			delay = timeMax - spikeTimes[spikeTimes.length-1];
		}
		return delay;
	}
	
	public double[] getSubSSVoltageSamples(int nSamples) {
		int sampleTimeStart = (int) (timeMin);
		int sampleTimeEnd = (int) (timeMin + durationOfCurrentInjection);		
		int[] sampleTimes = new int[nSamples];
		int sampleInterval = (sampleTimeEnd - sampleTimeStart) / (nSamples + 1);
		sampleTimes[0] = sampleTimeStart + sampleInterval;
		for(int i=1; i<nSamples; i++) {
			sampleTimes[i] = sampleTimes[i-1] + sampleInterval;
		}
		double[] sampleVolOffset = new double[sampleTimes.length];
		for(int i=0; i<sampleTimes.length; i++) {
			int volt_indx = spikePatternData.getIndex(sampleTimes[i]);
			sampleVolOffset[i] = spikePatternData.getVoltage()[volt_indx] - vR;			
		}
		return sampleVolOffset;
	}
	
	
	
	public double getAsymptoticVoltage() {
		return getAsymptoticVoltage(5.0);
	}
	public double getAsymptoticVoltage(double dvCriterion) {
		double asymVolt = Double.MAX_VALUE;
	
		double[] ssVoltSamples = getSubSSVoltageSamples(3);
		double mean = StatUtil.calculateMean(ssVoltSamples);
		double sd = StatUtil.calculateStandardDeviation(ssVoltSamples, mean);
	
		if(sd<dvCriterion) {
			return mean;
		}else{
			return asymVolt;
		}
	}
	public double getTimeConstant() {
		if(this.noOfSpikes > 0) {
			return 0;
		}
		double asymVoltAmp = getAsymptoticVoltage();
		double timeToreach63perc = this.spikePatternData.getTimeToReach((asymVoltAmp * 0.632) + vR); // convert offset to absolute
	
		if(timeToreach63perc < Double.MAX_VALUE) {
			return  timeToreach63perc - this.timeMin;
		}else{
			return this.durationOfCurrentInjection;
		}
	}
	public boolean isValidDendriticPattern(double vOffset, double vR, double vPeak, double validMaxV) {
		
		return isValidSubSSVoltPattern(vOffset, vR, vPeak, validMaxV);
	}
	
	public boolean isValidPattern(double vOffset, double vR, double vPeak, double vMin, double validMaxV) {		
	
		return isValidSubSSVoltPattern(vOffset, vR, vPeak, validMaxV);
	}
	
	
	
	public boolean isValidSpikesPattern(double vMin, double vRest) {		
		
		if(spikeTimes.length < 1 ||
				(spikeTimes[spikeTimes.length-1] - this.timeMin) > (this.durationOfCurrentInjection+10)) {
			
			return false;
		}	
		double[] voltage = spikePatternData.getVoltage();
				
		int idxMin = spikePatternData.getIndex(spikeTimes[0]);
		int idxMax = spikePatternData.getIndex(spikeTimes[spikeTimes.length-1]);		
		for(int i=0;i<voltage.length;i++) {				
			//2. voltage doesn't go below vMin, between first spike and last spike
			if(i>=idxMin && i<=idxMax) {
				if(voltage[i]<vMin && !GeneralUtils.isCloseEnough(voltage[i], vMin, V_BELOW_ALLOWED_OFFSET_FROM_VMIN_DURING_SPIKE)){
					
					return false;
				}								
			}
			
			//1. voltage doesn't go below vRest at all times.
			if(voltage[i]<vRest && !GeneralUtils.isCloseEnough(voltage[i], vRest, ALLOWED_OFFSET_FROM_VREST))	{
				
				return false;
			}
			
			//3.if spike generated before or after the duration of current injection:
			if(spikeTimes[0] < timeMin || spikeTimes[spikeTimes.length - 1] > timeMin + durationOfCurrentInjection)
			{
			
				return false;
			}
		}
		
		return true;
	}		
	
	public boolean isValidSpikesPattern() {		
		
		if(spikeTimes.length < 1 ||
				(spikeTimes[spikeTimes.length-1] - this.timeMin) > (this.durationOfCurrentInjection+50)) {
			
			return false;
		}			
		return true;
	}		
	public double calculateAvgBelowVmin(double vMin) {
		double[] voltage = spikePatternData.getVoltage();
		
		int idxMin = spikePatternData.getIndex(spikeTimes[0]);
		int idxMax = spikePatternData.getIndex(spikeTimes[spikeTimes.length-1]);
		double voltageBelowVmin =0;
		double count = 0;
		
		for(int i=0;i<voltage.length;i++) {	
			if(i>=idxMin && i<=idxMax) {
				if(voltage[i]<vMin){
					voltageBelowVmin+= Math.abs(vMin - voltage[i]);
					count +=1;
				}								
			}						
		}
		if(count < 0.5) return 0;
		return voltageBelowVmin/count;
	}
	
	public double calculateAvgBelowVminAfterLastSpike(double vMin, double forDuration) {
		double[] voltage = spikePatternData.getVoltage();		
		
		int idxMin = spikePatternData.getIndex(spikeTimes[spikeTimes.length-1]);
		double timeMax = spikeTimes[spikeTimes.length-1] +forDuration;
		if(timeMax > timeMin+durationOfCurrentInjection){
			timeMax = timeMin+durationOfCurrentInjection;
		}
		int idxMax = spikePatternData.getIndex(timeMax);
		double voltageBelowVmin =0;
		double count = 0;
		
		for(int i=0;i<voltage.length;i++) {	
			if(i>=idxMin && i<=idxMax) {
				if(voltage[i]<vMin){
					voltageBelowVmin+= Math.abs(vMin - voltage[i]);
					count +=1;
				}								
			}						
		}
		if(count < 0.5) return 0;
		return voltageBelowVmin/count;
	}
	
	public double calculateAvgBelowVrest(double vRest) {
		double[] voltage = spikePatternData.getVoltage();
		double voltageBelowVrest =0;
		double count = 0;
		
		for(int i=0;i<voltage.length;i++) {	
			if(voltage[i]<vRest)	{
				voltageBelowVrest += Math.abs(vRest - voltage[i]);
				count += 1;
			}
		}	
		if(count < 0.5) return 0;
		return voltageBelowVrest/count;
	}
	
	public boolean isValidSubSSVoltPattern(double vOffset, double vRest, double vPeak, double validMaxV) {
		
		double[] voltage = spikePatternData.getVoltage();
			
		for(int i=0;i<voltage.length;i++) {				
			
			//1. voltage doesn't go far below vRest 
			if(voltage[i]<vRest && !GeneralUtils.isCloseEnough(voltage[i], vRest-Math.abs(vOffset), 50))	{
				return false;
			}
			//2. voltage doesn't go up above vpeak 
			if(voltage[i]>vPeak && !GeneralUtils.isCloseEnough(voltage[i], vPeak, validMaxV))	{
				return false;
			}
			
		}		
		return true;
	}		
	
	public double[] getISIsAfterNISIs(int n) {
		double[] afterNISIs = null;
		if(this.ISIs.length <= n) {
			return afterNISIs;
		}else{
			afterNISIs = new double[this.ISIs.length - n];
			for(int i=0; i<afterNISIs.length; i++) {
				afterNISIs[i] = this.ISIs[i+n];
			}
		}return afterNISIs;
	}
	
	public void initBurstPattern(double diffFactor){
		this.burstPattern = new BurstPattern(ISIs, diffFactor, 1.5);
	}
	public ArrayList<ArrayList<Double>> getISIsDuringBurst(){
		return burstPattern.getISIsDuringBurst();
	}
	public ArrayList<Double> getIBIs(){
		return burstPattern.getIBIs();
	}
	
		public int getNoOfSpikes() {
			return noOfSpikes;
		}
		public void setNoOfSpikes(int noOfSpikes) {
			this.noOfSpikes = noOfSpikes;
		}
		public double getAverageFrequency() {
			return averageFrequency;
		}
		public void setAverageFrequency(double avgFrequency) {
			this.averageFrequency = avgFrequency;
		}
		public double[] getSpikeTimes() {
			return spikeTimes;
		}
		public void setSpikeTimes(double[] spikeTimes) {
			this.spikeTimes = spikeTimes;
		}
		public double getCurrentInjected() {
			return currentInjected;
		}
		public void setCurrentInjected(double currentInjected) {
			this.currentInjected = currentInjected;
		}
		public double getDurationOfCurrentInjection() {
			return durationOfCurrentInjection;
		}
		public void setDurationOfCurrentInjection(double durationOfCurrentInjection) {
			this.durationOfCurrentInjection = durationOfCurrentInjection;
		}

		public ModelSpikePatternData getSpikePatternData() {
			return spikePatternData;
		}

		public void setSpikePatternData(ModelSpikePatternData spikePatternData) {
			this.spikePatternData = spikePatternData;
		}
		
		public void setAverageISI(double averageISI) {this.averageISI = averageISI;	}
		public double getAverageISI() {return this.averageISI;}

	public void display() {
		System.out.println(this.currentInjected + "\t" +this.averageFrequency +"\t"+this.noOfSpikes+"\t"+this.timeMin+"\t"+this.durationOfCurrentInjection);
		GeneralUtils.displayArraySameLine(spikeTimes);
	}
	public void displayISIs(){
		GeneralUtils.displayArray(this.ISIs);
	}

	public double getMinISI() {
		return minISI;
	}

	public void setMinISI(double minISI) {
		this.minISI = minISI;
	}

	public double getMaxISI() {
		return maxISI;
	}

	public void setMaxISI(double maxISI) {
		this.maxISI = maxISI;
	}

	public double[] getISIs() { return this.ISIs; }
	public double getTimeMin() { return this.timeMin; }
		
	public BurstPattern getBurstPattern(){
		return this.burstPattern;
	}
	public double getSwa() {
		return swa;
	}
	public void setSwa(double swa) {
		this.swa = swa;
	}
	
}
