import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

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
		// subject number
		int N = 20;
		// room number
		int M = 8;
		// edge constraint
		int E = 23;
		// max session
		int K = 100;
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
		MPVariable[][] x = new MPVariable[N][K];
		MPVariable[][] y = new MPVariable[N][M];
		MPVariable[] z = new MPVariable[K];
		for (int i = 0; i < N; i++) {
			for(int j=0;j<K;j++) {
				x[i][j] = solver.makeIntVar(0, 1, "x["+i+"]["+j+"]");
			}
			for(int j=0;j<M;j++) {
				y[i][j] = solver.makeIntVar(0, 1, "y["+i+"]["+j+"]");
			}
		}
		for (int i = 0; i < K; i++) {
			z[i]= solver.makeIntVar(0,1,"z["+i+"]");
		}
		// add constraint 1
		MPConstraint[] constraint1 = new MPConstraint[N];
		for (int i = 0; i < N; i++) {
			constraint1[i] = solver.makeConstraint(1, 1);
			for (int j = 0; j < K; j++) {
				constraint1[i].setCoefficient(x[i][j], 1);
			}
		}
		//add constraint 2
		MPConstraint[] constraint2 = new MPConstraint[N];
		for (int i = 0; i < N; i++) {
			constraint2[i] = solver.makeConstraint(d[i], infinity);
			for (int j = 0; j < M; j++) {
				constraint2[i].setCoefficient(y[i][j], c[j]);
			}
		}
		//add constraint 3
		MPConstraint[][] constraint3 = new MPConstraint[E][K];
		for (int i = 0; i < E; i++) {
			for (int j = 0; j < K; j++) {
				constraint3[i][j] = solver.makeConstraint(-infinity, 0);
				constraint3[i][j].setCoefficient(x[edge[i].a][j], 1);
				constraint3[i][j].setCoefficient(x[edge[i].b][j], 1);
				constraint3[i][j].setCoefficient(z[j], -1);
			}
		}
		//add constraint 4
		MPConstraint[][][][] constraint4 = new MPConstraint[N][N][K][M];
		for (int i1 = 0; i1 < N-1; i1++) {
			for(int i2 = i1+1; i2 < N;i2++) {
				for(int j=0;j<K;j++) {
					for (int k = 0; k < M; k++) {
						constraint4[i1][i2][j][k] = solver.makeConstraint(-infinity, 2 * infinity + 1);
						constraint4[i1][i2][j][k].setCoefficient(x[i1][j], infinity);
						constraint4[i1][i2][j][k].setCoefficient(x[i2][j], infinity);
						constraint4[i1][i2][j][k].setCoefficient(y[i1][k], 1);
						constraint4[i1][i2][j][k].setCoefficient(y[i2][k], 1);
					}
				}
			}
		}

		//add objective
		MPObjective obj = solver.objective();
		for (MPVariable objVar : z) {
			obj.setCoefficient(objVar, 1);
		}
//		MPObjective obj2 = solver.objective();
//		for (int i = 0; i < N; i++) {
//			for (int j = 0; j < K; j++) {
//				obj2.setCoefficient(y[i][j], 1);
//			}
//		}
		obj.setMinimization();
//		obj2.setMinimization();
		final MPSolver.ResultStatus resultStatus = solver.solve();
		if (resultStatus != MPSolver.ResultStatus.ABNORMAL) {
			System.err.println("The problem does not have an optimal solution!");
			return;
		} else {
			System.out.print("ket qua:" + "\n");
			for (int i = 0; i < N; i++) {
				System.out.print("subject: " + i);
				for (int j = 0; j < K; j++) {
					if (x[i][j].solutionValue() > 0) {
						System.out.print(" session: " + j);
						break;
					}
				}
				System.out.print("room: ");
				for (int j = 0; j < M; j++) {
					if (y[i][j].solutionValue() > 0) {
						System.out.print(j + "  ");
					}
				}
				System.out.print("\n");
			}
		}
		return;
	}

	public static void main(String[] args) {
//		try {
//			System.out.println("---- Integer programming example with SCIP (recommended) ----");
//			solve("SCIP_MIXED_INTEGER_PROGRAMMING");
//		} catch (java.lang.IllegalArgumentException e) {
//			System.err.println("Bad solver type: " + e);
//		}
		try {
			System.out.println("---- Integer programming example with CBC ----");
			solve("CBC_MIXED_INTEGER_PROGRAMMING");
		} catch (java.lang.IllegalArgumentException e) {
			System.err.println("Bad solver type: " + e);
		}
//		try {
//			System.out.println("---- Integer programming example with GLPK ----");
//			solve("GLPK_MIXED_INTEGER_PROGRAMMING");
//		} catch (java.lang.IllegalArgumentException e) {
//			System.err.println("Bad solver type: " + e);
//		}
	}
}
