package disamb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import mosek.Env;
import spotting.Config;
import spotting.EdgePotentialsMatrix;
import spotting.NodePotentials;
import spotting.TrainingData;

/* 
   Copyright: Copyright (c) 1998-2012 MOSEK ApS, Denmark. All rights reserved.

   File:      qo1.java

   Purpose:   Demonstrate how to solve a quadratic
              optimization problem using the MOSEK Java API.
 */

class msgclass extends mosek.Stream {
  public msgclass ()
  {
    super ();
  }
  
  public void stream (String msg)
    {
      System.out.print (msg);
  }
}


public class QuadraticOptimizer
{
  private int NUMCON = 1;   /* Number of constraints.             */
  private int NUMVAR = 3;   /* Number of variables.               */
  private int NUMANZ = 3;   /* Number of numzeros in A.           */
  private int NUMQNZ = 4;   /* Number of nonzeros in Q.           */
  
  private int NUMHRD = 0;   /* Number of extra "hard" constraints */
  
  private double[] obj_c = null;
  private int[] obj_c_sub = null;
  private int[] qsubi = null;
  private int[] qsubj = null;
  private double[] qval = null;
  private HashSet<Integer> hard_constraints = null;
  
  private ArrayList<String> node_names;
  
  public void loadEntityData(TrainingData data){
    NUMVAR = 2 * data.nodes.potentials_set.size();
    NUMCON = data.nodes.potentials_set.size();
    NUMHRD = 0;
    
    hard_constraints = new HashSet<Integer>();
    //load linear term in objective
    
    int index = 0;
    double[][] node_features = new double[data.nodes.potentials_set.size()][3];
    double max_inlink_count = 0.0;
    double max_outlink_count = 0.0;
    double max_context_score = 0.0;
    for (NodePotentials np: data.nodes.potentials_set){
      node_features[index][0] = (double)Math.log(1 + np.inlink_count);
      node_features[index][1] = (double)Math.log(1 + np.outlink_count);
      node_features[index][2] = (double) np.sense_probability + 
                                         np.context_score_synopsis +
                                         np.context_score_frequent +
                                         np.context_score_vbdj +
                                         np.page_title_score;
      
      if (node_features[index][0] > max_inlink_count){
        max_inlink_count = node_features[index][0];
      }
      if (node_features[index][1] > max_outlink_count){
        max_outlink_count = node_features[index][1];
      }
      if (node_features[index][2] > max_context_score){
        max_context_score = node_features[index][2];
      }
                  
      if (np.label == 1){
        hard_constraints.add(index);
        NUMHRD++;
        
      } else if (np.label == -1){
        hard_constraints.add(index+1);
        NUMHRD++;
      }
      
      index++;
    }

    NUMANZ = NUMVAR + NUMHRD;    
    
    obj_c_sub = new int[NUMVAR];
    obj_c = new double[NUMVAR];
    for (int i = 0; i < NUMVAR; i++){
      obj_c_sub[i] = i;
      if (i % 2 == 0) {
        obj_c[i] = node_features[i/2][2];
      } else {
        obj_c[i] = 0.0;//1.0 - obj_c[i-1];
      }
      //System.out.println(obj_c[i]);
    }
    
    //load quadratic term in objective
    /*
       The lower triangular part of the Q
       matrix in the objective is specified.
      */
            
    qsubi = new int[NUMVAR*(NUMVAR+1)/2];
    qsubj = new int[NUMVAR*(NUMVAR+1)/2];
    qval =  new double[NUMVAR*(NUMVAR+1)/2];
    index = 0;
    double[] rowsum = new double[NUMVAR];
    for (int i = 0; i < NUMVAR; i++){
        rowsum[i] = 0.0;
    }
    for (int i = 0; i < NUMVAR; i++){
      for (int j = 0; j < i; j++){
        qsubi[index] = i;
        qsubj[index] = j;
        int var1 = i/2;
        int var2 = j/2;
        if (i % 2 == j % 2 && Config.useEdgePotentials) {
          qval[index] = 0.0;
          if (Config.useInlinkSim)
            qval[index] += Config.inlinkWt * data.inlink_sim.matrix.get(var1,var2);
          if (Config.useOutlinkSim)
            qval[index] += Config.outlinkWt * data.outlink_sim.matrix.get(var1,var2);
          if (Config.useCategSim)
            qval[index] += Config.categWt * data.category_sim.matrix.get(var1,var2);
          if (Config.useContextSim)
            qval[index] += Config.contextWt * data.context_sim.matrix.get(var1,var2);
        } else {
          if (Config.useDissocPotential && 
              (data.nodes.potentials_set.get(var1).interval.firstInt() ==
               data.nodes.potentials_set.get(var2).interval.firstInt())){
            qval[index] = Config.dissocEdgePotWt;
          } else {
            qval[index] = 0.0;
          }
          
        }
        rowsum[i] += qval[index];
        rowsum[j] += qval[index];
        index++;
      }
    }
        
    //make the matrix negative definite
    for (int i = 0; i < NUMVAR; i++){
        //System.out.println(rowsum[i]);
        qsubi[index] = i;
        qsubj[index] = i;
        qval[index] = -1*rowsum[i];
        obj_c[i] += rowsum[i];
        //System.out.println(obj_c[i]);
        index++;
    }
    
    // load entity names
    node_names = new ArrayList<String>();
    for (NodePotentials np: data.nodes.potentials_set){
      node_names.add(np.name);
    }
  }
  
  public HashMap<String,Double> runDisambQO(){
    // Since the value infinity is never used, we define
    // 'infinity' symbolic purposes only
    double infinity = 0;
            
    HashMap<String,Double> result = null;
    double[] xx   = new double[NUMVAR];
    mosek.Env env = null;
    mosek.Task task = null;
    env = new mosek.Env ();
    
    try
      {
      // Direct the env log stream to the user specified
      // method env_msg_obj.stream
      msgclass env_msg_obj = new msgclass ();
      env.set_Stream (mosek.Env.streamtype.log, env_msg_obj);
      env.init ();
      
      task = new mosek.Task (env,0,0);
      // Directs the log task stream to the user specified
      // method task_msg_obj.stream
      msgclass task_msg_obj = new msgclass ();
      task.set_Stream (mosek.Env.streamtype.log, task_msg_obj);
      task.putobjsense(Env.objsense.maximize);
      /* Give MOSEK an estimate of the size of the input data. 
     This is done to increase the speed of inputting data. 
     However, it is optional. */
      task.putmaxnumvar(NUMVAR);
      task.putmaxnumcon(NUMCON+NUMHRD);
      task.putmaxnumanz(NUMANZ);
      /* Append 'NUMCON' empty constraints.
     The constraints will initially have no bounds. */
      task.append(mosek.Env.accmode.con,NUMCON+NUMHRD);

      /* Append 'NUMVAR' variables.
     The variables will initially be fixed at zero (x=0). */
      task.append(mosek.Env.accmode.var,NUMVAR);

        /* Optionally add a constant term to the objective. */
      task.putcfix(0.0);

        /* Set the linear term c in the objective.*/  
      task.putclist(obj_c_sub,obj_c);

      int hardcon_index = NUMCON;
      for(int j=0; j<NUMVAR; ++j)
      {
        /* Set the bounds on variable j.
           blx[j] <= x_j <= bux[j] */
          task.putbound(mosek.Env.accmode.var,j,mosek.Env.boundkey.ra,0.0,1.0);
          
          /* Input column j of A */   
          task.putaij(j/2,j,1.0);
          
          if (hard_constraints.contains(j)){
            task.putaij(hardcon_index,j,1.0);
            hardcon_index++;
          }
      }
      
      /* Set the bounds on constraints.
       for i=1, ...,NUMCON : blc[i] <= constraint i <= buc[i] */
      for(int i=0; i<NUMCON; ++i)
        task.putbound(mosek.Env.accmode.con,i,mosek.Env.boundkey.fx,1.0,1.0);
        
      for(int i=0; i<NUMHRD; ++i)
        task.putbound(mosek.Env.accmode.con,NUMCON+i,mosek.Env.boundkey.fx,1.0,1.0);

      /* Input the Q for the objective. */

      task.putqobj(qsubi,qsubj,qval);

      /* Solve the problem */
      mosek.Env.rescode r = task.optimize();
      System.out.println (" Mosek warning:" + r.toString());
      // Print a summary containing information
      //   about the solution for debugging purposes
      task.solutionsummary(mosek.Env.streamtype.msg);
      
      mosek.Env.solsta solsta[] = new mosek.Env.solsta[1];
      mosek.Env.prosta prosta[] = new mosek.Env.prosta[1];
      /* Get status information about the solution */ 
      task.getsolutionstatus(mosek.Env.soltype.itr,
                             prosta,
                             solsta);
                /* Get the solution */
      task.getsolutionslice(mosek.Env.soltype.itr, // Interior solution.     
                            mosek.Env.solitem.xx,  // Which part of solution.
                            0,      // Index of first variable.
                            NUMVAR, // Index of last variable+1 
                            xx);
                
      switch(solsta[0])
      {
      case optimal:
      case near_optimal:      
        /*System.out.println("Optimal primal solution\n");
        for(int j = 0; j < NUMVAR; ++j)
          System.out.println ("x[" + j + "]:" + xx[j]);*/
        result = new HashMap<String,Double>();
        for(int j = 0; j < node_names.size(); ++j) {
          result.put(node_names.get(j),xx[2*j]);
        }
        break;
      case dual_infeas_cer:
      case prim_infeas_cer:
      case near_dual_infeas_cer:
      case near_prim_infeas_cer:  
        System.out.println("Primal or dual infeasibility\n");
        break;
      case unknown:
        System.out.println("Unknown solution status.\n");
        break;
      default:
        System.out.println("Other solution status");
        break;
      }
    }
    catch (mosek.Exception e)
    {
      System.out.println ("An error/warning was encountered");
      System.out.println (e.toString());
    }
    catch (java.lang.Exception e)
    {
      System.out.println ("An error/warning was encountered");
      System.out.println (e.toString());
    }
    
    if (task != null) task.dispose ();
    if (env  != null)  env.dispose ();
    
    return result;
  }
  
  
  public static void main (String[] args){
    TrainingData dummy_data = new TrainingData();
    EdgePotentialsMatrix dummy_inlink = new EdgePotentialsMatrix(4);
    NodePotentials dummy_np = null;
    
    dummy_np = new NodePotentials();
    dummy_np.name = "A";
    dummy_np.inlink_count = 100;
    dummy_data.nodes.potentials_set.add(dummy_np);
    dummy_np = new NodePotentials();
    dummy_np.name = "B";
    dummy_np.inlink_count = 1000;
    dummy_np.label = -1;
    dummy_data.nodes.potentials_set.add(dummy_np);
    dummy_np = new NodePotentials();
    dummy_np.name = "C";
    dummy_np.inlink_count = 10000;
    dummy_data.nodes.potentials_set.add(dummy_np);
    dummy_np = new NodePotentials();
    dummy_np.name = "D";
    dummy_np.inlink_count = 100;
    dummy_data.nodes.potentials_set.add(dummy_np);
    
    dummy_inlink.matrix.set(0,0,0.0);
    dummy_inlink.matrix.set(0,1,1.0);
    dummy_inlink.matrix.set(0,2,10.0);
    dummy_inlink.matrix.set(0,3,0.0);
    dummy_inlink.matrix.set(1,0,1.0);
    dummy_inlink.matrix.set(1,1,0.0);
    dummy_inlink.matrix.set(1,2,0.0);
    dummy_inlink.matrix.set(1,3,10.0);
    dummy_inlink.matrix.set(2,0,10.0);
    dummy_inlink.matrix.set(2,1,0.0);
    dummy_inlink.matrix.set(2,2,0.0);
    dummy_inlink.matrix.set(2,3,2.0);
    dummy_inlink.matrix.set(3,0,0.0);
    dummy_inlink.matrix.set(3,1,10.0);
    dummy_inlink.matrix.set(3,2,2.0);
    dummy_inlink.matrix.set(3,3,0.0);
    
    dummy_data.inlink_sim = dummy_inlink;
    
    Config.useInlinkCount = true;
    Config.useLogisticScore = false;
    Config.useEdgePotentials = true;
    Config.useInlinkSim = true;
    
    QuadraticOptimizer qo = new QuadraticOptimizer();
    qo.loadEntityData(dummy_data);
    HashMap<String,Double> disamb = qo.runDisambQO();
    if (disamb == null) {
        System.out.println("Disambiguation failed");    
        return;
    }
    System.out.println("Disambiguation results:");
    for (String key: disamb.keySet()){
        System.out.println(key + ": " + disamb.get(key));
    }
  } 
}
