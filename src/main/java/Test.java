import mandarin.packpack.supporter.calculation.Matrix;

import java.math.BigDecimal;
import java.util.Random;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Test {
    public static void main(String[] args) throws Exception {
        int n = 500;

        Matrix a = new Matrix(n, n);
        Matrix b = new Matrix(n, 1);

        Random r = new Random();

        int min = 0;
        int max = 100;

        for(int x = 0; x < a.getRow(); x++) {
            for(int y = 0; y < a.getColumn(); y++) {
                a.setValue(x, y, BigDecimal.valueOf(min + (max - min) * r.nextDouble()));
            }

            b.setValue(x, 0, BigDecimal.valueOf(min + (max - min) * r.nextDouble()));
        }

        long start = System.currentTimeMillis();

        Matrix.solvePolynomial(a, b, Matrix.ALGORITHM.LU);

        long end = System.currentTimeMillis();

        System.out.printf("%d equations\nLU Decomposition (%.4g sec)", n, (end - start) / 1000.0);
    }
}
