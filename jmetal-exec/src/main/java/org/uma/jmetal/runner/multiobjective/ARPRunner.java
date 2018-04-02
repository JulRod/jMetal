//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.uma.jmetal.runner.multiobjective;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.InteractiveAlgorithm;
import org.uma.jmetal.algorithm.multiobjective.adm.ARP;
import org.uma.jmetal.algorithm.multiobjective.adm.ARPBuilder;
import org.uma.jmetal.algorithm.multiobjective.rnsgaii.RNSGAIIBuilder;
import org.uma.jmetal.algorithm.multiobjective.wasfga.WASFGA;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.crossover.SBXCrossover;
import org.uma.jmetal.operator.impl.mutation.PolynomialMutation;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ4;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ5;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ6;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7;
import org.uma.jmetal.runner.AbstractAlgorithmRunner;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.util.AlgorithmRunner;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import org.uma.jmetal.util.referencePoint.ReferencePoint;
import org.uma.jmetal.util.referencePoint.impl.IdealPoint;
import org.uma.jmetal.util.referencePoint.impl.NadirPoint;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to configure and run the R-NSGA-II algorithm
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 * @author Cristobal Barba <cbarba@lcc.uma.es>
 */
public class ARPRunner extends AbstractAlgorithmRunner {
  /**
   * @param args Command line arguments.
   * @throws JMetalException
   * @throws FileNotFoundException
   * Invoking command:
    java org.uma.jmetal.runner.multiobjective.RNSGAIIRunner_Ant problemName [referenceFront]
   */
  public static void main(String[] args) throws JMetalException, FileNotFoundException {
    Problem<DoubleSolution> problem;
    Algorithm<List<DoubleSolution>> algorithm;
    InteractiveAlgorithm<DoubleSolution,List<DoubleSolution>> algorithmRun;
    CrossoverOperator<DoubleSolution> crossover;
    MutationOperator<DoubleSolution> mutation;
    SelectionOperator<List<DoubleSolution>, DoubleSolution> selection;
    String referenceParetoFront = "" ;

    int numberIterations =1;
   // String problemName = "DTLZ1" ;
    //int numberObjectives = 3;
    //int numberVariables = 7;
    String problemName = "DTLZ4" ;
    int numberObjectives = 3;
    int numberVariables = 12;
    String algorithmName ="RNSGAII";
    String weightsName = "MOEAD_Weights/W3_100.dat";
    int populationSize=100;
    String aspName = "ppsn_asp/ASP_DTLZ6_5bj.dat";
    int aspOrden =1;
    /*if (args.length == 1) {
      problemName = args[0];
    } else if (args.length == 2) {
      problemName = args[0] ;
      referenceParetoFront = args[1] ;
    } else {
      problemName = "org.uma.jmetal.problem.multiobjective.zdt.ZDT1";
      referenceParetoFront = "jmetal-problem/src/test/resources/pareto_fronts/ZDT1.pf" ;
    }*/
    if(args!=null){
      if(args.length>2) {
        problemName = args[0];
        numberIterations = Integer.parseInt(args[1]);
        numberObjectives = Integer.parseInt(args[2]);
        algorithmName = args[3];
        aspOrden = Integer.parseInt(args[4]);
      }
    }

    switch (problemName){
      case "DTLZ1":
        numberVariables = 7;
        problem =new DTLZ1(numberVariables,numberObjectives);

        break;
      case "DTLZ2":
        numberVariables = 12;
        problem =new DTLZ2(numberVariables,numberObjectives);

        break;
      case "DTLZ3":
        numberVariables = 12;
        problem =new DTLZ3(numberVariables,numberObjectives);

        break;
      case "DTLZ4":
        numberVariables = 12;
        problem =new DTLZ4(numberVariables,numberObjectives);

        break;
      case "DTLZ5":
        numberVariables = 12;
        problem =new DTLZ5(numberVariables,numberObjectives);

        break;
      case "DTLZ6":
        numberVariables = 12;
        problem =new DTLZ6(numberVariables,numberObjectives);

        break;
      case "DTLZ7":
        numberVariables = 22;
        problem =new DTLZ7(numberVariables,numberObjectives);

        break;
      default:
        problem =new DTLZ1(numberVariables,numberObjectives);//  ProblemUtils.<DoubleSolution> loadProblem(problemName);//Tanaka();//
    }
    switch (numberObjectives){
      case 3:weightsName = "MOEAD_Weights/W3D_100.dat";
        populationSize=100;

        switch (problemName){
          case "DTLZ1":aspName = "ppsn_asp/ASP_DTLZ1_3Obj.dat";
            break;
          case "DTLZ2":aspName = "ppsn_asp/ASP_DTLZ2_3Obj.dat";
            break;
          case "DTLZ3":aspName = "ppsn_asp/ASP_DTLZ3_3Obj.dat";
            break;
          case "DTLZ4":aspName = "ppsn_asp/ASP_DTLZ4_3Obj.dat";
            break;
          case "DTLZ5":aspName = "ppsn_asp/ASP_DTLZ5_3Obj.dat";
            break;
          case "DTLZ6":aspName = "ppsn_asp/ASP_DTLZ6_3Obj.dat";
            break;
          case "DTLZ7":aspName = "ppsn_asp/ASP_DTLZ7_3Obj.dat";
            break;
        }

        break;
      case 5:
        weightsName = "MOEAD_Weights/W5D_100.dat";
        populationSize=100;
        aspName= "ppsn_asp/ASP_DTLZ1_5Obj.dat";
        switch (problemName){
          case "DTLZ1":aspName = "ppsn_asp/ASP_DTLZ1_5Obj.dat";
            break;
          case "DTLZ2":aspName = "ppsn_asp/ASP_DTLZ2_5Obj.dat";
            break;
          case "DTLZ3":aspName = "ppsn_asp/ASP_DTLZ3_5Obj.dat";
            break;
          case "DTLZ4":aspName = "ppsn_asp/ASP_DTLZ4_5Obj.dat";
            break;
          case "DTLZ5":aspName = "ppsn_asp/ASP_DTLZ5_5Obj.dat";
            break;
          case "DTLZ6":aspName = "ppsn_asp/ASP_DTLZ6_5Obj.dat";
            break;
          case "DTLZ7":aspName = "ppsn_asp/ASP_DTLZ7_5Obj.dat";
            break;
        }
        break;
      case 7:
        weightsName = "MOEAD_Weights/W7D_100.dat";
        populationSize=100;
        aspName= "ppsn_asp/ASP_DTLZ1_7Obj.dat";
        switch (problemName){
          case "DTLZ1":aspName= "ppsn_asp/ASP_DTLZ1_7Obj.dat";
            break;
          case "DTLZ2":aspName= "ppsn_asp/ASP_DTLZ2_7Obj.dat";
            break;
          case "DTLZ3":aspName= "ppsn_asp/ASP_DTLZ3_7Obj.dat";
            break;
          case "DTLZ4":aspName= "ppsn_asp/ASP_DTLZ4_7Obj.dat";
            break;
          case "DTLZ5":aspName= "ppsn_asp/ASP_DTLZ5_7Obj.dat";
            break;
          case "DTLZ6":aspName= "ppsn_asp/ASP_DTLZ6_7Obj.dat";
            break;
          case "DTLZ7":aspName= "ppsn_asp/ASP_DTLZ7_7Obj.dat";
            break;
        }
        break;
    }

    //problem =new DTLZ4(7,2);//  ProblemUtils.<DoubleSolution> loadProblem(problemName);//Tanaka();//
//problem = new ZDT1();
    double crossoverProbability = 0.9 ;
    double crossoverDistributionIndex = 20.0 ;
    crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex) ;

    double mutationProbability = 1.0 / problem.getNumberOfVariables() ;
    double mutationDistributionIndex = 20.0 ;
    mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex) ;

    selection = new BinaryTournamentSelection<DoubleSolution>(
        new RankingAndCrowdingDistanceComparator<DoubleSolution>());

    IdealPoint idealPoint = new IdealPoint(problem.getNumberOfObjectives());
    idealPoint.update(problem.createSolution());
    NadirPoint nadirPoint = new NadirPoint(problem.getNumberOfObjectives());
    nadirPoint.update(problem.createSolution());
    double considerationProbability = 0.9;
    List<Double> rankingCoeficient = new ArrayList<>();
    for (int i = 0; i < problem.getNumberOfObjectives() ; i++) {
      rankingCoeficient.add(1.0/problem.getNumberOfObjectives());
    }
    double tolerance = 0.5;

    for (int cont = 0; cont < numberIterations ; cont++) {
      List<Double> referencePoint = new ArrayList<>();

    /*referencePoint.add(0.0) ;
    referencePoint.add(1.0) ;
    referencePoint.add(1.0) ;
    referencePoint.add(0.0) ;
    referencePoint.add(0.5) ;
    referencePoint.add(0.5) ;
    referencePoint.add(0.2) ;
    referencePoint.add(0.8) ;
    referencePoint.add(0.8) ;
    referencePoint.add(0.2) ;*/
      //Example fig 2 paper Deb
      // referencePoint.add(0.2) ;
      //referencePoint.add(0.4) ;
      //referencePoint.add(0.8) ;
      //referencePoint.add(0.4) ;
      //referencePoint.add(0.0) ;
      //referencePoint.add(0.0) ;
      //Example fig 3 paper Deb
   /* referencePoint.add(0.1) ;
    referencePoint.add(0.6) ;

    referencePoint.add(0.3) ;
    referencePoint.add(0.6) ;

    referencePoint.add(0.5) ;
    referencePoint.add(0.2) ;

    referencePoint.add(0.7) ;
    referencePoint.add(0.2) ;

    referencePoint.add(0.9) ;
    referencePoint.add(0.0) ;*/
    /*referencePoint.add(0.1) ;
    referencePoint.add(1.0) ;
    referencePoint.add(1.0) ;
    referencePoint.add(0.0) ;

    referencePoint.add(0.5) ;
    referencePoint.add(0.8);
    referencePoint.add(0.8) ;
    referencePoint.add(0.6) ;*/
      //referencePoint.add(0.0) ;
      //referencePoint.add(1.0);

      //referencePoint.add(1.0) ;
      //referencePoint.add(1.0);
      //referencePoint.add(0.4) ;
      //referencePoint.add(0.8);

      double epsilon = 0.0045;
      List<Double> asp = new ArrayList<>();
      for (int i = 0; i < problem.getNumberOfObjectives(); i++) {
        //asp.add(JMetalRandom.getInstance().nextDouble(((AbstractDoubleProblem)problem).getLowerBound(i),((AbstractDoubleProblem)problem).getUpperBound(i)));
        asp.add(0.0);
        referencePoint.add(0.0);//initialization
      }

     // asp.add(0.0);//x 0
      //asp.add(1.0);//y 1
      if(algorithmName.equalsIgnoreCase("RNSGAII")) {
        algorithmRun = new RNSGAIIBuilder<DoubleSolution>(problem, crossover, mutation,
            referencePoint, epsilon)
            .setSelectionOperator(selection)
            .setMaxEvaluations(10000)
            .setPopulationSize(populationSize)
            .build();
      }else {
        algorithmRun = new WASFGA<DoubleSolution>(problem, populationSize, 100, crossover, mutation,
            selection, new SequentialSolutionListEvaluator<DoubleSolution>(), referencePoint,weightsName);
      }
      algorithm = new ARPBuilder<DoubleSolution>(problem, algorithmRun)
          .setConsiderationProbability(0.9)
          .setMaxEvaluations(10)
          .setTolerance(0.9)
          .setAsp(asp)
          .setAspFile(aspName)
          .setAspOrden(aspOrden)
          .build();

      AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm)
          .execute();

      List<DoubleSolution> population = algorithm.getResult();
      long computingTime = algorithmRunner.getComputingTime();

      JMetalLogger.logger.info("Total execution time: " + computingTime + "ms"+ " cont "+cont);
     // String name = "_PPSN_RNSGAII_DTLZ4_0_1_"+cont;//+"_"+cont;
      //String name = "_ORIGINAL_"+algorithmRun.getName()+"_"+problemName+"_"+numberObjectives+"_"+aspOrden+"_"+cont;
      // printFinalSolutionSet(population);
      new SolutionListOutput(population)
          .setSeparator("\t")
          .setVarFileOutputContext(new DefaultFileOutputContext("VAR" + ".tsv"))
          .setFunFileOutputContext(new DefaultFileOutputContext("FUN"  + ".tsv"))
          .print();

      JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());
      JMetalLogger.logger.info("Objectives values have been written to file FUN.tsv");
      JMetalLogger.logger.info("Variables values have been written to file VAR.tsv");
      // if (!referenceParetoFront.equals("")) {
      //   printQualityIndicators(population, referenceParetoFront) ;
      //  }
     // System.out.println("Reference Points-----" + ((ARP) algorithm).getReferencePoints().size());
      writeLargerTextFile("ReferencePoint"  + ".txt", ((ARP) algorithm).getReferencePoints());
      writeLargerDoubleFile("Distances"  + ".txt", ((ARP) algorithm).getDistances());
    }//for borrar
  }
  private static String nameProblem(List<Double> list){
    String result ="";
    for (int i =0; i<list.size();i++){
      result += list.get(i)+"_";
    }
    return result;
  }

  private static List<Double> getReferencePoint(ReferencePoint referencePoint){
    List<Double> result = new ArrayList<>();
    for (int i = 0; i < referencePoint.getNumberOfObjectives(); i++) {
      result.add(referencePoint.getObjective(i));
    }
    return result;
  }
  private static void writeLargerTextFile(String aFileName, List<ReferencePoint> list)  {
    Path path = Paths.get(aFileName);

    try (BufferedWriter writer = Files.newBufferedWriter(path)){
      int i =0;
      while(i<list.size()){
        String line="";
        for (int j = 0; j <list.get(i).getNumberOfObjectives() ; j++) {
          line += list.get(i).getObjective(j) + " ";
        }
        line = line.substring(0,line.lastIndexOf(" "));
        i++;
        writer.write(line);
        writer.newLine();
      }
      writer.close();
    }catch (Exception e){

    }
  }
  private static void writeLargerDoubleFile(String aFileName, List<Double> list)  {
    Path path = Paths.get(aFileName);
    try (BufferedWriter writer = Files.newBufferedWriter(path)){
      int i =0;
      for (Double value:list) {
        String line="";
        line += value + " ";
        writer.write(line);
        writer.newLine();
      }
      writer.close();
    }catch (Exception e){

    }
  }
}
