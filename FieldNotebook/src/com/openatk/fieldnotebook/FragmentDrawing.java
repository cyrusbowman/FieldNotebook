package com.openatk.fieldnotebook;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

public class FragmentDrawing extends Fragment implements OnClickListener {

	private DrawingListener listener = null;
	private ImageButton butPoint = null;
	private ImageButton butPolyline = null;
	private ImageButton butPolygon = null;
	private ImageButton butColor = null;
	private ImageButton butCamera = null;

	// Interface for receiving data
	public interface DrawingListener {
		public void DrawingClickPoint();
		public void DrawingClickPolyline();
		public void DrawingClickPolygon();
		public void DrawingClickColor();
		public void DrawingClickCamera();
	}

	public void setPointIcon(int resId){
		this.butPoint.setImageResource(resId);
	}
	public void setPolylineIcon(int resId){
		this.butPolyline.setImageResource(resId);
	}
	public void setPolygonIcon(int resId){
		this.butPolygon.setImageResource(resId);
	}
	public void setColorIcon(int resId){
		this.butColor.setImageResource(resId);
	}
	public void setCameraIcon(int resId){
		this.butCamera.setImageResource(resId);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_drawing, container, false);

		butPoint = (ImageButton) view.findViewById(R.id.fragment_drawing_butPoint);
		butPolyline = (ImageButton) view.findViewById(R.id.fragment_drawing_butLine);
		butPolygon = (ImageButton) view.findViewById(R.id.fragment_drawing_butPolygon);
		butColor = (ImageButton) view.findViewById(R.id.fragment_drawing_butColor);
		butCamera = (ImageButton) view.findViewById(R.id.note_take_picture);

		butPoint.setOnClickListener(this);
		butPolyline.setOnClickListener(this);
		butPolygon.setOnClickListener(this);
		butColor.setOnClickListener(this);
		butCamera.setOnClickListener(this);

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.d("FragmentDrawing", "Attached");
	}
	
	public void setListener(DrawingListener listener){
		this.listener = listener;
	}

	public int getHeight() {
		// Method so close transition can work
		return getView().getHeight();
	}

	@Override
	public void onClick(View v) {
		if(listener != null){
			if (v.getId() == R.id.fragment_drawing_butPoint) {
				listener.DrawingClickPoint();
			} else if (v.getId() == R.id.fragment_drawing_butLine) {
				listener.DrawingClickPolyline();
			} else if (v.getId() == R.id.fragment_drawing_butPolygon) {
				listener.DrawingClickPolygon();
			} else if (v.getId() == R.id.fragment_drawing_butColor) {
				listener.DrawingClickColor();
			} else if (v.getId() == R.id.note_take_picture) {
				listener.DrawingClickCamera();
			}
		}
	}
}
