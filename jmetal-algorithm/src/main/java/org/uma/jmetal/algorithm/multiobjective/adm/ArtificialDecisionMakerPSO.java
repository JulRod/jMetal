package org.uma.jmetal.algorithm.multiobjective.adm;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.uma.jmetal.algorithm.InteractiveAlgorithm;
import org.uma.jmetal.algorithm.multiobjective.mombi.util.ASFWASFGA;
import org.uma.jmetal.algorithm.singleobjective.particleswarmoptimization.StandardPSO;
import org.uma.jmetal.problem.DoubleProblem;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.impl.AbstractDoubleProblem;
import org.uma.jmetal.problem.impl.AbstractIntegerDoubleProblem;
import org.uma.jmetal.problem.impl.AbstractIntegerProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.singleobjective.ReferencePointProblem;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.comparator.ObjectiveComparator;
import org.uma.jmetal.util.distance.impl.EuclideanDistanceBetweenSolutionsInObjectiveSpace;
import org.uma.jmetal.util.distance.impl.EuclideanDistanceBetweenSolutionsInSolutionSpace;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.point.Point;
import org.uma.jmetal.util.point.impl.ArrayPoint;
import org.uma.jmetal.util.point.util.distance.EuclideanDistance;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.referencePoint.ReferencePoint;
import org.uma.jmetal.util.referencePoint.impl.IdealPoint;
import org.uma.jmetal.util.referencePoint.impl.NadirPoint;

import java.util.*;


public class ArtificialDecisionMakerPSO<S extends Solution<?>> extends
    ArtificialDecisionMaker<S,List<S>> {
  protected IdealPoint idealOjectiveVector = null;
  protected NadirPoint nadirObjectiveVector = null;
  protected List<Double> rankingCoeficient = null;
  protected ReferencePoint asp = null;
  protected List<Double> aspList = null;
  protected double tolerance;
  protected int numberReferencePoints;
  protected JMetalRandom random = null;
  protected double considerationProbability;
  protected  int numberOfObjectives;
  protected double varyingProbability;
  protected int evaluations;
  protected int maxEvaluations;
  protected List<ReferencePoint> allReferencePoints;
  protected ReferencePoint currentReferencePoint;
  protected List<Double> distances;
  protected List<Double> distancesRP;
  private S solutionRun=null;
  private StandardPSO pso;
  private ReferencePointProblem rfProblem;
  private SolutionListEvaluator<DoubleSolution> evaluator;
  private DoubleProblem auxProblem;
  private int iterationIntern;
  private List<Double> min = null ;
  private List<Double> max = null;
 // private List<DoubleSolution> prueba;
  public ArtificialDecisionMakerPSO(Problem<S> problem,
      InteractiveAlgorithm<S, List<S>> algorithm,double considerationProbability,double tolerance,int maxEvaluations
      ,List<Double> rankingCoeficient,int numberReferencePoints,List<Double> asp,SolutionListEvaluator<DoubleSolution> evaluator,String aspFile, int aspOrden,int iterationIntern) {
    super(problem, algorithm);
    //crear pso monoobjectivo
    //el pso se crea cuando se le pasa el frente
    auxProblem = new DTLZ1(problem.getNumberOfObjectives(),problem.getNumberOfObjectives());

    this.considerationProbability = considerationProbability;
    this.varyingProbability = considerationProbability;
    this.tolerance = tolerance;
    this.numberOfObjectives=problem.getNumberOfObjectives();
    this.evaluator = evaluator;
    this.random = JMetalRandom.getInstance();
    this.maxEvaluations = maxEvaluations;
    this.rankingCoeficient = rankingCoeficient;
    if(rankingCoeficient==null || rankingCoeficient.isEmpty()){
      initialiceRankingCoeficient();
    }
    this.numberReferencePoints = numberReferencePoints;
    this.allReferencePoints = new ArrayList<>();
    this.distances = new ArrayList<>();
    this.distancesRP = new ArrayList<>();
    this.iterationIntern = iterationIntern;
    List<List<Double>> auxAsp=this.getAspirationLevel(aspFile);
    min = auxAsp.get(0);
     max = auxAsp.get(2);
    if(aspOrden==3) {

      asp = new ArrayList<>();
      for (int i = 0; i < min.size() ; i++) {
        double value=JMetalRandom.getInstance().nextDouble(min.get(i),max.get(i));
        asp.add(value);
      }
    }
    else{
      if (aspFile != null) {

        asp = this.getAspirationLevel(aspFile).get(aspOrden);
     /* List<List<Double>> auxAsp=this.getAspirationLevel(aspFile);
      List<Double> min = auxAsp.get(0);
      List<Double> max = auxAsp.get(2);
      asp = new ArrayList<>();
      for (int i = 0; i < min.size() ; i++) {
        double value=JMetalRandom.getInstance().nextDouble(min.get(i),max.get(i));
        asp.add(value);
      }*/
      }
    }
    this.aspList = asp;
    if(asp!=null){
      this.asp= new IdealPoint(numberOfObjectives);
      int i=0;
      for (Double obj:asp) {
        this.asp.setObjective(i,obj);
        i++;
      }
    }
    this.rfProblem = new ReferencePointProblem(asp,auxProblem);
    //prueba = new ArrayList<>();
  }
  private  void  initialiceRankingCoeficient(){
    rankingCoeficient = new ArrayList<>();
    for (int i = 0; i < problem.getNumberOfObjectives() ; i++) {
      rankingCoeficient.add(1.0/problem.getNumberOfObjectives());
    }
  }

  private void updateObjectiveVector(List<S> solutionList){
    for (int j = 0; j < numberOfObjectives; j++) {
      Collections.sort(solutionList, new ObjectiveComparator<>(j));
      double objetiveMinn = solutionList.get(0).getObjective(j);
      double objetiveMaxn = solutionList.get(solutionList.size() - 1).getObjective(j);
      idealOjectiveVector.setObjective(j,objetiveMinn);
      nadirObjectiveVector.setObjective(j,objetiveMaxn);
    }
    if(problem instanceof AbstractDoubleProblem){
      AbstractDoubleProblem aux =(AbstractDoubleProblem)problem;
      for (int i = 0; i < numberOfObjectives ; i++) {
        idealOjectiveVector.setObjective(i,aux.getLowerBound(i));
        nadirObjectiveVector.setObjective(i,aux.getUpperBound(i));
      }
    }else if(problem instanceof AbstractIntegerProblem){
      AbstractIntegerProblem aux =(AbstractIntegerProblem)problem;
      for (int i = 0; i < numberOfObjectives ; i++) {
        idealOjectiveVector.setObjective(i,aux.getLowerBound(i));
        nadirObjectiveVector.setObjective(i,aux.getUpperBound(i));
      }
    }else if(problem instanceof AbstractIntegerDoubleProblem){
      AbstractIntegerDoubleProblem aux =(AbstractIntegerDoubleProblem)problem;
      for (int i = 0; i < numberOfObjectives ; i++) {
        idealOjectiveVector.setObjective(i,aux.getLowerBound(i).doubleValue());
        nadirObjectiveVector.setObjective(i,aux.getUpperBound(i).doubleValue());
      }
    }
    if(asp==null)
      asp = idealOjectiveVector;
  }

  private IdealPoint createReferencePointIdeal(List<Double> list){
    IdealPoint solution =  new IdealPoint(numberOfObjectives);
    for (int i = 0; i < list.size() ; i++) {
      solution.setObjective(i,list.get(i));
      solution.setVariableValue(i,list.get(i));
    }
    return solution;
  }
  private NadirPoint createReferencePointNadir(List<Double> list){
    NadirPoint solution =  new NadirPoint(numberOfObjectives);
    for (int i = 0; i < list.size() ; i++) {
      solution.setObjective(i,list.get(i));
      solution.setVariableValue(i,list.get(i));
    }
    return solution;
  }
  @Override
  protected List<ReferencePoint> generatePreferenceInformation() {

    idealOjectiveVector = createReferencePointIdeal(min);//new IdealPoint(numberOfObjectives);
    nadirObjectiveVector = createReferencePointNadir(max);

    List<S> solutions = new ArrayList<>();
    S sol = problem.createSolution();
    problem.evaluate(sol);
    solutions.add(sol);
   // updateObjectiveVector(solutions);
    // solutionRun = solutions.get(0);
    List<ReferencePoint> referencePoints  = new ArrayList<>();
    for (int i=0;i < numberReferencePoints;i++){
      ReferencePoint referencePoint = new IdealPoint(numberOfObjectives);
      for (int j = 0; j < numberOfObjectives; j++) {
        double rand = random.nextDouble(0.0, 1.0);
      //  double mult=0.25;
       // if(j==1){
        //  mult=10;
       // }
        if (rand < considerationProbability * rankingCoeficient.get(i)) {
          referencePoint.setObjective(j, asp.getObjective(j));
        //  referencePoint.setObjective(j, nadirObjectiveVector.getObjective(j)*mult);
        } else {
          //referencePoint.setObjective(j, nadirObjectiveVector.getObjective(j));
          referencePoint.setObjective(j, nadirObjectiveVector.getObjective(j));
        }
      }
      referencePoints.add(referencePoint);
    }
    currentReferencePoint = referencePoints.get(0);
    allReferencePoints.addAll(referencePoints);
    return referencePoints;
  }

  @Override
  protected boolean isStoppingConditionReached() {
    boolean stop = evaluations > maxEvaluations; //|| stopConditionDistance(distances,tolerance);
    // if(distancesRP!=null){
    //  stop = stop || distancesRP.contains(0.0);
    // }
    //if(indexOfRelevantObjectiveFunctions!=null   ){
    //  stop = stop || indexOfRelevantObjectiveFunctions.size()==numberOfObjectives;
    //}
    return stop;
  }

  private boolean stopConditionDistance(List<Double> list, double value){
    boolean result = false;
    if(list!=null){
      int i=0;
      while (!result && i<list.size() ){
        result = list.get(i)<value;
        i++;
      }
    }
    return result;
  }
  @Override
  protected void initProgress() {
    this.distances = new ArrayList<>();
    evaluations =0;
    varyingProbability = considerationProbability;
  }

  @Override
  protected void updateProgress() {
    evaluations++;
  }

  @Override
  protected List<Integer> relevantObjectiveFunctions(List<S> front) {
    List<Integer> order = new ArrayList<>();
    List<Integer> indexRelevantObjectivesFunctions = new ArrayList<>();
    /*Solution s = front.get(0);
    for (int i = 0; i < s.getNumberOfObjectives() ; i++) {
      indexRelevantObjectivesFunctions.add(i);
    }*/
    //updateObjectiveVector(front);
   SortedMap<Double, List<Integer>> map = new TreeMap<>(Collections.reverseOrder());
    for (int i = 0; i < rankingCoeficient.size(); i++) {
      List<Integer> aux = map.getOrDefault(rankingCoeficient.get(i), new ArrayList<>());
      aux.add(i);
      map.putIfAbsent(rankingCoeficient.get(i),aux);
    }
    Set<Double> keys =map.keySet();
    for (Double key:keys) {
      order.addAll(map.get(key));
    }
    S solution = getSolution(front,currentReferencePoint);
    for (Integer i : order) {
      double rand = random.nextDouble(0.0, 1.0);
      if ((asp.getObjective(i) - solution.getObjective(i)) < tolerance
          && rand < considerationProbability) {
        indexRelevantObjectivesFunctions.add(i);
      } else if (rand < varyingProbability) {
        indexRelevantObjectivesFunctions.add(i);
      }
      varyingProbability -= (varyingProbability / i) * indexRelevantObjectivesFunctions.size();
    }
    return indexRelevantObjectivesFunctions;
  }

  private List<DoubleSolution> createSwarm(List<S> front){
    List<DoubleSolution> swarm = new ArrayList<>();
    for (S solution:front) {
      swarm.add(createParticle(solution));
    }
    return swarm;
  }

  private DoubleSolution createParticle(S solution){
    DoubleSolution result = rfProblem.createSolution();
    for (int i = 0; i < solution.getNumberOfObjectives() ; i++) {
      result.setVariableValue(i,solution.getObjective(i));
      //result.setObjective(i,solution.getObjective(i));
    }
    return result;
  }
  @Override
  protected List<ReferencePoint> calculateReferencePoints(
      List<Integer> indexOfRelevantObjectiveFunctions, List<S> front,List<S> paretoOptimalSolutions) {
    List<ReferencePoint> result = new ArrayList<>();
    List<S> temporal = new ArrayList<>(front);
    List<DoubleSolution> swarm = createSwarm(front);
    //10 + (int) (2 * Math.sqrt(rfProblem.getNumberOfVariables()))
    int numPart =3;
    if(swarm.size()<=numPart){
      numPart =swarm.size()-1;
    }
  //  System.out.println("Iteracion " + evaluations);
   // System.out.println("");
    pso = new StandardPSO(rfProblem,
        swarm.size(),
        iterationIntern, numPart, evaluator,swarm,aspList);

     pso.run();
         DoubleSolution psoSolution = pso.getResult();

  //  distances.add(psoSolution.getObjective(0));

    /*DifferentialEvolutionSelection selection;
    DifferentialEvolutionCrossover crossover;
    crossover = new DifferentialEvolutionCrossover(0.5, 0.5, "rand/1/bin") ;
    selection = new DifferentialEvolutionSelection();

    if(swarm.size()<=4){
      while (swarm.size()<100 ) {
        swarm.addAll(createSwarm(front));
      }
    }
    DifferentialEvolution de = new DifferentialEvolutionBuilder(rfProblem)
        .setCrossover(crossover)
        .setSelection(selection)
        .setSolutionListEvaluator(evaluator)
        .setMaxEvaluations(250000)
        .setPopulationSize(swarm.size()).setInitialPopulation(swarm)
        .build() ;
    de.run();
     DoubleSolution psoSolution = de.getResult();*/
    S solution = null;
    // if(solutionRun!=null) {
   // if(distances.isEmpty()){
   //   solution=getMaxSolution(temporal, currentReferencePoint);
   // }else {
      solution = getSolution(temporal, currentReferencePoint);
  //  }
    solutionRun = solution;
    for(int numRefPoint=0;numRefPoint<numberReferencePoints;numRefPoint++){
     // S sol = problem.createSolution();
      if(solutionRun!=null) {//currentReferencePoint
        // calculateDistanceRP(currentReferencePoint, asp);
        calculateDistance(solutionRun, asp);
        // calculateDistanceRP(solutionRun, currentReferencePoint);
      }

      //}else{
       // solution = getMaxSolution(temporal, currentReferencePoint);//front.get(JMetalRandom.getInstance().nextInt(0,front.size()-1));
      //}

      //temporal.remove(solution);
      // if (indexOfRelevantObjectiveFunctions.size() == numberOfObjectives) {
      // result.add(getReferencePointFromSolution(solution));
      //} else {
      ReferencePoint referencePoint = new NadirPoint(numberOfObjectives);
      for (int i = 0; i < referencePoint.getNumberOfObjectives(); i++) {
        if (indexOfRelevantObjectiveFunctions.contains(i)) {
          //referencePoint.setObjective(i,asp.getObjective(i) - (asp.getObjective(i) - solution.getObjective(i)) / 2);

         referencePoint.setObjective(i,
              psoSolution.getVariableValue(i));
        } else {
          //predict the i position of reference point
          //coger el mas cercano entre
          //double pred =prediction(i,paretoOptimalSolutions,solution);
          //double cal= asp.getObjective(i) - (asp.getObjective(i) - solution.getObjective(i)) / 2;
          //referencePoint.setObjective(i, Math.abs(pred-asp.getObjective(i)) <= Math.abs(cal-asp.getObjective(i)) ? pred : cal);
           referencePoint.setObjective(i, prediction(i,front,solutionRun));//paretoOptimalSolutions
          //referencePoint.setObjective(i,
           //   psoSolution.getVariableValue(i));
          //referencePoint.setObjective(i, predictionDouble(i,prueba,psoSolution));
        //  referencePoint.setObjective(i,asp.getObjective(i) - (asp.getObjective(i) - solution.getObjective(i)) / 2);
          //referencePoint.setObjective(i,asp.getObjective(i) - (asp.getObjective(i) - psoSolution.getVariableValue(i)) / 2);
          //referencePoint.setObjective(i,
           //   psoSolution.getVariableValue(i));
        }
      }
      result.add(referencePoint);
      //}
    }

    currentReferencePoint = result.get(0);
    allReferencePoints.addAll(result);

    return result;
  }

  private void calculateDistance(S solution, ReferencePoint referencePoint){
    EuclideanDistanceBetweenSolutionsInObjectiveSpace euclidean = new EuclideanDistanceBetweenSolutionsInObjectiveSpace();
    //EuclideanDistance euclideanDistance = new EuclideanDistance();

    //double distance = euclideanDistance.compute(getPointFromSolution(solution),
    //    getPointFromReferencePoint(referencePoint));
    double distance = euclidean.getDistance((DoubleSolution)solution,getSolutionFromRP(referencePoint));
    distances.add(distance);
   System.out.println(distance);
   // distances.add(distance);
  }

  private void calculateDistanceDS(DoubleSolution solution, ReferencePoint referencePoint){
    EuclideanDistanceBetweenSolutionsInSolutionSpace euclidean = new EuclideanDistanceBetweenSolutionsInSolutionSpace();
    //EuclideanDistance euclideanDistance = new EuclideanDistance();

    //double distance = euclideanDistance.compute(getPointFromSolution(solution),
    //    getPointFromReferencePoint(referencePoint));
    double distance = euclidean.getDistance(solution,getSolutionFromRP(referencePoint));
    //System.out.println("Distancia "+distance);

  }

  private void calculateDistanceRP(S solution, ReferencePoint referencePoint){
    EuclideanDistance euclideanDistance = new EuclideanDistance();

    double distance = euclideanDistance.compute(getPointFromSolution(solution),
        getPointFromReferencePoint(referencePoint));
    distancesRP.add(distance);
  }
  private ReferencePoint getReferencePointFromSolution(S solution) {
    ReferencePoint result = new IdealPoint(solution.getNumberOfObjectives());
    result.update(solution);
    return result;
  }
  private DoubleSolution getSolutionFromRP(ReferencePoint referencePoint){
    DoubleSolution result = auxProblem.createSolution();
    for (int i = 0; i < result.getNumberOfVariables(); i++) {
      result.setObjective(i,referencePoint.getObjective(i));
      result.setVariableValue(i,referencePoint.getObjective(i));

    }
    return result;
  }
  /* @Override
   protected double calculateComponentReferencePoint(int index,List<S> front) {
     S solution = getSolution(front,currentReferencePoint);
     double result = asp.getObjective(index) - (asp.getObjective(index) -
           solution.getObjective(index)) / 2;
     return result;
   }

   @Override
   protected double prediction(int index,List<List<S>> paretoOptimalSolutions) {
     //FALTA PREDICTION
     return currentReferencePoint.getObjective(index);
   }*/
  private double prediction(int index,List<S> paretoOptimalSolutions,S solution) {
    //FALTA PREDICTION
    DecisionTreeEstimator<S> dte = new DecisionTreeEstimator<S>(paretoOptimalSolutions);

    double data=dte.doPrediction(index,solution);
    return data;//currentReferencePoint.getObjective(index);
  }
  /*private double predictionDouble(int index,List<DoubleSolution> paretoOptimalSolutions,
      DoubleSolution solution) {
    //FALTA PREDICTION
    DecisionTreeEstimator<DoubleSolution> dte = new DecisionTreeEstimator<DoubleSolution>(prueba);

    double data=dte.doPredictionVariable(index,solution);
    return data;//currentReferencePoint.getObjective(index);
  }*/
  @Override
  protected void updateParetoOptimal(List<S> front,List<S> paretoOptimalSolutions) {
    //paretoOptimalSolutions.addAll(front);
    paretoOptimalSolutions = new ArrayList<>(front);
  }

  @Override
  public List<ReferencePoint> getReferencePoints() {
    allReferencePoints.remove(allReferencePoints.size()-1);
    return allReferencePoints;
  }

  @Override
  public List<Double> getDistances() {
    //for (ReferencePoint referencePoint:allReferencePoints) {
    //   calculateDistance(getSolution(paretoOptimalSolutions,referencePoint),referencePoint);
    //}
    return distances;
  }

  private S getSolution(List<S> front, ReferencePoint referencePoint) {
    S result = front.get(0);
    EuclideanDistanceBetweenSolutionsInObjectiveSpace euclidean = new EuclideanDistanceBetweenSolutionsInObjectiveSpace();
   // EuclideanDistance euclideanDistance = new EuclideanDistance();
    SortedMap<Double, S> map = new TreeMap<>();
    DoubleSolution aux = getSolutionFromRP(referencePoint);
    double suma=0;
    for (S solution : front) {
     // double distance = euclideanDistance.compute(getPointFromSolution(solution),
     //     getPointFromReferencePoint(referencePoint));
     // map.put(distance, solution);
      double distance = euclidean.getDistance(solution,aux);
      suma+=distance;
      map.put(distance, solution);
    }
  //  System.out.println("Sumaaaaaaaaaaaaaa "+suma);
    result = map.get(map.firstKey());
    return result;
  }
  private S getMaxSolution(List<S> front, ReferencePoint referencePoint) {
    S result = front.get(0);
    EuclideanDistanceBetweenSolutionsInObjectiveSpace euclidean = new EuclideanDistanceBetweenSolutionsInObjectiveSpace();
    // EuclideanDistance euclideanDistance = new EuclideanDistance();
    SortedMap<Double, S> map = new TreeMap<>();
    DoubleSolution aux = getSolutionFromRP(referencePoint);
    for (S solution : front) {
      // double distance = euclideanDistance.compute(getPointFromSolution(solution),
      //     getPointFromReferencePoint(referencePoint));
      // map.put(distance, solution);
      double distance = euclidean.getDistance(solution,aux);
      map.put(distance, solution);
    }
    result = map.get(map.lastKey());
    return result;
  }

  private S getSolutionPSO(List<S> front, DoubleSolution ds) {
    S result = front.get(0);
    EuclideanDistanceBetweenSolutionsInObjectiveSpace euclidean = new EuclideanDistanceBetweenSolutionsInObjectiveSpace();
    // EuclideanDistance euclideanDistance = new EuclideanDistance();
    SortedMap<Double, S> map = new TreeMap<>();
   // DoubleSolution aux = getSolutionFromRP(referencePoint);
    for (S solution : front) {
      // double distance = euclideanDistance.compute(getPointFromSolution(solution),
      //     getPointFromReferencePoint(referencePoint));
      // map.put(distance, solution);
      double distance = euclidean.getDistance(solution,ds);
      map.put(distance, solution);
    }
    result = map.get(map.firstKey());
    return result;
  }
  private Point getPointFromSolution(S solution) {
    Point result = new ArrayPoint(solution.getNumberOfObjectives());
    for (int i = 0; i < solution.getNumberOfObjectives(); i++) {
      result.setDimensionValue(i, solution.getObjective(i));
    }
    return result;
  }

  private Point getPointFromReferencePoint(ReferencePoint referencePoint) {
    Point result = new ArrayPoint(referencePoint.getNumberOfObjectives());
    for (int i = 0; i < referencePoint.getNumberOfObjectives(); i++) {
      result.setDimensionValue(i, referencePoint.getObjective(i));
    }
    return result;
  }
  private S referencePointToSolution(ReferencePoint referencePoint){
    S result = problem.createSolution();
    for (int i = 0; i < referencePoint.getNumberOfObjectives(); i++) {
      result.setObjective(i,referencePoint.getObjective(i));
    }
    return result;
  }

  public S getProjection(ReferencePoint referencePoint) {
    S solution = problem.createSolution();
    double [] [] weights = new double[1][rankingCoeficient.size()];
    int i=0;
    for (Double ranking:rankingCoeficient) {
      weights[0][i]= ranking;
      i++;
    }
    List<Double> interestPoint = new ArrayList<>();

    for (int j = 0; j < referencePoint.getNumberOfObjectives(); j++) {
      interestPoint.add(referencePoint.getObjective(j));
    }
    List<Double> nadir = new ArrayList<>();
    for (int j = 0; j < nadirObjectiveVector.getNumberOfObjectives(); j++) {
      nadir.add(nadirObjectiveVector.getObjective(j));
    }
    List<Double> utopia = new ArrayList<>();
    for (int j = 0; j <idealOjectiveVector.getNumberOfObjectives() ; j++) {
      utopia.add(idealOjectiveVector.getObjective(j));
    }
    ASFWASFGA<S> asf = new ASFWASFGA<S>(weights, interestPoint);
    asf.setNadir(nadir);
    asf.setUtopia(utopia);
    solution.setObjective(0,asf.evaluate(referencePointToSolution(asp),0));
    return solution;
  }
  private  List<List<Double>> getAspirationLevel(String name){
    List<List<Double>> result = null;
    if(name!=null){
      try{
        InputStream in = getClass().getResourceAsStream("/" + name);
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(isr);
        result = new ArrayList<>();
        String thisLine;
        while ((thisLine = br.readLine()) != null) { // while loop begins here
          String [] aux =thisLine.split(" ");
          List<Double> aspList = new ArrayList<>();
          for (int i = 0; i <aux.length ; i++) {
            aspList.add(Double.parseDouble(aux[i]));
          }
          result.add(aspList);
        } // end while
        br.close();
      }catch (Exception ex){
      }
    }
    return result;
  }
}
