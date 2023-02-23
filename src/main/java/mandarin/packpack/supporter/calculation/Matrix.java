package mandarin.packpack.supporter.calculation;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.MathContext;

public class Matrix {
    public enum ALGORITHM {
        GAUSSIAN,
        LU
    }

    public static Matrix solvePolynomial(Matrix a, Matrix b, ALGORITHM algorithm) {
        if(a.getRow() != a.getColumn()) {
            throw new IllegalStateException(String.format("A Matrix must have same row and column : A = %dx%d Matrix", a.getRow(), a.getColumn()));
        }

        if(b.getRow() != a.getRow()) {
            throw new IllegalStateException(String.format("A Matrix must have same row of B matrix\nA = %dx%d Matrix\nB = %dx%d Matrix", a.getRow(), a.getColumn(), b.getRow(), b.getColumn()));
        }

        if(b.getColumn() != 1) {
            throw new IllegalStateException(String.format("B Matrix must have only 1 column : B = %dx%d Matrix", b.getRow(), b.getColumn()));
        }

        switch (algorithm) {
            case GAUSSIAN:
                return a.attachColumn(b).gaussianElimination().extractColumn(a.getColumn() - 1);
            case LU:
                int n = a.getRow();

                Matrix[] lu = a.LUDecomposition();

                Matrix d = new Matrix(n, 1);
                Matrix x = new Matrix(n, 1);

                for(int i = 0; i < n; i++) {
                    BigDecimal value = b.getValue(i, 0);

                    for(int j = 0; j < i; j++) {
                        value = value.subtract(lu[0].getValue(i, j).multiply(d.getValue(j, 0)));
                    }

                    value = value.divide(lu[0].getValue(i, i), Equation.context);

                    if(value.abs().compareTo(Number.cutOff) < 0)
                        value = BigDecimal.ZERO;

                    d.setValue(i, 0, value);
                }

                for(int i = n - 1; i >= 0; i--) {
                    BigDecimal value = d.getValue(i, 0);

                    for(int j = n - 1; j > i; j--) {
                        value = value.subtract(lu[1].getValue(i, j).multiply(x.getValue(j, 0)));
                    }

                    value = value.divide(lu[1].getValue(i, i), Equation.context);

                    if(value.abs().compareTo(Number.cutOff) < 0)
                        value = BigDecimal.ZERO;

                    x.setValue(i, 0, value);
                }

                return x;
            default:
                throw new IllegalStateException("Unknown algorithm type specified : " + algorithm);
        }
    }

    @Nonnull
    private BigDecimal[][] matrix;

    public Matrix(int row, int col) {
        if (row <= 0) {
            throw new IllegalStateException(String.format("Row must be positive integer, the value passed : %d, %d", row, col));
        }

        if (col <= 0) {
            throw new IllegalStateException(String.format("Column must be positive integer, the value passed : %d, %d", row, col));
        }

        matrix = new BigDecimal[row][col];

        for(int x = 0; x < row; x++) {
            BigDecimal[] r = new BigDecimal[col];

            for(int y = 0; y < col; y++) {
                r[y] = BigDecimal.valueOf(0);
            }

            matrix[x] = r;
        }
    }

    public int getRow() {
        return matrix.length;
    }

    public int getColumn() {
        return matrix[0].length;
    }

    public void setValue(int row, int col, BigDecimal value) {
        if (row < 0 || row >= getRow()) {
            throw new IllegalStateException(String.format("Row is out of range, this matrix is %dx%d", getRow(), getColumn()));
        }

        if (col < 0 || col >= getColumn()) {
            throw new IllegalStateException(String.format("Column is out of range, this matrix is %dx%d", getRow(), getColumn()));
        }

        matrix[row][col] = value;
    }

    public BigDecimal getValue(int row, int col) {
        if (row < 0 || row >= getRow()) {
            throw new IllegalStateException(String.format("Row is out of range, this matrix is %dx%d", getRow(), getColumn()));
        }

        if (col < 0 || col >= getColumn()) {
            throw new IllegalStateException(String.format("Column is out of range, this matrix is %dx%d", getRow(), getColumn()));
        }

        return matrix[row][col];
    }

    public Matrix add(Matrix m) {
        if (getRow() != m.getRow()) {
            throw new IllegalStateException(String.format("This matrix has different row from target matrix\nSource : %dx%d\nTarget : %dx%d", getRow(), getColumn(), m.getRow(), m.getColumn()));
        }

        if (getColumn() != m.getColumn()) {
            throw new IllegalStateException(String.format("This matrix has different column from target matrix\nSource : %dx%d\nTarget : %dx%d", getRow(), getColumn(), m.getRow(), m.getColumn()));
        }

        for(int x = 0; x < getRow(); x++) {
            for(int y = 0; y < getColumn(); y++) {
                setValue(x, y, getValue(x, y).add(m.getValue(x, y)));
            }
        }

        return this;
    }

    public Matrix subtract(Matrix m) {
        if (getRow() != m.getRow()) {
            throw new IllegalStateException(String.format("This matrix has different row from target matrix\nSource : %dx%d\nTarget : %dx%d", getRow(), getColumn(), m.getRow(), m.getColumn()));
        }

        if (getColumn() != m.getColumn()) {
            throw new IllegalStateException(String.format("This matrix has different column from target matrix\nSource : %dx%d\nTarget : %dx%d", getRow(), getColumn(), m.getRow(), m.getColumn()));
        }

        for(int x = 0; x < getRow(); x++) {
            for(int y = 0; y < getColumn(); y++) {
                setValue(x, y, getValue(x, y).subtract(m.getValue(x, y)));
            }
        }

        return this;
    }

    public Matrix multiply(Matrix m) {
        if (getColumn() != m.getRow()) {
            throw new IllegalStateException(String.format("Column of this matrix, and row of target matrix are different!\nSource : %dx%d\nTarget : %dx%d", getRow(), getColumn(), m.getRow(), m.getColumn()));
        }

        BigDecimal[][] decimals = new BigDecimal[getRow()][m.getColumn()];

        for (int x = 0; x < getRow(); x++) {
            for(int y = 0; y < m.getColumn(); y++) {
                BigDecimal value = BigDecimal.ZERO;

                for(int i = 0; i < getColumn(); i++) {
                    value = value.add(getValue(x, i).multiply(m.getValue(i, y)));
                }

                decimals[x][y] = value;
            }
        }

        this.matrix = decimals;

        return this;
    }

    public Matrix square(BigDecimal value) {
        if (value.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0 && value.abs().compareTo(BigDecimal.valueOf(999999999)) < 0) {
            for(int x = 0; x < getRow(); x++) {
                for(int y = 0; y < getColumn(); y++) {
                    setValue(x, y, getValue(x, y).pow(value.intValue(), MathContext.UNLIMITED));
                }
            }
        } else {
            for(int x = 0; x < getRow(); x++) {
                for(int y = 0; y < getColumn(); y++) {
                    double v = Math.pow(getValue(x, y).doubleValue(), value.doubleValue());

                    if(Double.isFinite(v)) {
                        setValue(x, y, BigDecimal.valueOf(v));
                    } else {
                        setValue(x, y, BigDecimal.ZERO);
                    }
                }
            }
        }

        return this;
    }

    public Matrix gaussianElimination() {
        if (getRow() + 1 != getColumn()) {
            throw new IllegalStateException(String.format("Number of column must be one larger than number of row, this matrix is %dx%d", getRow(), getColumn()));
        }

        for(int x = 0; x < getRow() - 1; x++) {
            if(getValue(x, x).compareTo(BigDecimal.ZERO) == 0) {
                for(int i = x + 1; i < getRow(); i++) {
                    if(getValue(i, x).compareTo(BigDecimal.ZERO) != 0) {
                        switchRow(x, i);

                        break;
                    }
                }
            }

            if(getValue(x, x).compareTo(BigDecimal.ZERO) == 0) {
                throw new ArithmeticException("Failed to perform gaussian elimination. Matrix is below \n\n" + this);
            }

            for(int i = x + 1; i < getRow(); i++) {
                BigDecimal factor = getValue(i, x).divide(getValue(x, x), Equation.context);

                if(factor.abs().compareTo(Number.cutOff) < 0)
                    factor = BigDecimal.ZERO;

                for(int j = 0; j < getColumn(); j++) {
                    setValue(i, j, getValue(i, j).subtract(getValue(x, j).multiply(factor)));

                    if(getValue(i, j).abs().compareTo(Number.cutOff) < 0)
                        setValue(i, j, BigDecimal.ZERO);
                }
            }
        }

        for(int x = getRow() - 1; x >= 0; x--) {
            BigDecimal factor = getValue(x, x);

            for(int y = 0; y < getColumn(); y++) {
                if(getValue(x, y).compareTo(BigDecimal.ZERO) == 0)
                    continue;

                setValue(x, y, getValue(x, y).divide(factor, Equation.context));

                if(getValue(x, y).abs().compareTo(Number.cutOff) < 0)
                    setValue(x, y, BigDecimal.ZERO);
            }

            if(x == 0)
                continue;

            for(int i = x - 1; i >= 0; i--) {
                factor = getValue(i, x);

                if(factor.compareTo(BigDecimal.ZERO) == 0)
                    continue;

                for(int j = 0; j < getColumn(); j++) {
                    if(getValue(i, j).compareTo(BigDecimal.ZERO) == 0)
                        continue;

                    setValue(i, j, getValue(i, j).subtract(getValue(x, j).multiply(factor)));

                    if(getValue(i, j).abs().compareTo(Number.cutOff) < 0)
                        setValue(i, j, BigDecimal.ZERO);
                }
            }
        }

        return this;
    }

    /**
     * Perform LU Decomposition
     *
     * @return Return two matrices, one being L (lu[0]), the another being U (lu[1])
     */
    public Matrix[] LUDecomposition() {
        if(getRow() != getColumn()) {
            throw new IllegalStateException(String.format("To perform LU decomposition, row and column must be same! This matrix is %dx%d", getRow(), getColumn()));
        }

        int n = getRow();

        Matrix[] lu = new Matrix[2];

        lu[0] = new Matrix(n, n);
        lu[1] = new Matrix(n, n);

        for(int i = 0; i < n; i++) {
            lu[0].setValue(i, 0, getValue(i, 0));
            lu[1].setValue(i, i, BigDecimal.ONE);
        }

        for(int j = 1; j < n; j++) {
            lu[1].setValue(0, j, getValue(0, j).divide(lu[0].getValue(0, 0), Equation.context));

            if(lu[1].getValue(0, j).abs().compareTo(Number.cutOff) < 0)
                lu[1].setValue(0, j, BigDecimal.ZERO);
        }

        for(int j = 1; j < n - 1; j++) {
            for(int i = j; i < n; i++) {
                BigDecimal value = getValue(i, j);

                for(int k = 0; k < j; k++) {
                    value = value.subtract(lu[0].getValue(i, k).multiply(lu[1].getValue(k, j)));
                }

                lu[0].setValue(i, j, value);
            }

            for(int k = j; k < n; k++) {
                BigDecimal value = getValue(j, k);

                for(int i = 0; i < j; i++) {
                    value = value.subtract(lu[0].getValue(j, i).multiply(lu[1].getValue(i, k)));
                }

                value = value.divide(lu[0].getValue(j, j), Equation.context);

                if(value.abs().compareTo(Number.cutOff) < 0)
                    value = BigDecimal.ZERO;

                lu[1].setValue(j, k, value);
            }

            BigDecimal value = getValue(n - 1, n - 1);

            for(int k = 0; k < n; k++) {
                value = value.subtract(lu[0].getValue(n - 1, k).multiply(lu[1].getValue(k, n - 1)));
            }

            lu[0].setValue(n - 1, n - 1, value);
        }

        return lu;
    }

    public Matrix attachColumn(Matrix m) {
        if(getRow() != m.getRow()) {
            throw new IllegalStateException(String.format("This matrix's raw is different from target matrix's row!\nSource : %dx%d\nTarget : %dx%d", getRow(), getColumn(), m.getRow(), m.getColumn()));
        }

        Matrix result = new Matrix(getRow(), getColumn() + m.getColumn());

        for(int x = 0; x < getRow(); x++) {
            for(int y = 0; y < getColumn() + m.getColumn(); y++) {
                if(y >= getColumn()) {
                    result.setValue(x, y, m.getValue(x, y - getColumn()));
                } else {
                    result.setValue(x, y, getValue(x, y));
                }
            }
        }

        this.matrix = result.matrix;

        return this;
    }

    private void switchRow(int source, int target) {
        if (source < 0 || source >= getRow()) {
            throw new IllegalStateException(String.format("Source row is out of range, this matrix is %dx%d", getRow(), getColumn()));
        }

        if (target < 0 || target >= getRow()) {
            throw new IllegalStateException(String.format("Target row is out of range, this matrix is %dx%d", getRow(), getColumn()));
        }

        Matrix m = copy();

        for(int i = 0; i < getColumn(); i++) {
            m.setValue(source, i, getValue(target, i));
            m.setValue(target, i, getValue(source, i));
        }

        this.matrix = m.matrix;
    }

    private Matrix extractColumn(int col) {
        if (col < 0 || col >= getColumn()) {
            throw new IllegalStateException(String.format("Column is out of range, this matrix is %dx%d", getRow(), getColumn()));
        }

        Matrix result = new Matrix(getRow(), 1);

        for(int x = 0; x < getRow(); x++) {
            result.setValue(x, 0, getValue(x, col));
        }

        return result;
    }

    public Matrix copy() {
        Matrix m = new Matrix(getRow(), getColumn());

        for(int i = 0; i < getRow(); i++) {
            for(int j = 0; j < getColumn(); j++) {
                m.setValue(i, j, getValue(i, j));
            }
        }

        return m;
    }

    @Override
    public String toString() {
        int maxLength = 0;

        for(int x = 0; x < getRow(); x++) {
            for(int y = 0; y < getColumn(); y++) {
                maxLength = Math.max(maxLength, Equation.df.format(getValue(x, y)).length());
            }
        }

        if(getRow() == 1) {
            StringBuilder builder = new StringBuilder("[ ");

            for(int y = 0; y < getColumn(); y++) {
                String value = Equation.df.format(getValue(0, y));

                builder.append(value).append(" ".repeat(Math.max(0, value.length() - maxLength)));

                if(y < getColumn() - 1) {
                    builder.append(", ");
                }
            }

            builder.append(" ]");

            return builder.toString();
        } else {
            StringBuilder builder = new StringBuilder();

            for(int x = 0; x < getRow(); x++) {
                if (x == 0) {
                    builder.append("┌ ");
                } else if(x < getRow() - 1) {
                    builder.append("│ ");
                } else {
                    builder.append("└ ");
                }

                for(int y = 0; y < getColumn(); y++) {
                    String value = Equation.df.format(getValue(x, y));

                    builder.append(value).append(" ".repeat(Math.max(0, maxLength - value.length())));

                    if(y < getColumn() - 1) {
                        builder.append(", ");
                    }
                }

                if (x == 0) {
                    builder.append(" ┐\n");
                } else if(x < getRow() - 1) {
                    builder.append(" │\n");
                } else {
                    builder.append(" ┘");
                }
            }

            return builder.toString();
        }
    }
}
