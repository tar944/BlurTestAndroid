package at.favre.app.blurtest.models;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import at.favre.app.blurtest.util.BenchmarkUtil;
import at.favre.app.blurtest.blur.EBlurAlgorithm;

/**
 * Created by PatrickF on 18.04.2014.
 */
public class ResultTableModel {
	public static final String TAG = ResultTableModel.class.getSimpleName();
    public static final double BEST_WORST_THRESHOLD_PERCENTAGE = 5;
    public static final String MISSING = "?";

    public static final String NUMBER_FORMAT = "0.00";

    public enum DataType {AVG(true), MIN_MAX(true), OVER_16_MS(true);
        private boolean minIsBest;

        DataType(boolean minIsBest) {
            this.minIsBest = minIsBest;
        }

        public boolean isMinIsBest() {
            return minIsBest;
        }
    }
    public enum RelativeType {BEST, WORST, AVG}

    Map<String,Map<String,BenchmarkResultDatabase.BenchmarkEntry>> tableModel;
    List<String> rows;
    List<String> columns;

    public ResultTableModel(BenchmarkResultDatabase db) {
        setUpModel(db);
    }

    private void setUpModel(BenchmarkResultDatabase db) {
        columns = new ArrayList<String>();
        for (EBlurAlgorithm algorithm : EBlurAlgorithm.getAllAlgorithms()) {
            columns.add(algorithm.toString());
        }
        Collections.sort(columns);

        TreeSet<String> rowHeaders = new TreeSet<String>();
        for (BenchmarkResultDatabase.BenchmarkEntry benchmarkEntry : db.getEntryList()) {
            rowHeaders.add(benchmarkEntry.getCategory());
        }
        rows = new ArrayList<String>(rowHeaders);

        tableModel = new HashMap<String, Map<String, BenchmarkResultDatabase.BenchmarkEntry>>();
        for (String column : columns) {
            tableModel.put(column, new HashMap<String, BenchmarkResultDatabase.BenchmarkEntry>());
            for (String row : rows) {
                tableModel.get(column).put(row, db.getByCategoryAndAlgorithm(row, EBlurAlgorithm.valueOf(column)));
            }
        }
    }

    public BenchmarkResultDatabase.BenchmarkEntry getCell(int row,int column) {
        return tableModel.get(columns.get(column)).get(rows.get(row));
    }

    private BenchmarkWrapper getRecentWrapper(int row, int column) {
        BenchmarkResultDatabase.BenchmarkEntry entry = getCell(row,column);
        if(entry != null && !entry.getWrapper().isEmpty()) {
            Collections.sort(entry.getWrapper());
            return entry.getWrapper().get(0);
        } else {
            return null;
        }
    }

    public String getValue(int row, int column, DataType type) {
		try {
			BenchmarkWrapper wrapper = getRecentWrapper(row, column);
			if (wrapper != null) {
				return getValueForType(wrapper,type).getRepresentation();
			}
		} catch (Exception e) {
			Log.w(TAG, "Error while getting data",e);
		}
        return MISSING;
    }

    public RelativeType getRelativeType(int row,int column, DataType type, boolean minIsBest) {
        if(row < 0 || column < 0) {
            return RelativeType.AVG;
        }
        List<Double> columns = new ArrayList<Double>();
        BenchmarkResultDatabase.BenchmarkEntry entry;
        BenchmarkWrapper wrapper=null;
        for (int i = 0; i < this.columns.size(); i++) {
            entry = getCell(row,i);

            if(entry != null && !entry.getWrapper().isEmpty()) {
                Collections.sort(entry.getWrapper());
                wrapper = entry.getWrapper().get(0);
            }

            if(wrapper != null) {
                columns.add(getValueForType(wrapper,type).getValue());
            } else {
                columns.add(Double.NEGATIVE_INFINITY);
            }
        }
        List<Double> sortedColumns = new ArrayList<Double>(columns);
        Collections.sort(sortedColumns);

        Double columnVal = columns.get(column);

        if(columnVal.equals(Double.NEGATIVE_INFINITY)) {
            return RelativeType.AVG;
        }

        double minThreshold = sortedColumns.get(0)+(sortedColumns.get(0)*BEST_WORST_THRESHOLD_PERCENTAGE/100);
        double maxThreshold = sortedColumns.get(columns.size()-1)-(sortedColumns.get(columns.size()-1)*BEST_WORST_THRESHOLD_PERCENTAGE/100);
         if(columnVal >= maxThreshold && columnVal <= sortedColumns.get(columns.size()-1)) {
            if(minIsBest) {
                return RelativeType.WORST;
            } else {
                return RelativeType.BEST;
            }
        } else if(columnVal <= minThreshold && columnVal >= sortedColumns.get(0)) {
            if(minIsBest) {
                return RelativeType.BEST;
            } else {
                return RelativeType.WORST;
            }
        } else {
            return RelativeType.AVG;
        }
    }

    public List<String> getRows() {
        return rows;
    }

    public List<String> getColumns() {
        return columns;
    }

    public static StatValue getValueForType(BenchmarkWrapper wrapper, DataType type) {
        switch (type) {
            case AVG:
                return new StatValue(wrapper.getStatInfo().getAsAvg().getAvg(),
                        BenchmarkUtil.formatNum(wrapper.getStatInfo().getAsAvg().getAvg(), NUMBER_FORMAT)+"ms");
            case MIN_MAX:
                return new StatValue(wrapper.getStatInfo().getAsAvg().getMax()+wrapper.getStatInfo().getAsAvg().getMin(),
                        BenchmarkUtil.formatNum(wrapper.getStatInfo().getAsAvg().getMin(), "0.#")+"/"+BenchmarkUtil.formatNum(wrapper.getStatInfo().getAsAvg().getMax(), "0.#")+"ms");
            case OVER_16_MS:
                return new StatValue(wrapper.getStatInfo().getAsAvg().getPercentageOverGivenValue(16d),
                        BenchmarkUtil.formatNum(wrapper.getStatInfo().getAsAvg().getPercentageOverGivenValue(16d), NUMBER_FORMAT)+"%");
        }
        return new StatValue();
    }

    public static class StatValue {
        public static final String MISSING = "?";

        private final Double value;
        private final String representation;
        private final boolean noValue;

        public StatValue(Double value, String representation) {
            this.value = value;
            this.representation = representation;
            noValue = false;
        }

        public StatValue() {
            value =Double.NEGATIVE_INFINITY;
            representation =MISSING;
            noValue = true;
        }

        public Double getValue() {
            return value;
        }

        public String getRepresentation() {
            return representation;
        }

        public boolean isNoValue() {
            return noValue;
        }
    }
}
