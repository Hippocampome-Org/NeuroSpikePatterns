package classifier;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import spike.BurstPattern;
import spike.SpikePatternAdapting;
import spike.labels.SpikePatternClass;
import spike.labels.SpikePatternComponent;
import util.GeneralUtils;
import util.StatUtil;

/**
 * @author Siva Venkadesh
 *
 * 3/2017
 */
public class SpikePatternClassifier {
	public static boolean DISABLED = true;
	public static float SHADOW_FITNESS = 1;
	
	private SpikePatternAdapting somaSpikePattern;
	private SpikePatternClass patternClass;
	private SolverResultsStat[] solverStats;
	
	private static final double DELAY_FACTOR = 2d;
	private static final double SLN_FACTOR = 2d;
	
	private static final double PSTUT_PRE_FACTOR = 2.5d;
	private static final double PSTUT_POST_FACTOR = 2d;
	private static final double PSTUT_FACTOR = 5d;
	
	private static final double SWA_FACTOR = 5d;
	private static final double TSTUT_PRE_FACTOR = 2.5d;
	private static final double TSTUT_POST_FACTOR = 1.5d;
	private static final double MIN_TSTUT_FREQ = 25;
	
	private static final int TSTUT_ISI_CNT = 4;
	
	private static final double FAST_TRANSIENT_FACTOR = 1.5d;//2d;
	private static final int FAST_TRANS_ISI_CNT = 3;
	
	private static final double ADAPT_IDX = 0.28d;
	//private static final double ADAPT_IDX_FOR_SLOPE = 0.0d;
	
	private static final double SLOPE_THRESHOLD = 0.003d;
	private static final double SLOPE_THRESHOLD_FT = 0.2d;
	
	public SpikePatternClassifier(SpikePatternAdapting _somaPattern){
		this();
		this.somaSpikePattern = _somaPattern;
	}
	/*
	 * for experimental trace classifier
	 */
	public SpikePatternClassifier(){
		somaSpikePattern = null;
		patternClass = new SpikePatternClass();
		SolverResultsStat init = new SolverResultsStat(0, 0, 0, 0, new double[]{}, 0);
		solverStats = new SolverResultsStat[] {init,init,init,init};
	}
	
	public void classifySpikePattern(double swa, boolean isModel){		
		classifySpikePattern_EXP(swa, isModel);
	}
	
	/*
	 *              tstut, pstut checks after NASP
	 */
	public void classifySpikePattern_EXP(double swa, boolean isModel){
		/*
		 * 0. Check for valid ISIs
		 */	
		if(somaSpikePattern.getISIs()==null){
			patternClass.addComponent(SpikePatternComponent.EMPTY);
			return;
		}	
		/*
		 * I. check for Delay
		 *   if no isis OR fsl > criterion, it's a delay
		 */
		if(somaSpikePattern.getISIs().length == 0){		
			patternClass.addComponent(SpikePatternComponent.EMPTY);
			return;
		}
		if(hasDelay()){
			patternClass.addComponent(SpikePatternComponent.D);
		}	
		
		
		/*
		 * II. Check TSTUT
		 */
		int startIdxForTstut = 0;
		if(somaSpikePattern.getISIsStartingFromIdx(startIdxForTstut).length >= 1){			
			startIdxForTstut = startIdxForTSTUT(swa);
			if(startIdxForTstut>1){
				if(swa>SWA_FACTOR){
					patternClass.addComponent(SpikePatternComponent.TSWB);
				}else
					patternClass.addComponent(SpikePatternComponent.TSTUT);						
			}			
		}
		
		/*
		 * NEW: FAST TRANSIENT ADDITION!!
		 *    //  - tstut not satisfied!, make sure you are not missing the fast adaptation!
		 */
		
	
		if(somaSpikePattern.getISIsStartingFromIdx(startIdxForTstut)!=null){
			if(somaSpikePattern.getISIsStartingFromIdx(startIdxForTstut).length == 1){
				patternClass.addComponent(SpikePatternComponent.X);
			}
			if(somaSpikePattern.getISIsStartingFromIdx(startIdxForTstut).length > 1){
				/*
				 * III. Classify - Stat Tests
				 */
				classifySpikes_AK(startIdxForTstut, isModel);			
				if(!patternClass.contains(SpikePatternComponent.ASP) 
						|| isModel){
					
					/*
					 * Check PSTUT
					 */
					int startIdxForPSTUT = 0;
					if(startIdxForTstut>0){
						startIdxForPSTUT = startIdxForTstut-1;
					}
					if(hasPSTUT(startIdxForPSTUT))
					//if(hasSTUT(startIdxForPSTUT))	
					{
						patternClass.removeLastAddedComponent(); //remove NASP
						if(patternClass.contains(SpikePatternComponent.TSTUT) ||
								patternClass.contains(SpikePatternComponent.TSWB)){
							patternClass.removeLastAddedComponent();//remove TSTUT
						}
						//if(isPSTUT())
						{
							if(swa>SWA_FACTOR){
								patternClass.addComponent(SpikePatternComponent.PSWB);
							}else
								patternClass.addComponent(SpikePatternComponent.PSTUT);
						}
													
					}
					
					/*
					 * if no TSTUT, detect Fast transient and reclassify!
					 */
					if((!isModel || !patternClass.contains(SpikePatternComponent.ASP))&&
						!patternClass.contains(SpikePatternComponent.TSTUT) && 
							!patternClass.contains(SpikePatternComponent.TSWB)&&
								!patternClass.contains(SpikePatternComponent.PSTUT) && 
									!patternClass.contains(SpikePatternComponent.PSWB)&&										
									(somaSpikePattern.getISIsStartingFromIdx(startIdxForTstut).length >= FAST_TRANS_ISI_CNT)){			
						
						int fastTransIdx =classifyFastTransientStat(FAST_TRANS_ISI_CNT);// classifyFastTransientSimple();		//
						if(fastTransIdx>0 && somaSpikePattern.getISIsStartingFromIdx(fastTransIdx)!=null){								
							
							if(somaSpikePattern.getISIsStartingFromIdx(fastTransIdx-1).length > 2){
								classifySpikes_AK(fastTransIdx-1, isModel);
							}
						}							
					}					
				}
			}
		}
		
		/*
		 * III. check for SLN 
		 */
		if(hasPostSLN()){
			if(patternClass.steadyStateReached()){
				if(swa > SWA_FACTOR)
					patternClass.replaceWithPSWB();
				else
					patternClass.replaceWithPSTUT();
			}else{
				patternClass.addComponent(SpikePatternComponent.SLN);
			}			
		}
	}
	
	
	private int startIdxForTSTUT(double swa) {
		int startIdx = 0;
		double[] isis = somaSpikePattern.getISIs();
		double[] isisAndPss = new double[isis.length+1];
		for(int i=0;i<isis.length;i++){
			isisAndPss[i]=isis[i];
		}
		isisAndPss[isisAndPss.length-1] = somaSpikePattern.getPSS();
		int maxIdxForCheck = ((TSTUT_ISI_CNT>(isisAndPss.length-2))? (isisAndPss.length-2) : TSTUT_ISI_CNT);
		
		/*
		 * to avoid X.SLN, if only one ISI present, ignore 
		 */
		if(maxIdxForCheck == 0){
			if(isisAndPss[1]>isisAndPss[0]*TSTUT_PRE_FACTOR){
				double[] isisPre = GeneralUtils.getFirstNelements(isisAndPss, 1);
				double[] isisPost = GeneralUtils.getLastNelements(isisAndPss, 1);
				if(StatUtil.calculateMean(isisPost) > StatUtil.calculateMean(isisPre)*TSTUT_PRE_FACTOR &&
						somaSpikePattern.getFiringFrequencyBasedOnISIs(1)>MIN_TSTUT_FREQ){
					startIdx = 2;	
					return startIdx;
			}
		}
		}
		/*
		 * another sneak-in for tswb.sln (with 2 ISIs)-- without swa this would NOT be a tstut.
		 */
		if(swa>=SWA_FACTOR){
			if(isisAndPss.length<=5){
				if(isisAndPss[isisAndPss.length-1] > isisAndPss[isisAndPss.length-2]*TSTUT_PRE_FACTOR){
					return isisAndPss.length-1;
				}
			}
		}
		for(int i=1;i<=maxIdxForCheck;i++){
			if(isisAndPss[i]>isisAndPss[i-1]*TSTUT_PRE_FACTOR
					&& (isisAndPss[i] > isisAndPss[i+1]*TSTUT_POST_FACTOR 
							|| 
							swa >= SWA_FACTOR
						)
				){
				double[] isisPre = GeneralUtils.getFirstNelements(isisAndPss, i);
				double[] isisPost = GeneralUtils.getLastNelements(isisAndPss, i);
				if(StatUtil.calculateMean(isisPost) > StatUtil.calculateMean(isisPre)*TSTUT_PRE_FACTOR &&
						somaSpikePattern.getFiringFrequencyBasedOnISIs(i)>MIN_TSTUT_FREQ){
					startIdx = i+1;
					break;
				}				
			}
		}
		return startIdx;
	}
	
	
	/*
	 * Delay > 2*ISIavg(1,2)
	 */
	private boolean hasDelay(){
		double isiFilt=somaSpikePattern.getISI0();
		//System.out.println(isiFilt);	
		if(somaSpikePattern.getISIs().length>1){
			isiFilt = (somaSpikePattern.getISI0()+somaSpikePattern.getISI(1))/2d;
		}		
		//System.out.println(isiFilt);
		if(somaSpikePattern.getFSL() > DELAY_FACTOR*isiFilt){					
			return true;
		}else
			return false;
	}
	
	/*
	 * look at last 2 isis, and max isis
	 */
	private boolean hasPostSLN(){
		double isiFilt=somaSpikePattern.getISILast();
		if(somaSpikePattern.getISIs().length>1){
			isiFilt = (somaSpikePattern.getISILast()+
					somaSpikePattern.getISI(somaSpikePattern.getISIs().length-2))
					/2d;
		}		
		if(somaSpikePattern.getPSS() > SLN_FACTOR * isiFilt &&
				somaSpikePattern.getPSS() > SLN_FACTOR * somaSpikePattern.getMaxISI()){					
			return true;
		}else
			return false;
	}

	
	private void classifySpikes_AK(int n, boolean isModel){
		solverStats = new SolverResultsStat[4];
		
		double[] latencies = somaSpikePattern.getISILatenciesToSecondSpike(n);
		double[] ISIs = somaSpikePattern.getISIsStartingFromIdx(n);
		
		
		double scaleFactor = GeneralUtils.findMin(ISIs);
		double[] X = StatUtil.shiftLeftToZeroAndScaleSimple(latencies, scaleFactor);
		double[] Y = StatUtil.scaleSimple(ISIs, scaleFactor);
		
		if(StatAnalyzer.display_stats){
			GeneralUtils.display2DArrayVerticalUnRounded(new double[][]{X,Y});
		}
		LeastSquareSolverUtil l = new LeastSquareSolverUtil(X, Y);
		
		/*
		 * 1. SSF
		 */
		
		solverStats[0] = l.solveFor1Parm(1);			
		solverStats[1] = l.solveFor2Parms(0, 1);	
		solverStats[2] = l.solveFor3Parms(solverStats[1].getM1(), 1, 1);
		solverStats[3] = l.solveFor4Parms(solverStats[2].getM1(), 1, 0, 1);
		
		populateSolverSlopes();
		
	
		double[] f_p_stats = new double[4];
		for(int i=0;i<4;i++){
			f_p_stats[i]=Double.NaN;
		}
		if(!StatAnalyzer.isSignificantImprovement(solverStats[0].getFitResidualsAbs(), solverStats[1].getFitResidualsAbs(), 1, 2, f_p_stats)) {	
			// model requires 1->3 parm check!
			if(!(isModel)||
					!StatAnalyzer.isSignificantImprovement(solverStats[0].getFitResidualsAbs(), solverStats[2].getFitResidualsAbs(), 1, 3, f_p_stats)){
				patternClass.addComponent(SpikePatternComponent.NASP);
				populateFPStats(f_p_stats, 1);
				return;
			}
			patternClass.addComponent(SpikePatternComponent.ASP);
			patternClass.addComponent(SpikePatternComponent.NASP);
			populateFPStats(f_p_stats, 3);
			return;
						
		}
		/*
		 * 2. ASP
		 */	
		populateFPStats(f_p_stats, 1);
		f_p_stats = new double[4];
		for(int i=0;i<4;i++){
			f_p_stats[i]=Double.NaN;
		}
		if(!StatAnalyzer.isSignificantImprovement(solverStats[1].getFitResidualsAbs(), solverStats[2].getFitResidualsAbs(), 2, 3, f_p_stats)) {
			if(solverStats[1].getM1() > SLOPE_THRESHOLD){				
				patternClass.addComponent(SpikePatternComponent.ASP);
			}else{
				patternClass.addComponent(SpikePatternComponent.NASP);
			}	
			populateFPStats(f_p_stats, 2);
			return;
		}			
		/*
		 * 3. ASP.NASP / ASP.ASP
		 */	
		populateFPStats(f_p_stats, 2);
		f_p_stats = new double[4];
		for(int i=0;i<4;i++){
			f_p_stats[i]=Double.NaN;
		}
		if(!StatAnalyzer.isSignificantImprovement(solverStats[2].getFitResidualsAbs(), solverStats[3].getFitResidualsAbs(), 3, 4, f_p_stats)){				
				patternClass.addComponent(SpikePatternComponent.ASP);
				patternClass.addComponent(SpikePatternComponent.NASP);	
				populateFPStats(f_p_stats, 3);
			return;
		}else{
			patternClass.addComponent(SpikePatternComponent.ASP);			
			if(solverStats[3].getM2() > SLOPE_THRESHOLD){
				patternClass.addComponent(SpikePatternComponent.ASP);
			}else{
				patternClass.addComponent(SpikePatternComponent.NASP);	
			}
			populateFPStats(f_p_stats, 3);
			return;
		}
	}
private void populateSolverSlopes(){
	patternClass.addClassificationParameter(ClassificationParameterID.B_1p, solverStats[0].getC1());
	
	patternClass.addClassificationParameter(ClassificationParameterID.M_2p, solverStats[1].getM1());
	patternClass.addClassificationParameter(ClassificationParameterID.B_2p, solverStats[1].getC1());
	
	patternClass.addClassificationParameter(ClassificationParameterID.M_3p, solverStats[2].getM1());
	patternClass.addClassificationParameter(ClassificationParameterID.B1_3p, solverStats[2].getC1());
	patternClass.addClassificationParameter(ClassificationParameterID.B2_3p, solverStats[2].getC2());
	patternClass.addClassificationParameter(ClassificationParameterID.N_ISI_cut_3p, 
				solverStats[2].getBreakPoint()<0?0:(double)solverStats[2].getBreakPoint());
	
	patternClass.addClassificationParameter(ClassificationParameterID.M1_4p, solverStats[3].getM1());
	patternClass.addClassificationParameter(ClassificationParameterID.B1_4p, solverStats[3].getC1());
	patternClass.addClassificationParameter(ClassificationParameterID.M2_4p, solverStats[3].getM2());
	patternClass.addClassificationParameter(ClassificationParameterID.B2_4p, solverStats[3].getC2());
	patternClass.addClassificationParameter(ClassificationParameterID.N_ISI_cut_4p,
			solverStats[3].getBreakPoint()<0?0:(double)solverStats[3].getBreakPoint());	
}
private void populateFPStats(double[] f_p_stats, int level){
	/*F_12, F_crit_12, F_23, F_crit_23, F_34, F_crit_34,
	P_12, P_12_UV, P_23, P_23_UV, P_34, P_34_UV,
	*/
	if(level ==1){
		patternClass.addClassificationParameter(ClassificationParameterID.F_12, f_p_stats[0]);
		patternClass.addClassificationParameter(ClassificationParameterID.F_crit_12, f_p_stats[1]);
		patternClass.addClassificationParameter(ClassificationParameterID.P_12, f_p_stats[2]);
		patternClass.addClassificationParameter(ClassificationParameterID.P_12_UV, f_p_stats[3]);
	}
	if(level ==2){
		patternClass.addClassificationParameter(ClassificationParameterID.F_23, f_p_stats[0]);
		patternClass.addClassificationParameter(ClassificationParameterID.F_crit_23, f_p_stats[1]);
		patternClass.addClassificationParameter(ClassificationParameterID.P_23, f_p_stats[2]);
		patternClass.addClassificationParameter(ClassificationParameterID.P_23_UV, f_p_stats[3]);
	}
	if(level ==3){
		patternClass.addClassificationParameter(ClassificationParameterID.F_34, f_p_stats[0]);
		patternClass.addClassificationParameter(ClassificationParameterID.F_crit_34, f_p_stats[1]);
		patternClass.addClassificationParameter(ClassificationParameterID.P_34, f_p_stats[2]);
		patternClass.addClassificationParameter(ClassificationParameterID.P_34_UV, f_p_stats[3]);
	}
	
}
public void calculateAdaptationForNonSP(int n){
		solverStats = new SolverResultsStat[4];		
		double[] latencies = somaSpikePattern.getISILatenciesToSecondSpike(n);
		double[] ISIs = somaSpikePattern.getISIsStartingFromIdx(n);		
		
		double scaleFactor = GeneralUtils.findMin(ISIs);
		double[] X = StatUtil.shiftLeftToZeroAndScaleSimple(latencies, scaleFactor);
		double[] Y = StatUtil.scaleSimple(ISIs, scaleFactor);		
		
		LeastSquareSolverUtil l = new LeastSquareSolverUtil(X, Y);
		
		solverStats[0] = l.solveFor1Parm(1);			
		solverStats[1] = l.solveFor2Parms(0, 1);	
		solverStats[2] = l.solveFor3Parms(solverStats[1].getM1(), 1, 1);
		solverStats[3] = l.solveFor4Parms(solverStats[2].getM1(), 1, 0, 1);
				
		
	}
	
	
	private int classifyFastTransientStat(int firstNIsIs){	
		int idx = 0;
		for(int i=firstNIsIs; i>=2; i--){
			double[][] latenciesAndISIs = somaSpikePattern.getFirstNISIsAndTheirLatenciesToSecondSpike(i);
			double[] latencies = latenciesAndISIs[0];
			double[] ISIs = latenciesAndISIs[1];				
			double scaleFactor = GeneralUtils.findMin(ISIs);
			double[] X = StatUtil.shiftLeftToZeroAndScaleSimple(latencies, scaleFactor);
			double[] Y = StatUtil.scaleSimple(ISIs, scaleFactor);
			
			LeastSquareSolverUtil l = new LeastSquareSolverUtil(X, Y);	
			SolverResultsStat solverStatLocal = l.solveFor2Parms(0, 1);
			
			if(solverStatLocal.getM1()>SLOPE_THRESHOLD_FT){
				patternClass.removeLastAddedComponent();				
				patternClass.addComponent(SpikePatternComponent.FASP);
				patternClass.addClassificationParameter(ClassificationParameterID.M_FASP, solverStatLocal.getM1());
				patternClass.addClassificationParameter(ClassificationParameterID.B_FASP, solverStatLocal.getC1());
				patternClass.addClassificationParameter(ClassificationParameterID.N_ISI_cut_FASP, (double)i);
				
				
				
				idx = i;
				break;
			}
		}
		return idx;
	}
	
	
	private boolean hasPSTUT(int startIdx){
		double[] isis = somaSpikePattern.getISIsStartingFromIdx(startIdx);
		
		int maxISIidx = GeneralUtils.findMaxIdx(isis);
		if(maxISIidx == isis.length-1) {
			//last ISI is max ISI
			return false;
		}
		double factor = 0;
		double maxISI = isis[maxISIidx];		
		if(maxISIidx>0){
			double pre_maxISI = isis[maxISIidx-1];
			factor += maxISI / pre_maxISI;			
		}		
		double post_maxISI = isis[maxISIidx+1];		
		//System.out.print("\t"+startIdx+","+maxISI+","+post_maxISI+","+factor);
		factor += maxISI/post_maxISI;
		if(factor>= PSTUT_FACTOR)
			return true;
		else 
			return false;
	}
	
	private boolean hasSTUT(int startIdx){
		double[] ISIs = somaSpikePattern.getISIsStartingFromIdx(startIdx);
		BurstPattern bp= new BurstPattern(ISIs, PSTUT_PRE_FACTOR, PSTUT_POST_FACTOR);
		
		if(bp.isBurst())
			return true;
		else
			return false;
	}
	
	
	public SolverResultsStat[] getSolverResultsStats(){
		return solverStats;
	}
	public SpikePatternClass getSpikePatternClass(){
		return this.patternClass;
	}
	
	
	
	
	public static void main(String[] args) {
		//String fileName = args[0];
		try {
			BufferedReader br = new BufferedReader(new FileReader("spike.csv"));
			String str = br.readLine();
			if(str==null){
				System.out.println("Empty input(s)! - line 1");
				System.exit(-1);
			}
			StringTokenizer st = new StringTokenizer(str, ",");
			if(st.countTokens()!=2){
				System.out.println("line 1 requires 2 tokens!");
				System.exit(-1);
			}
			double current = Double.parseDouble(st.nextToken());
			double duration = Double.parseDouble(st.nextToken());
			
			str = br.readLine();
			if(str==null){
				System.out.println("Empty input(s)! - line 2");
				System.exit(-1);
			}
			st = new StringTokenizer(str, ",");
			List<Double> sptimes = new ArrayList<>();			
			while(st.hasMoreTokens()){
				sptimes.add(Double.parseDouble(st.nextToken()));
			}
			
			str=br.readLine();
			if(str==null){
				System.out.println("Empty input! - line 3");
				System.exit(-1);
			}
			double swa = Double.parseDouble(str);			
			br.close();
			double[] spikeTimes = new double[sptimes.size()];
			for(int i=0;i<spikeTimes.length;i++){
				spikeTimes[i] = sptimes.get(i);
			}
		
			SpikePatternAdapting sp = new SpikePatternAdapting(spikeTimes, current, 0, duration);
			SpikePatternClassifier classifier = new SpikePatternClassifier(sp);
			StatAnalyzer.display_stats = true;
			classifier.classifySpikePattern_EXP(swa, false);
			classifier.getSpikePatternClass().display();
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	

}
