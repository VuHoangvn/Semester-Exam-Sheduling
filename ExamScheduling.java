import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.LinearExpr;
import java.util.Collections;
import java.util.Arrays;
import java.util.Collection;

public class ExamScheduling {
	public static class Edge {
		public int a, b;

		public Edge(int a, int b) {
			this.a = a;
			this.b = b;
		}
	}

	static {
		System.loadLibrary("jniortools");
	}

	private static MPSolver createSolver(String solverType) {
		return new MPSolver("MIPDiet", MPSolver.OptimizationProblemType.valueOf(solverType));
	}

	private static void solve(String solverType) {
		MPSolver solver = createSolver(solverType);
		double infinity = MPSolver.infinity();

		/** invariants */
		int T = 6; // max time slot 
		int S = 20; // number of subjects
		int E = 23; // number of constraint two subjects
		int R = 8; // number of rooms

		Edge[] edges = { 
				new Edge(0, 16), 
				new Edge(1, 2), 
				new Edge(1, 6), 
				new Edge(1, 7), 
				new Edge(1, 8),
				new Edge(2, 11), 
				new Edge(2, 16), 
				new Edge(2, 17), 
				new Edge(3, 14), 
				new Edge(3, 16), 
				new Edge(3, 17),
				new Edge(4, 7), 
				new Edge(4, 13), 
				new Edge(4, 17), 
				new Edge(5, 6), 
				new Edge(5, 11), 
				new Edge(6, 18),
				new Edge(9, 12), 
				new Edge(10, 13), 
				new Edge(11, 17), 
				new Edge(13, 15), 
				new Edge(15, 17),
				new Edge(16, 19) };
		//  number of student in subject
		int[] d = { 90, 66, 129, 81, 167, 176, 83, 109, 87, 126, 30, 107, 67, 58, 49, 133, 41, 94, 150, 87 };
		// max student in room
		int[] c = { 193, 95, 195, 39, 172, 173, 29, 53 };

		/** variables */
		// x[s][t] = 1 when subject s is organized in the t(th) exam
		MPVariable[][] x = new MPVariable[S][T];
		for (Integer s = 0; s < S; s++) {
			x[s] = solver.makeBoolVarArray(T);
		}
		// y[s][r] = 1 when subject s is organized in room r
		MPVariable[][] y = new MPVariable[S][R];
		for (Integer s = 0; s < S; s++) {
			y[s] = solver.makeBoolVarArray(R);
		}
		// z[t] = 1 when time slot t is used
		MPVariable[] z = solver.makeBoolVarArray(T);
		MPVariable v = solver.makeIntVar(0,T ,"v") ;
		// minimize number of time slots are used
		MPObjective obj = solver.objective();
//		obj.setCoefficient(v, 1);
		for (MPVariable objVar : z) {
			obj.setCoefficient(objVar, 1);
		}

		/** add constraint 1 */
		//SUMt (x[s][t]) = 1 
		MPConstraint[] constraint1 = new MPConstraint[S];
		for (int s = 0; s < S; s++) {
			constraint1[s] = solver.makeConstraint(1, 1);
			for (int t = 0; t < T; t++) {
				constraint1[s].setCoefficient(x[s][t], 1);
			}
		}

		/** add constraint 2 */
		//SUMr (y[s][r]*c[r] >= d[s]
		MPConstraint[] constraint2 = new MPConstraint[S];
		for (int s = 0; s < S; s++) {
			constraint2[s] = solver.makeConstraint(d[s], infinity);
			for (int r = 0; r < R; r++) {
				constraint2[s].setCoefficient(y[s][r], c[r]);
			}
		}

		/** add constraint 3 */
		// x[s1][t] + s[s2][t] <= z[t] <=> x[s1][t] + x[s2][t] <= 1 and z[t] >= x[s][t
		MPConstraint[][] constraint3 = new MPConstraint[E][T];
		for (int i = 0; i < E; i++) {
			for (int t = 0; t < T; t++) {
				constraint3[i][t] = solver.makeConstraint(-infinity, 0);
				constraint3[i][t].setCoefficient(x[edges[i].a][t], 1);
				constraint3[i][t].setCoefficient(x[edges[i].b][t], 1);
				constraint3[i][t].setCoefficient(z[t], -1);
			}
		}

		/** add constraint 4 */
		// x[s1][t] + x[s2][t] = 2 => y[s1][r] + y[s2][r] <= 1
		int bigM = 50;
		MPConstraint[][][][] constraint4 = new MPConstraint[S][S][T][R];
		for (int s1 = 0; s1 < S; s1++) {
			for (int s2 = s1 + 1; s2 < S; s2++) {
				for (int t = 0; t < T; t++) {
					for (int r = 0; r < R; r++) {
						if (s1 != s2) {
							constraint4[s1][s2][t][r] = solver.makeConstraint(0, 2 * bigM + 1);
							constraint4[s1][s2][t][r].setCoefficient(x[s1][t], bigM);
							constraint4[s1][s2][t][r].setCoefficient(x[s2][t], bigM);
							constraint4[s1][s2][t][r].setCoefficient(y[s1][r], 1);
							constraint4[s1][s2][t][r].setCoefficient(y[s2][r], 1);
						} else {
						}
					}
				}
			}
		}

		/** Minimize by default */
		obj.setMinimization();
		final MPSolver.ResultStatus resultStatus = solver.solve();
		
		/** printing */
		if (resultStatus != MPSolver.ResultStatus.OPTIMAL) {
			System.err.println("The problem does not have an optimal solution!");
			return;
		} else {
			int[] swap_array = new int[T];
			int	count = 0;
			for (int t=0;t<T;t++) {
				if(z[t].solutionValue() > 0) {
					swap_array[t] = count;
					count++;
				}
			}
			int[] t_subject = new int[S];
			for (int s = 0; s < S; s++) {
				for (int t = 0; t < T; t++) {
					if (x[s][t].solutionValue() > 0) {
						t_subject[s] = swap_array[t];
						break;
					}
				}
			}
			System.out.print("Result:" + "\n");
			for (int s = 0; s < S; s++) {
				System.out.print("subject: " + s + "  time slot: " +t_subject[s]);
				System.out.print(" room: ");
				for (int r = 0; r< R; r++) {
					if (y[s][r].solutionValue() > 0) {
						System.out.print(r + "  ");
					}
				}
				System.out.print("\n");
				
			}
		}
		return;
	}
	
	// model Mip 2
	private static void solve2(String solverType) {
		MPSolver solver = createSolver(solverType);
		double infinity = MPSolver.infinity();
		// subject number
		int S = 20;
		// room number
		int R = 8;
		// edge constraint
		int E = 23;
		// max time slot
		int T = 100;
		// edge constraint
		Edge[] edge = {
			new Edge(0, 16),
			new Edge(1, 2),
			new Edge(1, 6),
			new Edge(1, 7),
			new Edge(1, 8),
			new Edge(2, 11),
			new Edge(2, 16),
			new Edge(2, 17),
			new Edge(3, 14),
			new Edge(3, 16),
			new Edge(3, 17),
			new Edge(4, 7),
			new Edge(4, 13),
			new Edge(4, 17),
			new Edge(5, 6),
			new Edge(5, 11),
			new Edge(6, 18),
			new Edge(9, 12),
			new Edge(10, 13),
			new Edge(11, 17),
			new Edge(13, 15),
			new Edge(15, 17),
			new Edge(16, 19)
		};
		//student number in subject
		int[] d = {90, 66, 129, 81, 167, 176, 83, 109, 87, 126, 30, 107, 67, 58, 49, 133, 41, 94, 150, 87};
		//max student in room
		int[] c = {193, 95, 195, 39, 172, 173, 29, 53};

		MPVariable[][][] X = new MPVariable[S][T][R];
		MPVariable[][] Y = new MPVariable[S][T];
		MPVariable[] Z = new MPVariable[T];
		
		//add var x
		for(int s=0; s<S;s++) {
			for(int t=0;t<T;t++) {
				for(int r= 0; r<R;r++) {
					X[s][t][r] = solver.makeIntVar(0, 1, "x["+s+"]["+t+"]["+r+"]");
				}
			}
		}
		//add var y
		for(int s=0;s<S;s++){
			for(int t=0;t<T;t++) {
				Y[s][t] = solver.makeIntVar(0,1,"y["+s+"]["+t+"]");
			}
		}
		// add var z
		for(int t=0;t<T;t++) {
			Z[t] = solver.makeIntVar(0, 1, "x["+t+"]");
		}
		// add constraint1
		MPConstraint[] constraint1 = new MPConstraint[S];
		for(int s=0; s<S;s++) {
			constraint1[s] = solver.makeConstraint(1,1);
			for (int t=0;t<T;t++) {
				constraint1[s].setCoefficient(Y[s][t], 1);
			}
		}
		// add constraint2
		MPConstraint[][] constraint2 = new MPConstraint[E][T];
		for (int i=0;i<edge.length;i++) {
			for(int t=0;t<T;t++) {
				constraint2[i][t] = solver.makeConstraint(-1, 0);
				constraint2[i][t].setCoefficient(Y[edge[i].a][t], 1);
				constraint2[i][t].setCoefficient(Y[edge[i].b][t], 1);
				constraint2[i][t].setCoefficient(Z[t], -1);
			}
		}
		// add constraint3
		MPConstraint[][] constraint3 = new MPConstraint[T][R];
		for (int t = 0; t < T; t++) {
			for (int r = 0; r < R; r++) {
				constraint3[t][r] = solver.makeConstraint(0, 1);
				for (int s = 0; s < S; s++) {
					constraint3[t][r].setCoefficient(X[s][t][r],1);
				}
			}
		}
		//add constraint4
		MPConstraint[][][] constraint4 = new MPConstraint[S][T][R];
		for(int s=0;s<S;s++) {
			for(int t=0;t<T;t++) {
				for(int r=0;r<R;r++) {
					constraint4[s][t][r] = solver.makeConstraint(0, 1);
					constraint4[s][t][r].setCoefficient(Y[s][t], 1);
					constraint4[s][t][r].setCoefficient(X[s][t][r], -1);
 				}
			}
		}
		// add constraint5
		MPConstraint[][] constraint5 = new MPConstraint[S][T];
		for(int s=0;s<S;s++) {
			for(int t=0;t<T;t++) {
				constraint5[s][t] = solver.makeConstraint(0, infinity);
				for(int r=0;r<R;r++) {
					constraint5[s][t].setCoefficient(X[s][t][r], c[r]);
				}
				constraint5[s][t].setCoefficient(Y[s][t], -d[s]);
			}
		}
  		//add objective
		MPObjective obj = solver.objective();
		for( MPVariable v : Z) {
			obj.setCoefficient(v, 1);
		}
		obj.setMinimization();
		final MPSolver.ResultStatus resultStatus = solver.solve();
		if (resultStatus != MPSolver.ResultStatus.ABNORMAL) {
			System.err.println("The problem does not have an optimal solution!");
			return;
		} else {
			System.out.print("ket qua:" + "\n");
			for (int s = 0; s < S; s++) {
				System.out.print("subject: " + s);
				for (int t = 0; t < T; t++) {
					if (Y[s][t].solutionValue() > 0) {
						System.out.print(" time slot: " + t);
						System.out.print("room: ");
						for (int r = 0; r< R; r++) {
							if (X[s][t][r].solutionValue() > 0) {
								System.out.print(r + "  ");
							}
						}
						System.out.print("\n");
						break;
					}
				}
				
			}
		}
		return;
	}

	// constrain solver
	private static void cp_solver(int S,int R,int T,int E,Edge[] edges,int[] d, int[] c) {
		int[][]	e_matrix = new int[S][S];
		for(int e=0;e<E;e++) {
			e_matrix[edges[e].a][edges[e].b] = 1;
		}
		CpModel model = new CpModel();
		IntVar[] x = new IntVar[S];
		IntVar[][] y = new IntVar[S][R];
		for(int s=0;s<S;s++) {
			x[s] = model.newIntVar(0, T-1,"x["+s+"]");
			for(int r=0;r<R;r++) {
				y[s][r] = model.newIntVar(0,1,"y["+s+"]["+r+"]");
			}
		}
		// x[ea] != x[eb]
		for(int e=0;e<E;e++) {
			model.addDifferent(x[edges[e].a], x[edges[e].b]);
		}
		// Room(S) >= d(s)
		for(int s=0;s<S;s++) {
			model.addGreaterOrEqual(LinearExpr.scalProd(y[s],c), d[s]);
		}
		
		// bool b1
		// bool b2
		IntVar[][] b1 = new IntVar[S][S];
		IntVar[][][] b2 = new IntVar[S][S][R];
		for(int s1=0;s1<S;s1++) {
			for(int s2=0;s2<S;s2++) {
				b1[s1][s2] = model.newBoolVar("b1_"+s1+"_"+s2);
				for(int r=0;r<R;r++) {
					b2[s1][s2][r] = model.newBoolVar("b2_"+s1+"_"+s2+"_"+r);
				}
			}
		}
		// b1 = x[s1] == x[s2]
		for(int s1=0;s1<S;s1++) {
			for(int s2=0;s2<S;s2++) {
				if(s1 != s2) {
					model.addEquality(x[s1], x[s2]).onlyEnforceIf(b1[s1][s2]);
					model.addDifferent(x[s1], x[s2]).onlyEnforceIf(b1[s1][s2].not());
					for(int r=0;r<R;r++) {
						//b2 = y[s1][r]+y[s2][r] <= 1
						model.addLessOrEqual(LinearExpr.sum(new IntVar[] {y[s1][r], y[s2][r]}), 1).onlyEnforceIf(b2[s1][s2][r]);;
						model.addGreaterThan(LinearExpr.sum(new IntVar[] {y[s1][r], y[s2][r]}), 1).onlyEnforceIf(b2[s1][s2][r].not());
						// x[s1] == x[s2] => y[s1][r] + y[s2][r] <= 1
						model.addImplication(b1[s1][s2], b2[s1][s2][r]);
				}
				}
			}
		}
//		 heuristic constraint		
//		IntVar[][] z = new IntVar[S][R];
//		for(int s=0;s<S;s++) {
//			for(int r=0;r<R;r++) {
//				z[s][r] = model.newIntVar(1, 1000, "z["+s+"]["+r+"]");
//			}
//		}
//		IntVar[][] b3 = new IntVar[S][R];
//		for(int s=0;s<S;s++) {
//			for(int r=0;r<R;r++) {
//				b3[s][r] = model.newBoolVar("b3["+s+"]["+r+"]");
//				model.addEquality(y[s][r], 1).onlyEnforceIf(b3[s][r]);
//				model.addLessThan(y[s][r], 1).onlyEnforceIf(b3[s][r].not());
//				model.addEquality(z[s][r], c[r]).onlyEnforceIf(b3[s][r]);
//				model.addEquality(z[s][r], 1000).onlyEnforceIf(b3[s][r].not());
//			}
//		}
//		IntVar[] minc = new IntVar[S];
//		for(int s=0;s<S;s++) {
//			minc[s] = model.newIntVar(1, 1000, "minc["+s+"]");
//			model.addMinEquality(minc[s], z[s]);
//			model.addLessOrEqualWithOffset(LinearExpr.scalProd(y[s],c),minc[s], -d[s]);
//		}
		// add objective
		
		IntVar obj ;
		obj = model.newIntVar(0, T-1, "obj");
		model.addMaxEquality(obj, x);
		model.minimize(obj);
		
		CpSolver solver = new CpSolver();
		CpSolverStatus status = solver.solve(model);
		System.out.println("--------------CP_Model--------------");
		System.out.println("solve status: " + status);
		if(status == CpSolverStatus.OPTIMAL) {
			for (int s = 0; s < S; s++) {
				System.out.print("subject: " + s + "  time slot: " + solver.value(x[s]));
				System.out.print(" room: ");
				for (int r = 0; r< R; r++) {
					if (solver.value(y[s][r]) > 0) {
						System.out.print(r + "  ");
					}
				}
				System.out.print("\n");
				
			}
		}
		else {
			System.out.print("don't have solution!!!!!!");
		}
		return;
	}
		
		
	
	public static void main(String[] args) {
		int T = 20; // max time slot 
		int S = 20; // number of subjects
		int E = 23; // number of constraint two subjects
		int R = 8; // number of rooms

		Edge[] edges = { 
				new Edge(0, 16), 
				new Edge(1, 2), 
				new Edge(1, 6), 
				new Edge(1, 7), 
				new Edge(1, 8),
				new Edge(2, 11), 
				new Edge(2, 16), 
				new Edge(2, 17), 
				new Edge(3, 14), 
				new Edge(3, 16), 
				new Edge(3, 17),
				new Edge(4, 7), 
				new Edge(4, 13), 
				new Edge(4, 17), 
				new Edge(5, 6), 
				new Edge(5, 11), 
				new Edge(6, 18),
				new Edge(9, 12), 
				new Edge(10, 13), 
				new Edge(11, 17), 
				new Edge(13, 15), 
				new Edge(15, 17),
				new Edge(16, 19) };
		//  number of student in subject
		int[] d = { 90, 66, 129, 81, 167, 176, 83, 109, 87, 126, 30, 107, 67, 58, 49, 133, 41, 94, 150, 87 };
		// max student in room
		int[] c = { 193, 95, 195, 39, 172, 173, 29, 53 };
//		try {
//			System.out.println("---- Integer programming example with SCIP (recommended) ----");
//			solve("SCIP_MIXED_INTEGER_PROGRAMMING");
//		} catch (java.lang.IllegalArgumentException e) {
//			System.err.println("Bad solver type: " + e);
//		}
//		try {
//			System.out.println("---- Integer programming example with CBC ----");
//			solve("CBC_MIXED_INTEGER_PROGRAMMING");
//		} catch (java.lang.IllegalArgumentException e) {
//			System.err.println("Bad solver type: " + e);
//		}
//		try {
//			System.out.println("---- Integer programming example with GLPK ----");
//			solve("GLPK_MIXED_INTEGER_PROGRAMMING");
//		} catch (java.lang.IllegalArgumentException e) {
//			System.err.println("Bad solver type: " + e);
//		}
		long time_start = System.currentTimeMillis();
		cp_solver(S,R, T, E, edges, d, c);
		long time_end = System.currentTimeMillis();
		System.out.print("\n"+(time_end - time_start)/1000.0);
	}
}