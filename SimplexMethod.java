import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimplexMethod {
    public static Object[] initializeTableau(double[] C, double[][] A, double[] b) {
        int nConstraints = A.length;
        int nVariables = C.length;

        List<List<Double>> tableau = new ArrayList<>();

        for (int i = 0; i < nConstraints; i++) {
            List<Double> row = new ArrayList<>();
            for (double value : A[i]) {
                row.add(value);
            }
            for (int j = 0; j < nConstraints; j++) {
                row.add(0.0);
            }
            row.add(b[i]);
            row.set(nVariables + i, 1.0);
            tableau.add(row);
        }

        List<Double> costRow = new ArrayList<>();
        for (double ci : C) {
            costRow.add(-ci);
        }
        for (int j = 0; j < nConstraints + 1; j++) {
            costRow.add(0.0);
        }
        tableau.add(costRow);

        List<Integer> basis = new ArrayList<>();
        for (int i = 0; i < nConstraints; i++) {
            basis.add(nVariables + i);
        }

        return new Object[]{tableau, basis};
    }

    public static Integer pivotColumn(List<List<Double>> tableau) {
        List<Double> lastRow = tableau.get(tableau.size() - 1);
        double minValue = Double.POSITIVE_INFINITY;
        Integer colIndex = null;

        for (int i = 0; i < lastRow.size() - 1; i++) {
            if (lastRow.get(i) < minValue) {
                minValue = lastRow.get(i);
                colIndex = i;
            }
        }

        if (minValue >= 0) {
            return null;
        }

        return colIndex;
    }

    public static Integer pivotRow(List<List<Double>> tableau, int col) {
        int nConstraints = tableau.size() - 1;
        List<Double> rhs = new ArrayList<>();
        List<Double> lhs = new ArrayList<>();
        List<Double> ratios = new ArrayList<>();

        for (int i = 0; i < nConstraints; i++) {
            rhs.add(tableau.get(i).get(tableau.get(i).size() - 1));
            lhs.add(tableau.get(i).get(col));
        }

        boolean unbounded = true;

        for (int i = 0; i < nConstraints; i++) {
            if (lhs.get(i) > 0) {
                ratios.add(rhs.get(i) / lhs.get(i));
                unbounded = false;
            } else {
                ratios.add(Double.POSITIVE_INFINITY);
            }
        }

        if (unbounded) {
            return null;

        }

        double minRatio = Double.POSITIVE_INFINITY;
        Integer rowIndex = null;

        for (int i = 0; i < ratios.size(); i++) {
            if (ratios.get(i) < minRatio) {
                minRatio = ratios.get(i);
                rowIndex = i;
            }
        }

        if (minRatio == Double.POSITIVE_INFINITY) {
            return null;
        }

        return rowIndex;
    }


    public static void pivot(List<List<Double>> tableau, int row, int col) {
        double pivotValue = tableau.get(row).get(col);
        for (int j = 0; j < tableau.get(row).size(); j++) {
            tableau.get(row).set(j, tableau.get(row).get(j) / pivotValue);
        }

        for (int i = 0; i < tableau.size(); i++) {
            if (i != row) {
                double factor = tableau.get(i).get(col);
                for (int j = 0; j < tableau.get(i).size(); j++) {
                    tableau.get(i).set(j, tableau.get(i).get(j) - factor * tableau.get(row).get(j));
                }
            }
        }
    }

    public static Object[] simplexMethod(double[] C, double[][] A, double[] b, int precision, double epsilon) {
        Object[] initResult = initializeTableau(C, A, b);
        List<List<Double>> tableau = (List<List<Double>>) initResult[0];
        List<Integer> basis = (List<Integer>) initResult[1];
        double prevZ = Double.NEGATIVE_INFINITY;

        while (true) {
            Integer col = pivotColumn(tableau);
            if (col == null) {
                double[] solution = new double[C.length];
                for (int i = 0; i < basis.size(); i++) {
                    if (basis.get(i) < C.length) {
                        solution[basis.get(i)] = tableau.get(i).get(tableau.get(i).size() - 1);
                    }
                }
                solution = Arrays.stream(solution)
                        .map(val -> Math.round(val * Math.pow(10, precision)) / Math.pow(10, precision))
                        .toArray();
                double optimalValue = Math.round(-tableau.get(tableau.size() - 1).get(tableau.get(0).size() - 1) * Math.pow(10, precision)) / Math.pow(10, precision);
                return new Object[]{solution, optimalValue};
            }

            Integer row = pivotRow(tableau, col);
            if (row == null) {
                return new Object[]{"Unbounded solution"};
            }

            basis.set(row, col);
            pivot(tableau, row, col);

            double currentZ = -tableau.get(tableau.size() - 1).get(tableau.get(0).size() - 1);

            if (Math.abs(currentZ - prevZ) < epsilon) {
                if (tableau.get(tableau.size() - 1).stream().anyMatch(value -> value < 0)) {
                    return new Object[]{"The method is not applicable!"};
                }
            }

            prevZ = currentZ;
        }
    }


    public static void runTests() {
        double[][][] testsA = {
                {{18, 15, 12}, {6, 4, 8}, {5, 3, 3}},
                {{1, -1, 0, 1}, {1, -2, 1, 0}},
                {{1, -2}, {-1, -1}, {1, -1}, {0, 1}},
                {{4, 1}, {-1, 1}},
                {{6, 4}, {1, 2}, {-1, 1}, {0, 1}}
        };
        double[][] testsC = {
                {9, 10, 16},
                {1, 2, -1, 1},
                {1, 2},
                {3, 4},
                {5, 4}
        };
        double[][] testsB = {
                {360, 192, 180},
                {1, 1},
                {-2, -4, 2, 6},
                {8, 3},
                {24, 6, 1, 2}
        };
        int precision = 6;
        double epsilon = 1e-6;

        for (int i = 0; i < testsA.length; i++) {
            System.out.println("Test " + (i + 1) + ":");
            Object[] result = simplexMethod(testsC[i], testsA[i], testsB[i], precision, epsilon);
            if (result[0] instanceof String) {
                System.out.println(result[0]);
            } else {
                double[] solution = (double[]) result[0];
                double optimalValue = (double) result[1];

                System.out.println("Solution: " + Arrays.toString(solution));
                System.out.println("Optimal Value: " + optimalValue);
            }
            System.out.println("------------------------------");
        }
    }


    public static void main(String[] args) {
        runTests();
    }
}
