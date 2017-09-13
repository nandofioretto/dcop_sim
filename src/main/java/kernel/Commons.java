package kernel;

import java.util.List;

/**
 * Created by nandofioretto on 5/25/17.
 */
public class Commons {

    public static double getAverage(double[] array) {
        double avg = 0;
        for (double a : array) {
            avg += a;
        }
        return (avg/(double)array.length);
    }

    public static double getMin(double[] array) {
        double min = array[0];
        for (int i = 1; i < array.length; i++)
            if (array[i] < min)
                min = array[i];
        return min;
    }

    public static int getArgMin(double[] array) {
        double min = array[0];
        int argmin = 0;
        for (int i = 1; i < array.length; i++)
            if (array[i] < min) {
                min = array[i];
                argmin = i;
            }
        return argmin;
    }

    public static double getMax(double[] array) {
        double max = array[0];
        for (int i = 1; i < array.length; i++)
            if (array[i] > max)
                max = array[i];
        return max;
    }

    public static int getArgMax(double[] array) {
        double max = array[0];
        int argmax = 0;
        for (int i = 1; i < array.length; i++)
            if (array[i] > max) {
                max = array[i];
                argmax = i;
            }
        return argmax;
    }

    public static void addValue(double[] array, double value) {
        for (int i = 0; i < array.length; i++)
            array[i] += value;
    }

    public static void mulValue(double[] array, double value) {
        for (int i = 0; i < array.length; i++)
            array[i] *= value;
    }

    public static void rmValue(double[] array, double value) {
        for (int i = 0; i < array.length; i++)
            array[i] -= value;
    }

    public static void addArray(double[] out, double[] in) {
        assert (out.length == in.length);
        for (int i = 0; i <out.length; i++)
            out[i] += in[i];
    }

    public static <T> int getIdx(List<T> array, T target) {
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).equals(target))
                return i;
        }
        return -1;
    }

    public static <T> int getIdx(T array[], T target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(target))
                return i;
        }
        return -1;
    }

    public static String toString(double[][] matrix) {
        String ret = "";
        for (int i = 0; i < matrix.length; i++) {
            ret += "[ ";
            for (int j = 0; j < matrix[i].length; j++) {
                ret += matrix[i][j] + " ";
            }
            ret += "]\n";
        }
        return ret;
    }
}
