package at.favre.app.blurtest.fragments;

import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.squareup.picasso.Picasso;

import at.favre.app.blurtest.R;
import at.favre.app.blurtest.blur.IBlur;
import at.favre.app.blurtest.models.BenchmarkWrapper;
import at.favre.app.blurtest.util.GraphUtil;
import at.favre.app.blurtest.util.JsonUtil;

/**
 * Created by PatrickF on 16.04.2014.
 */
public class BlurBenchmarkDetailsDialog extends DialogFragment {
	private static final String WRAPPER_KEY = "wrapperKey";


	private BenchmarkWrapper wrapper;

	public static BlurBenchmarkDetailsDialog createInstance(BenchmarkWrapper wrapper) {
		BlurBenchmarkDetailsDialog dialog = new BlurBenchmarkDetailsDialog();
		dialog.setWrapper(wrapper);
		return dialog;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(savedInstanceState != null) {
			wrapper = JsonUtil.fromJsonString(savedInstanceState.getString(WRAPPER_KEY),BenchmarkWrapper.class);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.dialog_benchmark_details,container,false);
		Picasso.with(getActivity()).load(wrapper.getBitmapAsFile()).into((android.widget.ImageView) v.findViewById(R.id.image));

		FrameLayout layout = (FrameLayout) v.findViewById(R.id.graph);
		layout.addView(createGraph(wrapper));
		getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
		return v;
	}


	private GraphView createGraph(BenchmarkWrapper wrapper) {
		Resources res = getResources();
		int lineThicknessPx = (int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, res.getDisplayMetrics()));

		GraphView.GraphViewData[] data  = new GraphView.GraphViewData[wrapper.getStatInfo().getBenchmarkData().size()];
		for (int j = 0; j < wrapper.getStatInfo().getBenchmarkData().size(); j++) {
			data[j] = new GraphView.GraphViewData(j,wrapper.getStatInfo().getBenchmarkData().get(j));
		}

		LineGraphView graphView = new LineGraphView(getActivity() , "");
		GraphViewSeries.GraphViewSeriesStyle seriesStyle = new GraphViewSeries.GraphViewSeriesStyle(res.getColor(R.color.graphBgGreen),lineThicknessPx);

		if(wrapper.getStatInfo().getAsAvg().getMin() <= IBlur.MS_THRESHOLD_FOR_SMOOTH) {
			graphView.addSeries(GraphUtil.getStraightLine(IBlur.MS_THRESHOLD_FOR_SMOOTH, wrapper.getStatInfo().getBenchmarkData().size() - 1, "16ms", new GraphViewSeries.GraphViewSeriesStyle(res.getColor(R.color.graphBgRed), lineThicknessPx)));
		}
		graphView.addSeries(GraphUtil.getStraightLine((int) wrapper.getStatInfo().getAsAvg().getAvg(), wrapper.getStatInfo().getBenchmarkData().size()-1, "Avg", new GraphViewSeries.GraphViewSeriesStyle(res.getColor(R.color.graphBlue), lineThicknessPx)));
		graphView.addSeries(new GraphViewSeries("Blur", seriesStyle, data));
		graphView.setScrollable(true);
		graphView.setScalable(true);
		graphView.setManualYAxis(true);
		graphView.getGraphViewStyle().setGridColor(res.getColor(R.color.transparent));
		graphView.setCustomLabelFormatter(new CustomLabelFormatter() {
			@Override
			public String formatLabel(double value, boolean isValueX) {
				if (!isValueX) {
					return Math.round(value) + "ms";
				} else {
					return null;
				}
			}
		});
		graphView.setManualYAxisBounds(wrapper.getStatInfo().getAsAvg().getMax(), Math.max(0, wrapper.getStatInfo().getAsAvg().getMin() - 3l));
		graphView.setDrawBackground(false);
		graphView.setShowLegend(true);

		graphView.getGraphViewStyle().setHorizontalLabelsColor(res.getColor(R.color.transparent));
		graphView.getGraphViewStyle().setNumHorizontalLabels(0);
		graphView.getGraphViewStyle().setVerticalLabelsColor(res.getColor(R.color.optionsTextColor));
		graphView.getGraphViewStyle().setNumVerticalLabels(4);
		graphView.getGraphViewStyle().setVerticalLabelsAlign(Paint.Align.CENTER);
		graphView.getGraphViewStyle().setVerticalLabelsWidth( (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 34, res.getDisplayMetrics()));
		graphView.getGraphViewStyle().setTextSize((int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, res.getDisplayMetrics())));

		return graphView;
	}





	public void setWrapper(BenchmarkWrapper wrapper) {
		this.wrapper = wrapper;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(WRAPPER_KEY, JsonUtil.toJsonString(wrapper));
	}
}
